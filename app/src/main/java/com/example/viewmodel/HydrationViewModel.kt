package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HydrationRepository
import com.example.data.ReminderPrefsStore
import com.example.model.DrinkType
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.model.WaterLog
import com.example.model.todayDateKey
import com.example.notifications.ReminderNotificationHelper
import com.example.notifications.WaterReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface ReminderCoordinator {
    fun syncSchedule(settings: UserSettings, todayTotalMl: Long): Long
    fun readNextReminderEpochMs(): Long
    fun sendTestNotification(settings: UserSettings, todayTotalMl: Long): Boolean
}

class AndroidReminderCoordinator(context: Context) : ReminderCoordinator {
    private val appContext = context.applicationContext
    private val prefsStore = ReminderPrefsStore(appContext)

    override fun syncSchedule(settings: UserSettings, todayTotalMl: Long): Long {
        return WaterReminderScheduler.scheduleNextReminder(
            context = appContext,
            settings = settings,
            todayTotalMl = todayTotalMl
        )
    }

    override fun readNextReminderEpochMs(): Long =
        prefsStore.readSnapshot().nextReminderEpochMs

    override fun sendTestNotification(settings: UserSettings, todayTotalMl: Long): Boolean {
        return ReminderNotificationHelper.showHydrationReminder(
            context = appContext,
            defaultCupMl = settings.defaultCupMl,
            todayTotalMl = todayTotalMl,
            dailyGoalMl = settings.dailyGoalMl,
            isTest = true
        )
    }
}

class HydrationViewModel(
    private val repository: HydrationRepository,
    private val reminderCoordinator: ReminderCoordinator
) : ViewModel() {

    val settingsState: StateFlow<UiState<UserSettings>> = repository.observeSettings()
        .map<UserSettings, UiState<UserSettings>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing user settings", error)
            emit(UiState.Error(error.message ?: "Failed to load hydration settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val logsState: StateFlow<UiState<List<WaterLog>>> = repository.observeLogs()
        .map<List<WaterLog>, UiState<List<WaterLog>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing water logs", error)
            emit(UiState.Error(error.message ?: "Failed to load hydration logs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _nextReminderEpochMs = MutableStateFlow(reminderCoordinator.readNextReminderEpochMs())
    val nextReminderEpochMs: StateFlow<Long> = _nextReminderEpochMs.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSettingsInitialized()
        }
        viewModelScope.launch {
            combine(settingsState, logsState) { sState, lState ->
                if (sState is UiState.Success && lState is UiState.Success) {
                    val todayKey = todayDateKey()
                    val todayTotal = lState.data
                        .filter { it.dateKey == todayKey }
                        .sumOf { it.amountMl }
                    sState.data to todayTotal
                } else {
                    null
                }
            }.collect { pair ->
                if (pair != null) {
                    val (settings, todayTotal) = pair
                    val nextTrigger = reminderCoordinator.syncSchedule(settings, todayTotal)
                    _nextReminderEpochMs.value = nextTrigger
                }
            }
        }
    }

    fun logWater(amountMl: Long, drinkType: DrinkType) {
        viewModelScope.launch {
            val result = repository.addWaterLog(
                amountMl = amountMl,
                drinkType = drinkType
            )
            result.onSuccess {
                _feedbackMessage.value = "Logged +$amountMl ml ${drinkType.displayName}"
                val settings = (settingsState.value as? UiState.Success)?.data
                val logs = (logsState.value as? UiState.Success)?.data.orEmpty()
                if (settings != null) {
                    val todayTotal = logs.filter { it.dateKey == todayDateKey() }.sumOf { it.amountMl } + amountMl
                    _nextReminderEpochMs.value = reminderCoordinator.syncSchedule(settings, todayTotal)
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not save water entry."
            }
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            val result = repository.deleteWaterLog(logId)
            result.onSuccess {
                _feedbackMessage.value = "Entry removed"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not remove entry."
            }
        }
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings, confirmationMessage: String? = null) {
        val current = (settingsState.value as? UiState.Success)?.data ?: UserSettings()
        val updated = transform(current).copy(id = 1)
        viewModelScope.launch {
            val result = repository.saveSettings(updated)
            result.onSuccess {
                val todayTotal = ((logsState.value as? UiState.Success)?.data.orEmpty())
                    .filter { it.dateKey == todayDateKey() }
                    .sumOf { it.amountMl }
                _nextReminderEpochMs.value = reminderCoordinator.syncSchedule(updated, todayTotal)
                if (confirmationMessage != null) {
                    _feedbackMessage.value = confirmationMessage
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to update settings."
            }
        }
    }

    fun sendTestNotification() {
        val settings = (settingsState.value as? UiState.Success)?.data ?: UserSettings()
        val todayTotal = ((logsState.value as? UiState.Success)?.data.orEmpty())
            .filter { it.dateKey == todayDateKey() }
            .sumOf { it.amountMl }
        val posted = reminderCoordinator.sendTestNotification(settings, todayTotal)
        _feedbackMessage.value = if (posted) {
            "Test reminder notification sent!"
        } else {
            "Please grant notification permission first."
        }
    }

    fun refreshNextReminder() {
        _nextReminderEpochMs.value = reminderCoordinator.readNextReminderEpochMs()
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    private companion object {
        const val TAG = "HydrationVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
