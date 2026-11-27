package com.example.data

import android.content.Context
import com.example.model.AppThemeMode
import com.example.model.DrinkType
import com.example.model.UserSettings
import com.example.model.WaterLog
import com.example.model.todayDateKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HydrationRepository(
    private val dao: HydrationDao
) {
    constructor(context: Context) : this(
        HydroPulseDatabase.getDatabase(context).hydrationDao()
    )

    fun observeSettings(): Flow<UserSettings> =
        dao.observeSettings().map { it ?: UserSettings() }

    fun observeLogs(): Flow<List<WaterLog>> =
        dao.observeAllLogs()

    suspend fun ensureSettingsInitialized(): Result<Unit> = try {
        if (dao.getSettingsOnce() == null) {
            dao.upsertSettings(UserSettings())
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun saveSettings(settings: UserSettings): Result<Unit> = try {
        val sanitized = settings.copy(
            id = 1,
            dailyGoalMl = settings.dailyGoalMl.coerceIn(500L, 10000L),
            intervalMinutes = settings.intervalMinutes.coerceIn(15L, 480L),
            startMinutesFromMidnight = settings.startMinutesFromMidnight.coerceIn(0L, 1439L),
            endMinutesFromMidnight = settings.endMinutesFromMidnight.coerceIn(0L, 1439L),
            defaultCupMl = settings.defaultCupMl.coerceIn(50L, 2000L),
            textScale = settings.textScale.coerceIn(0.80f, 1.25f),
            themeMode = AppThemeMode.safeFromString(settings.themeMode).name
        )
        dao.upsertSettings(sanitized)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addWaterLog(
        amountMl: Long,
        drinkType: DrinkType,
        dateKey: String = todayDateKey(),
        loggedAtEpochMs: Long = System.currentTimeMillis()
    ): Result<Unit> = try {
        require(amountMl in 10L..5000L) { "Amount must be between 10 and 5000 ml." }
        require(loggedAtEpochMs > 0L) { "Timestamp must be positive." }
        dao.insertWaterLog(
            WaterLog(
                amountMl = amountMl,
                drinkType = drinkType.name,
                dateKey = dateKey,
                loggedAtEpochMs = loggedAtEpochMs
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteWaterLog(logId: Long): Result<Unit> = try {
        require(logId > 0L) { "Log ID must be valid." }
        dao.deleteWaterLogById(logId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTotalMlForDate(dateKey: String = todayDateKey()): Long =
        dao.getTotalMlForDate(dateKey)
}
