package com.example

import com.example.data.IntervalWindow
import com.example.data.firstOverlap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalOverlapTest {
    private val afternoon = IntervalWindow("afternoon", 780, 960, 30)
    private val night = IntervalWindow("night", 1320, 120, 60)
    private val plan = listOf(afternoon, night)

    @Test
    fun `disjoint windows and editing the same window are allowed`() {
        assertNull(plan.firstOverlap(1080, 1140, null))
        assertNull(plan.firstOverlap(780, 960, afternoon.id))
        assertNull(plan.firstOverlap(121, 779, null))
        assertEquals(night, plan.firstOverlap(780, 1380, afternoon.id))
    }

    @Test
    fun `partial contained enclosing and touching windows conflict`() {
        for ((start, end) in listOf(720L to 810L, 810L to 900L, 750L to 990L, 960L to 1020L, 780L to 960L)) {
            assertEquals(afternoon, plan.firstOverlap(start, end, null))
        }
    }

    @Test
    fun `overnight conflicts are caught on either day`() {
        assertEquals(night, plan.firstOverlap(60, 180, null))
        assertEquals(night, plan.firstOverlap(1260, 1380, null))
        assertEquals(night, plan.firstOverlap(1380, 30, null))
        assertEquals(night, plan.firstOverlap(120, 120, null))
    }
}
