package com.example.data

import android.content.Context
import com.example.model.UserSettings
import com.example.model.todayDateKey

data class IntervalWindow(val id: String, val start: Long, val end: Long, val minutes: Long)

// Both endpoints can trigger an alert, including the end minute of an overnight window.
private fun containsMinute(start: Long, end: Long, minute: Long): Boolean =
    if (start <= end) minute in start..end else minute >= start || minute <= end

fun List<IntervalWindow>.firstOverlap(start: Long, end: Long, editingId: String?): IntervalWindow? =
    firstOrNull { window ->
        window.id != editingId && (containsMinute(window.start, window.end, start) ||
            containsMinute(start, end, window.start))
    }

data class LocalReminderSnapshot(
    val remindersEnabled: Boolean,
    val pauseWhenGoalMet: Boolean,
    val defaultCupMl: Long,
    val dailyGoalMl: Long,
    val todayDateKey: String,
    val todayTotalMl: Long,
    val nextReminderEpochMs: Long,
    val windows: List<IntervalWindow>,
    val reminderMode: String = REMINDER_MODE_INTERVAL,
    val scheduledTimesMinutes: List<Long> = DEFAULT_SCHEDULED_TIMES
) {
    companion object {
        const val REMINDER_MODE_NONE = "NONE"
        const val REMINDER_MODE_INTERVAL = "INTERVAL"
        const val REMINDER_MODE_SCHEDULED = "SCHEDULED"
        const val REMINDER_MODE_BOTH = "BOTH"
        val DEFAULT_SCHEDULED_TIMES = listOf(540L, 720L, 900L, 1080L, 1200L)
    }
}

class ReminderPrefsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveFromSettings(settings: UserSettings, todayTotalMl: Long) {
        val currentDateKey = todayDateKey()
        prefs.edit()
            .putBoolean(KEY_REMINDERS_ENABLED, settings.remindersEnabled)
            .putLong(KEY_INTERVAL_MINUTES, settings.intervalMinutes)
            .putLong(KEY_START_MINUTES, settings.startMinutesFromMidnight)
            .putLong(KEY_END_MINUTES, settings.endMinutesFromMidnight)
            .putBoolean(KEY_PAUSE_WHEN_GOAL_MET, settings.pauseWhenGoalMet)
            .putLong(KEY_DEFAULT_CUP_ML, settings.defaultCupMl)
            .putLong(KEY_DAILY_GOAL_ML, settings.dailyGoalMl)
            .putString(KEY_TODAY_DATE_KEY, currentDateKey)
            .putLong(KEY_TODAY_TOTAL_ML, todayTotalMl)
            .apply()
    }

    fun updateTodayTotal(todayTotalMl: Long) {
        prefs.edit()
            .putString(KEY_TODAY_DATE_KEY, todayDateKey())
            .putLong(KEY_TODAY_TOTAL_ML, todayTotalMl)
            .apply()
    }

    fun saveNextReminderEpochMs(epochMs: Long) {
        prefs.edit().putLong(KEY_NEXT_REMINDER_EPOCH_MS, epochMs).apply()
    }

    fun savePlan(mode: String, times: List<Long>, windows: List<IntervalWindow>) {
        val encoded = org.json.JSONArray()
        for (window in windows) {
            encoded.put(org.json.JSONObject().put("id", window.id).put("start", window.start)
                .put("end", window.end).put("minutes", window.minutes))
        }
        prefs.edit()
            .putString(KEY_REMINDER_MODE, mode)
            .putString(KEY_WINDOWS, encoded.toString())
            .putString(KEY_SCHEDULED_TIMES, times.distinct().sorted().joinToString(","))
            .apply()
    }

    fun readSnapshot(): LocalReminderSnapshot {
        val storedDate = prefs.getString(KEY_TODAY_DATE_KEY, null)
        val currentDate = todayDateKey()
        val totalToday = if (storedDate == currentDate) {
            prefs.getLong(KEY_TODAY_TOTAL_ML, 0L)
        } else {
            0L
        }
        val mode = prefs.getString(KEY_REMINDER_MODE, null) ?: LocalReminderSnapshot.REMINDER_MODE_INTERVAL
        val start = prefs.getLong(KEY_START_MINUTES, 480L)
        val end = prefs.getLong(KEY_END_MINUTES, 1320L)
        val minutes = prefs.getLong(KEY_INTERVAL_MINUTES, 60L)
        val storedWindows = prefs.getString(KEY_WINDOWS, null)
        val windows = if (storedWindows != null) {
            val array = org.json.JSONArray(storedWindows)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                IntervalWindow(item.getString("id"), item.getLong("start"), item.getLong("end"), item.getLong("minutes"))
            }
        } else if (mode == LocalReminderSnapshot.REMINDER_MODE_INTERVAL || mode == LocalReminderSnapshot.REMINDER_MODE_BOTH) {
            listOf(IntervalWindow("legacy", start, end, minutes))
        } else emptyList()
        return LocalReminderSnapshot(
            remindersEnabled = prefs.getBoolean(KEY_REMINDERS_ENABLED, true),
            windows = windows,
            pauseWhenGoalMet = prefs.getBoolean(KEY_PAUSE_WHEN_GOAL_MET, false),
            defaultCupMl = prefs.getLong(KEY_DEFAULT_CUP_ML, 250L),
            dailyGoalMl = prefs.getLong(KEY_DAILY_GOAL_ML, 2500L),
            todayDateKey = currentDate,
            todayTotalMl = totalToday,
            nextReminderEpochMs = prefs.getLong(KEY_NEXT_REMINDER_EPOCH_MS, 0L),
            reminderMode = mode,
            scheduledTimesMinutes = prefs.getString(KEY_SCHEDULED_TIMES, null)
                ?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?.distinct()
                ?.sorted()
                ?: if (prefs.getString(KEY_REMINDER_MODE, null) in listOf(
                    LocalReminderSnapshot.REMINDER_MODE_SCHEDULED, LocalReminderSnapshot.REMINDER_MODE_BOTH
                )) LocalReminderSnapshot.DEFAULT_SCHEDULED_TIMES else emptyList()
        )
    }

    private companion object {
        const val PREFS_NAME = "hydropulse_reminder_prefs"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        const val KEY_START_MINUTES = "start_minutes"
        const val KEY_END_MINUTES = "end_minutes"
        const val KEY_PAUSE_WHEN_GOAL_MET = "pause_when_goal_met"
        const val KEY_DEFAULT_CUP_ML = "default_cup_ml"
        const val KEY_DAILY_GOAL_ML = "daily_goal_ml"
        const val KEY_TODAY_DATE_KEY = "today_date_key"
        const val KEY_TODAY_TOTAL_ML = "today_total_ml"
        const val KEY_NEXT_REMINDER_EPOCH_MS = "next_reminder_epoch_ms"
        const val KEY_REMINDER_MODE = "reminder_mode"
        const val KEY_WINDOWS = "interval_windows"
        const val KEY_SCHEDULED_TIMES = "scheduled_times_minutes"
    }
}
