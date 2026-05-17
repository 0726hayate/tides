package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CycleStatsTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            LocalDate.parse(start),
            LocalDate.parse(periodEnd),
            nextStart?.let { LocalDate.parse(it) }
        )

    @Test
    fun `empty cycles list returns empty stats`() {
        val s = CycleStats.compute(emptyList())
        assertNull(s.medianCycleLength)
        assertNull(s.medianPeriodLength)
    }

    @Test
    fun `single completed cycle reports its length`() {
        val s = CycleStats.compute(listOf(
            cycle("2026-01-01", "2026-01-04", "2026-01-29")
        ))
        assertEquals(28, s.medianCycleLength)
        assertEquals(4, s.medianPeriodLength)
    }

    @Test
    fun `median of an even number takes the lower middle`() {
        // lengths: 27, 28, 29, 30 → median = 28 (low-median convention)
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-04", "2026-01-28"),  // 27
            cycle("2026-01-28", "2026-01-31", "2026-02-25"),  // 28
            cycle("2026-02-25", "2026-02-28", "2026-03-26"),  // 29
            cycle("2026-03-26", "2026-03-29", "2026-04-25"),  // 30 (length-wise)
        )
        val s = CycleStats.compute(cycles)
        assertEquals(28, s.medianCycleLength)
    }

    @Test
    fun `regularity score is HIGH for variance under 2 days`() {
        // 28, 28, 29 — variance = 1
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 28
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 28
            cycle("2026-02-26", "2026-03-02", "2026-03-27"),  // 29
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Regularity.VERY_REGULAR, s.regularity)
    }

    @Test
    fun `regularity score is MODERATELY_VARIABLE for variance 4 to 7`() {
        // 26, 28, 30, 32 — max-min = 6
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-27"),  // 26
            cycle("2026-01-27", "2026-01-31", "2026-02-24"),  // 28
            cycle("2026-02-24", "2026-02-28", "2026-03-26"),  // 30
            cycle("2026-03-26", "2026-03-30", "2026-04-27"),  // 32
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Regularity.MODERATELY_VARIABLE, s.regularity)
    }

    @Test
    fun `regularity score is HIGHLY_VARIABLE for variance over 7`() {
        // 22, 28, 35 — max-min = 13
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-23"),  // 22
            cycle("2026-01-23", "2026-01-27", "2026-02-20"),  // 28
            cycle("2026-02-20", "2026-02-24", "2026-03-27"),  // 35
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Regularity.HIGHLY_VARIABLE, s.regularity)
    }

    @Test
    fun `period length trend detects shortening`() {
        // Recent period lengths: 5, 5, 4, 3, 3 — clearly shortening
        val cycles = listOf(
            // longer first half
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 5
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 5
            cycle("2026-02-26", "2026-03-01", "2026-03-26"),  // 4
            cycle("2026-03-26", "2026-03-28", "2026-04-23"),  // 3
            cycle("2026-04-23", "2026-04-25", "2026-05-21"),  // 3
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Trend.DECREASING, s.periodLengthTrend)
    }

    @Test
    fun `active cycle is excluded from length stats`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-04", "2026-01-29"),
            cycle("2026-01-29", "2026-02-02", null),  // active, no length yet
        )
        val s = CycleStats.compute(cycles)
        assertEquals(28, s.medianCycleLength)
        assertEquals(1, s.completedCycleCount)
        assertTrue(s.hasActiveCycle)
    }
}
