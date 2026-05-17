package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Phase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PhaseCalculatorTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            LocalDate.parse(start),
            LocalDate.parse(periodEnd),
            nextStart?.let { LocalDate.parse(it) }
        )

    private val nonHormonal = BirthControlMethod.NONE
    private val hormonal = BirthControlMethod.PILL
    private val ovulationGoals = setOf(Goal.AVOID_PREGNANCY)
    private val noOvulationGoals = setOf(Goal.TRACK_PERIOD)

    @Test
    fun `phase is null when bc method is hormonal regardless of goals`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(active),
            today = LocalDate.parse("2026-05-14"),
            birthControl = hormonal,
            goals = ovulationGoals,
        )
        assertNull(result)
    }

    @Test
    fun `phase is null when goals do not include ovulation-relevant`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(active),
            today = LocalDate.parse("2026-05-14"),
            birthControl = nonHormonal,
            goals = noOvulationGoals,
        )
        assertNull(result)
    }

    @Test
    fun `during period day 1-4 phase is MENSTRUAL`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.MENSTRUAL, result.currentPhase)
    }

    @Test
    fun `around day 14 (median 28-day cycle) phase is OVULATION`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-12"), // active cycle day 14
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.OVULATION, result.currentPhase)
    }

    @Test
    fun `between menstrual and ovulation phase is FOLLICULAR`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-06"), // active cycle day 8
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.FOLLICULAR, result.currentPhase)
    }

    @Test
    fun `after ovulation phase is LUTEAL`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-20"), // active cycle day 22
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.LUTEAL, result.currentPhase)
    }

    @Test
    fun `ovulation window is a contiguous 5-day span`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-12"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        val window = result.ovulationWindow
        assertEquals(4L, ChronoUnit.DAYS.between(window.start, window.end))
    }

    @Test
    fun `phase is suppressed when median cycle length is outside 21-35 days`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-02-10"),  // 40-day cycle
            cycle("2026-02-10", "2026-02-14", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-03-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )
        assertNull(result)
    }

    @Test
    fun `requires cycles sorted ascending by start`() {
        val unsorted = listOf(
            cycle("2026-04-29", "2026-05-02", null),
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
        )
        assertThrows(IllegalArgumentException::class.java) {
            PhaseCalculator.compute(
                cycles = unsorted,
                today = LocalDate.parse("2026-05-12"),
                birthControl = nonHormonal,
                goals = ovulationGoals,
            )
        }
    }

    @Test
    fun `single completed cycle with no active uses heuristic 28 days`() {
        // Only one completed cycle, no active → no inference possible
        val result = PhaseCalculator.compute(
            cycles = listOf(cycle("2026-04-01", "2026-04-04", "2026-04-29")),
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )
        assertNull(result)
    }
}
