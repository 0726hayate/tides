package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FigoAnalysisTest {

    private val today = LocalDate.parse("2026-06-01")

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(LocalDate.parse(start), LocalDate.parse(periodEnd), nextStart?.let { LocalDate.parse(it) })

    @Test
    fun `regular cycles flag no FIGO patterns`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 28, 5
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 28, 5
            cycle("2026-02-26", "2026-03-02", "2026-03-26"),  // 28, 5
        )
        val patterns = FigoAnalysis.analyze(
            cycles = cycles,
            cycleFlowEntries = emptyList(),
            painEntries = emptyList(),
            intermenstrualBleedingDates = emptyList(),
            today = today,
        )
        assertTrue(patterns.isEmpty(), "expected no patterns, got $patterns")
    }

    @Test
    fun `frequent cycles (less than 24 days) flag CYCLE_FREQUENT`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-22"),  // 21
            cycle("2026-01-22", "2026-01-26", "2026-02-12"),  // 21
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList(), today = today)
        assertTrue(patterns.contains(FigoAnalysis.Pattern.CYCLE_FREQUENT))
    }

    @Test
    fun `infrequent cycles (over 38 days) flag CYCLE_INFREQUENT`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-02-15"),  // 45
            cycle("2026-02-15", "2026-02-19", "2026-04-01"),  // 45
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList(), today = today)
        assertTrue(patterns.contains(FigoAnalysis.Pattern.CYCLE_INFREQUENT))
    }

    @Test
    fun `variation greater than 7 days flags CYCLE_IRREGULAR`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-23"),  // 22
            cycle("2026-01-23", "2026-01-27", "2026-02-23"),  // 31
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList(), today = today)
        assertTrue(patterns.contains(FigoAnalysis.Pattern.CYCLE_IRREGULAR))
    }

    @Test
    fun `period longer than 8 days flags PERIOD_PROLONGED`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-10", "2026-01-29"),  // 9-day period
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList(), today = today)
        assertTrue(patterns.contains(FigoAnalysis.Pattern.PERIOD_PROLONGED))
    }

    @Test
    fun `intermenstrual bleeding flags INTERMENSTRUAL_BLEEDING`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            emptyList(),
            listOf(LocalDate.parse("2026-01-15")),
            today = today,
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.INTERMENSTRUAL_BLEEDING))
    }

    @Test
    fun `severe dysmenorrhea (pain greater than equal to 7) flags SEVERE_DYSMENORRHEA`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            painEntries = listOf(
                FigoAnalysis.PainEntry(LocalDate.parse("2026-01-01"), 8),
            ),
            intermenstrualBleedingDates = emptyList(),
            today = today,
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.SEVERE_DYSMENORRHEA))
    }

    @Test
    fun `heavy flow logged repeatedly flags HEAVY_FLOW`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            cycleFlowEntries = listOf(
                FigoAnalysis.FlowEntry(LocalDate.parse("2026-01-01"), FlowIntensity.HEAVY),
                FigoAnalysis.FlowEntry(LocalDate.parse("2026-01-29"), FlowIntensity.HEAVY),
            ),
            painEntries = emptyList(),
            intermenstrualBleedingDates = emptyList(),
            today = today,
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.HEAVY_FLOW))
    }

    @Test
    fun `amenorrhea 90+ days flags AMENORRHEA`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", null),  // active, never had next period
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            emptyList(),
            emptyList(),
            today = LocalDate.parse("2026-05-01"),  // 120 days later
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.AMENORRHEA))
    }

    @Test
    fun `amenorrhea under 90 days does NOT flag`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", null),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            emptyList(),
            emptyList(),
            today = LocalDate.parse("2026-03-01"),  // 59 days later
        )
        assertFalse(patterns.contains(FigoAnalysis.Pattern.AMENORRHEA))
    }
}
