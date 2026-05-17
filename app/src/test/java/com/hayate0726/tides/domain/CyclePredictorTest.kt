package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.PredictionRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CyclePredictorTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            LocalDate.parse(start),
            LocalDate.parse(periodEnd),
            nextStart?.let { LocalDate.parse(it) }
        )

    @Test
    fun `returns null when fewer than 2 completed cycles`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        assertNull(CyclePredictor.predictNextPeriod(listOf(active)))
    }

    @Test
    fun `regular cycles produce a narrow HIGH-confidence range`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 28
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 28
            cycle("2026-02-26", "2026-03-02", "2026-03-26"),  // 28
            cycle("2026-03-26", "2026-03-30", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)
        assertNotNull(range)
        assertEquals(PredictionRange.Confidence.HIGH, range!!.confidence)
        // Expected median start: active start + 28 = 2026-04-23
        assertTrue(range.start <= LocalDate.parse("2026-04-23"))
        assertTrue(range.end >= LocalDate.parse("2026-04-23"))
        // Narrow: <= 5 days wide
        val width = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end).toInt()
        assertTrue(width <= 4, "expected narrow range, got width=$width")
    }

    @Test
    fun `moderately variable cycles produce MEDIUM confidence and wider range`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-28"),  // 27
            cycle("2026-01-28", "2026-02-01", "2026-02-25"),  // 28
            cycle("2026-02-25", "2026-03-01", "2026-03-29"),  // 32 (variance up to 5)
            cycle("2026-03-29", "2026-04-02", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)!!
        assertEquals(PredictionRange.Confidence.MEDIUM, range.confidence)
        val width = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end).toInt()
        assertTrue(width >= 4, "expected wider range, got width=$width")
    }

    @Test
    fun `highly variable cycles produce LOW confidence and a wide range`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-22"),  // 21
            cycle("2026-01-22", "2026-01-26", "2026-02-26"),  // 35
            cycle("2026-02-26", "2026-03-02", "2026-04-10"),  // 43
            cycle("2026-04-10", "2026-04-14", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)!!
        assertEquals(PredictionRange.Confidence.LOW, range.confidence)
    }

    @Test
    fun `predicted range is centered on median next-start date`() {
        // All 28-day cycles → predicted next start = active.start + 28
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),
            cycle("2026-02-26", "2026-03-02", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)!!
        val expected = LocalDate.parse("2026-03-26")
        val midpoint = range.start.plusDays(
            java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end) / 2
        )
        // The expected date must be within the range; allow off-by-one for rounding.
        assertTrue(
            !expected.isBefore(range.start.minusDays(1)) &&
            !expected.isAfter(range.end.plusDays(1)),
            "expected $expected to be near range center, got [$range]"
        )
    }
}
