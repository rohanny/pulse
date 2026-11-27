package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.HydrationRepository
import com.example.data.ReminderPrefsStore
import com.example.model.DrinkType
import com.example.model.todayDateKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                WaterReminderScheduler.scheduleFromStoredSnapshot(context)
            }

            ACTION_REMINDER_TRIGGER -> {
                val store = ReminderPrefsStore(context)
                val snapshot = store.readSnapshot()
                if (!snapshot.remindersEnabled) {
                    WaterReminderScheduler.cancelReminders(context)
                    return
                }
                val goalMet = snapshot.todayTotalMl >= snapshot.dailyGoalMl
                val inWindow = snapshot.reminderMode == com.example.data.LocalReminderSnapshot.REMINDER_MODE_SCHEDULED ||
                    (snapshot.reminderMode == com.example.data.LocalReminderSnapshot.REMINDER_MODE_BOTH &&
                        java.util.Calendar.getInstance().apply { timeInMillis = snapshot.nextReminderEpochMs }.let {
                            (it.get(java.util.Calendar.HOUR_OF_DAY) * 60L + it.get(java.util.Calendar.MINUTE)) in snapshot.scheduledTimesMinutes
                        }) ||
                    snapshot.windows.any { window ->
                        WaterReminderScheduler.isWithinActiveWindow(System.currentTimeMillis(), window.start, window.end)
                    }
                if (inWindow && !(snapshot.pauseWhenGoalMet && goalMet)) {
                    ReminderNotificationHelper.showHydrationReminder(
                        context = context,
                        defaultCupMl = snapshot.defaultCupMl,
                        todayTotalMl = snapshot.todayTotalMl,
                        dailyGoalMl = snapshot.dailyGoalMl,
                        isTest = false
                    )
                }
                WaterReminderScheduler.scheduleFromStoredSnapshot(context)
            }

            ACTION_QUICK_LOG -> {
                val store = ReminderPrefsStore(context)
                val snapshot = store.readSnapshot()
                val amountMl = intent.getLongExtra(EXTRA_AMOUNT_ML, snapshot.defaultCupMl)
                    .coerceIn(10L, 5000L)

                ReminderNotificationHelper.cancelReminderNotification(context)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = HydrationRepository(context.applicationContext)
                        val result = repo.addWaterLog(
                            amountMl = amountMl,
                            drinkType = DrinkType.WATER
                        )
                        if (result.isSuccess) {
                            val updatedTotal = repo.getTotalMlForDate(todayDateKey())
                            store.updateTodayTotal(updatedTotal)
                        }
                        WaterReminderScheduler.scheduleFromStoredSnapshot(context.applicationContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                ReminderNotificationHelper.cancelReminderNotification(context)
                WaterReminderScheduler.scheduleSnooze(context, snoozeMinutes = 15L)
            }
        }
    }

    companion object {
        const val ACTION_REMINDER_TRIGGER = "com.example.action.WATER_REMINDER_TRIGGER"
        const val ACTION_QUICK_LOG = "com.example.action.WATER_QUICK_LOG"
        const val ACTION_SNOOZE = "com.example.action.WATER_SNOOZE"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
    }
}
