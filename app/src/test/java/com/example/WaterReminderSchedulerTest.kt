package com.example

import com.example.data.IntervalWindow
import com.example.notifications.WaterReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WaterReminderSchedulerTest {

    private fun epochAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `disabled reminders return 0L`() {
        val now = epochAt(2026, Calendar.NOVEMBER, 26, 12, 0)
        val next = WaterReminderScheduler.computeNextTriggerEpochMs(
            nowEpochMs = now,
            intervalMinutes = 60L,
            startMinutesFromMidnight = 8 * 60L,
            endMinutesFromMidnight = 22 * 60L,
            remindersEnabled = false,
            pauseWhenGoalMet = false,
            goalMet = false
        )
        assertEquals(0L, next)
    }

    @Test
    fun `midday reminder stays aligned to window start`() {
        val now = epochAt(2026, Calendar.NOVEMBER, 26, 14, 15)
        val next = WaterReminderScheduler.computeNextTriggerEpochMs(
            nowEpochMs = now,
            intervalMinutes = 45L,
            startMinutesFromMidnight = 8 * 60L,
            endMinutesFromMidnight = 22 * 60L,
            remindersEnabled = true,
            pauseWhenGoalMet = false,
            goalMet = false
        )
        assertEquals(epochAt(2026, Calendar.NOVEMBER, 26, 14, 45), next)
    }

    @Test
    fun `before wake up clamps to today start window`() {
        val now = epochAt(2026, Calendar.NOVEMBER, 26, 6, 30)
        val next = WaterReminderScheduler.computeNextTriggerEpochMs(
            nowEpochMs = now,
            intervalMinutes = 60L,
            startMinutesFromMidnight = 8 * 60L,
            endMinutesFromMidnight = 22 * 60L,
            remindersEnabled = true,
            pauseWhenGoalMet = false,
            goalMet = false
        )
        assertEquals(epochAt(2026, Calendar.NOVEMBER, 26, 8, 0), next)
    }

    @Test
    fun `after bedtime rolls over to tomorrow start window`() {
        val now = epochAt(2026, Calendar.NOVEMBER, 26, 22, 15)
        val next = WaterReminderScheduler.computeNextTriggerEpochMs(
            nowEpochMs = now,
            intervalMinutes = 60L,
            startMinutesFromMidnight = 8 * 60L,
            endMinutesFromMidnight = 22 * 60L,
            remindersEnabled = true,
            pauseWhenGoalMet = false,
            goalMet = false
        )
        assertEquals(epochAt(2026, Calendar.NOVEMBER, 27, 8, 0), next)
    }

    @Test
    fun `pause when goal met schedules for tomorrow morning`() {
        val now = epochAt(2026, Calendar.NOVEMBER, 26, 15, 0)
        val next = WaterReminderScheduler.computeNextTriggerEpochMs(
            nowEpochMs = now,
            intervalMinutes = 60L,
            startMinutesFromMidnight = 8 * 60L,
            endMinutesFromMidnight = 22 * 60L,
            remindersEnabled = true,
            pauseWhenGoalMet = true,
            goalMet = true
        )
        assertEquals(epochAt(2026, Calendar.NOVEMBER, 27, 8, 0), next)
    }

    @Test
    fun `active window helper checks daytime bounds accurately`() {
        val inside = epochAt(2026, Calendar.NOVEMBER, 26, 12, 30)
        val outside = epochAt(2026, Calendar.NOVEMBER, 26, 23, 15)
        assertTrue(WaterReminderScheduler.isWithinActiveWindow(inside, 8 * 60L, 22 * 60L))
        assertFalse(WaterReminderScheduler.isWithinActiveWindow(outside, 8 * 60L, 22 * 60L))
    }

    @Test
    fun `morning and evening alerts surround the interval window`() {
        fun at(hour: Int, minute: Int = 0) = epochAt(2026, Calendar.NOVEMBER, 26, hour, minute)
        fun next(now: Long, mode: String = "BOTH", times: List<Long> = listOf(480L, 1200L)) =
            WaterReminderScheduler.computeTriggerForMode(now, mode, times, listOf(IntervalWindow("midday", 720, 960, 60)), true, false, false)

        val expected = listOf(at(8), at(12), at(13), at(14), at(15), at(16), at(20))
        var now = at(7)
        for (alert in expected) {
            assertEquals(alert, next(now))
            now = alert
        }
        assertEquals(at(13), next(at(12), times = listOf(720L)))
        assertEquals(at(13), next(at(12, 25), times = listOf(745L)))
        assertEquals(at(20), next(at(8), mode = "SCHEDULED"))
        assertEquals(at(12), next(at(7), mode = "INTERVAL"))
        assertEquals(0L, next(at(7), mode = "SCHEDULED", times = emptyList()))
        assertEquals(0L, next(at(7), mode = "NONE"))
    }

    @Test
    fun `two windows keep separate cadences around the quiet gap`() {
        fun at(h: Int, m: Int = 0) = epochAt(2026, Calendar.NOVEMBER, 26, h, m)
        val windows = listOf(IntervalWindow("afternoon", 780, 960, 60), IntervalWindow("evening", 1080, 1140, 30))
        fun next(now: Long, plan: List<IntervalWindow> = windows) =
            WaterReminderScheduler.computeTriggerForMode(now, "BOTH", listOf(1140, 1200), plan, true, false, false)
        var now = at(12)
        for (expected in listOf(at(13), at(14), at(15), at(16), at(18), at(18, 30), at(19), at(20))) {
            assertEquals(expected, next(now))
            now = expected
        }
        assertEquals(at(18), next(at(12), windows.drop(1)))
        assertEquals(at(18, 15), next(at(18), windows.map { if (it.id == "evening") it.copy(minutes = 15) else it }))
    }
}
