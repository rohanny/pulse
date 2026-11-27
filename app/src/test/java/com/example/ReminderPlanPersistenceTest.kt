package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.IntervalWindow
import com.example.data.LocalReminderSnapshot.Companion.REMINDER_MODE_BOTH
import com.example.data.LocalReminderSnapshot.Companion.REMINDER_MODE_SCHEDULED
import com.example.data.ReminderPrefsStore
import com.example.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderPlanPersistenceTest {
    @Test
    fun `old interval survives adding editing and deleting another window`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ReminderPrefsStore(context)
        val settings = UserSettings(startMinutesFromMidnight = 780, endMinutesFromMidnight = 960, intervalMinutes = 30)
        store.saveFromSettings(settings, 0)
        val afternoon = store.readSnapshot().windows.single()
        assertEquals(IntervalWindow("legacy", 780, 960, 30), afternoon)

        val evening = IntervalWindow("evening", 1080, 1140, 15)
        val times = listOf(480L, 1200L)
        for (plan in listOf(listOf(afternoon, evening), listOf(afternoon, evening.copy(minutes = 45)), listOf(afternoon))) {
            store.savePlan(REMINDER_MODE_BOTH, times, plan)
            store.saveFromSettings(settings, 250)
            val restored = ReminderPrefsStore(context).readSnapshot()
            assertEquals(plan, restored.windows)
            assertEquals(times, restored.scheduledTimesMinutes)
        }
        store.savePlan(REMINDER_MODE_SCHEDULED, times, emptyList())
        assertEquals(emptyList<IntervalWindow>(), ReminderPrefsStore(context).readSnapshot().windows)
    }
}
