package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.IntervalWindow
import com.example.data.ReminderPrefsStore
import com.example.model.UserSettings
import java.util.Calendar

object WaterReminderScheduler {
    private const val REQUEST_CODE_REMINDER = 2001

    /**
     * Computes the next alarm epoch timestamp based on the user's interval and active daytime window.
     * Returns 0L when reminders are disabled.
     */
    fun computeNextTriggerEpochMs(
        nowEpochMs: Long,
        intervalMinutes: Long,
        startMinutesFromMidnight: Long,
        endMinutesFromMidnight: Long,
        remindersEnabled: Boolean,
        pauseWhenGoalMet: Boolean,
        goalMet: Boolean
    ): Long {
        if (!remindersEnabled) return 0L
        val safeIntervalMs = intervalMinutes.coerceIn(15L, 480L) * 60_000L
        val startMin = startMinutesFromMidnight.coerceIn(0L, 1439L).toInt()
        val endMin = endMinutesFromMidnight.coerceIn(0L, 1439L).toInt()

        val startHour = startMin / 60
        val startMinute = startMin % 60
        val endHour = endMin / 60
        val endMinute = endMin % 60

        fun calendarAtOffsetDays(daysOffset: Int, hour: Int, minute: Int): Calendar {
            return Calendar.getInstance().apply {
                timeInMillis = nowEpochMs
                add(Calendar.DAY_OF_YEAR, daysOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val todayStartMs = calendarAtOffsetDays(0, startHour, startMinute).timeInMillis
        val todayEndMs = calendarAtOffsetDays(0, endHour, endMinute).timeInMillis
        val tomorrowStartMs = calendarAtOffsetDays(1, startHour, startMinute).timeInMillis

        // If goal is met and auto-pause is on, schedule for tomorrow's morning start window.
        if (pauseWhenGoalMet && goalMet) {
            return tomorrowStartMs
        }

        val overnight = endMin < startMin
        val windowStart = when {
            overnight && nowEpochMs <= todayEndMs -> calendarAtOffsetDays(-1, startHour, startMinute).timeInMillis
            nowEpochMs < todayStartMs -> return todayStartMs
            else -> todayStartMs
        }
        val windowEnd = if (overnight && windowStart == todayStartMs) {
            calendarAtOffsetDays(1, endHour, endMinute).timeInMillis
        } else todayEndMs
        val elapsedIntervals = (nowEpochMs - windowStart) / safeIntervalMs
        val next = windowStart + (elapsedIntervals + 1) * safeIntervalMs
        return when {
            next <= windowEnd -> next
            windowStart < todayStartMs -> todayStartMs
            else -> tomorrowStartMs
        }
    }

    fun isWithinActiveWindow(
        nowEpochMs: Long,
        startMinutesFromMidnight: Long,
        endMinutesFromMidnight: Long
    ): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = startMinutesFromMidnight.coerceIn(0L, 1439L).toInt()
        val end = endMinutesFromMidnight.coerceIn(0L, 1439L).toInt()
        return if (end >= start) {
            nowMinutes in start..end
        } else {
            nowMinutes >= start || nowMinutes <= end
        }
    }

    fun scheduleNextReminder(
        context: Context,
        settings: UserSettings,
        todayTotalMl: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Long {
        val store = ReminderPrefsStore(context)
        store.saveFromSettings(settings, todayTotalMl)

        return scheduleFromStoredSnapshot(context, nowEpochMs)
    }

    fun computeNextScheduledEpochMs(
        nowEpochMs: Long,
        scheduledTimesMinutes: List<Long>,
        remindersEnabled: Boolean,
        pauseWhenGoalMet: Boolean,
        goalMet: Boolean
    ): Long {
        if (!remindersEnabled || scheduledTimesMinutes.isEmpty()) return 0L
        val sorted = scheduledTimesMinutes.map { it.coerceIn(0L, 1439L).toInt() }.distinct().sorted()

        fun calendarAtOffsetDays(daysOffset: Int, totalMinutes: Int): Long {
            return Calendar.getInstance().apply {
                timeInMillis = nowEpochMs
                add(Calendar.DAY_OF_YEAR, daysOffset)
                set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
                set(Calendar.MINUTE, totalMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        if (pauseWhenGoalMet && goalMet) {
            return calendarAtOffsetDays(1, sorted.first())
        }

        for (min in sorted) {
            val candidate = calendarAtOffsetDays(0, min)
            if (candidate > nowEpochMs) {
                return candidate
            }
        }
        return calendarAtOffsetDays(1, sorted.first())
    }

    internal fun computeTriggerForMode(
        nowEpochMs: Long,
        reminderMode: String,
        scheduledTimesMinutes: List<Long>,
        windows: List<IntervalWindow>,
        remindersEnabled: Boolean,
        pauseWhenGoalMet: Boolean,
        goalMet: Boolean
    ): Long {
        val intervalMs = windows.minOfOrNull { window ->
            computeNextTriggerEpochMs(nowEpochMs, window.minutes, window.start, window.end,
                remindersEnabled, pauseWhenGoalMet, goalMet)
        } ?: 0L
        val scheduledMs = computeNextScheduledEpochMs(
            nowEpochMs = nowEpochMs,
            scheduledTimesMinutes = scheduledTimesMinutes,
            remindersEnabled = remindersEnabled,
            pauseWhenGoalMet = pauseWhenGoalMet,
            goalMet = goalMet
        )
        return when (reminderMode) {
            com.example.data.LocalReminderSnapshot.REMINDER_MODE_SCHEDULED ->
                scheduledMs
            com.example.data.LocalReminderSnapshot.REMINDER_MODE_NONE -> 0L
            com.example.data.LocalReminderSnapshot.REMINDER_MODE_BOTH ->
                listOf(intervalMs, scheduledMs).filter { it > 0L }.minOrNull() ?: 0L
            else -> intervalMs
        }
    }

    fun scheduleFromStoredSnapshot(context: Context, nowEpochMs: Long = System.currentTimeMillis()): Long {
        val snapshot = ReminderPrefsStore(context).readSnapshot()
        if (!snapshot.remindersEnabled) {
            cancelReminders(context)
            return 0L
        }
        val goalMet = snapshot.todayTotalMl >= snapshot.dailyGoalMl
        val triggerAtMs = computeTriggerForMode(
            nowEpochMs = nowEpochMs,
            reminderMode = snapshot.reminderMode,
            scheduledTimesMinutes = snapshot.scheduledTimesMinutes,
            windows = snapshot.windows,
            remindersEnabled = snapshot.remindersEnabled,
            pauseWhenGoalMet = snapshot.pauseWhenGoalMet,
            goalMet = goalMet
        )
        if (triggerAtMs > 0L) scheduleExactAlarmAt(context, triggerAtMs) else cancelReminders(context)
        ReminderPrefsStore(context).saveNextReminderEpochMs(triggerAtMs)
        return triggerAtMs
    }

    fun scheduleSnooze(context: Context, snoozeMinutes: Long = 15L, nowEpochMs: Long = System.currentTimeMillis()): Long {
        val triggerAtMs = nowEpochMs + snoozeMinutes * 60_000L
        scheduleExactAlarmAt(context, triggerAtMs)
        ReminderPrefsStore(context).saveNextReminderEpochMs(triggerAtMs)
        return triggerAtMs
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(reminderPendingIntent(context))
        ReminderPrefsStore(context).saveNextReminderEpochMs(0L)
    }

    private fun scheduleExactAlarmAt(context: Context, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMs,
            reminderPendingIntent(context)
        )
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_REMINDER_TRIGGER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
