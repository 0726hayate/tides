package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Phase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `phase is computed for non-ovulation-relevant goals (v1_3 gate removal)`() {
        // v1.3 removed the goal-based gate. With non-hormonal BC and a
        // valid active cycle, phase + ovulation window are computed
        // regardless of goal selection.
        val completed = cycle("2026-05-01", "2026-05-04", "2026-05-29")
        val active = cycle("2026-05-29", "2026-06-02", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(completed, active),
            today = LocalDate.parse("2026-06-10"),
            birthControl = nonHormonal,
            goals = noOvulationGoals,
        )
        assertNotNull(result)
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

    @Test
    fun `assumeCycleLength 28 with single active cycle returns ovulation window day 12-16`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(active),
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
            assumeCycleLength = 28,
        )
        assertEquals(LocalDate.parse("2026-05-12"), result?.ovulationWindow?.start)
        assertEquals(LocalDate.parse("2026-05-16"), result?.ovulationWindow?.end)
    }

    @Test
    fun `assumeCycleLength 40 returns null (outside trust band)`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(active),
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
            assumeCycleLength = 40,
        )
        assertNull(result)
    }

    @Test
    fun `assumeCycleLength is ignored when completed cycles exist`() {
        val cycles = listOf(
            cycle("2026-03-01", "2026-03-04", "2026-03-29"),
            cycle("2026-03-29", "2026-04-01", "2026-04-26"),
            cycle("2026-04-26", "2026-04-29", null),
        )
        val ignoring = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
            assumeCycleLength = 35,
        )
        val baseline = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )
        assertEquals(baseline?.ovulationWindow, ignoring?.ovulationWindow)
    }

    @Test
    fun `project returns three cycles with correct stride`() {
        val cycles = listOf(
            cycle("2026-03-01", "2026-03-04", "2026-03-29"),
            cycle("2026-03-29", "2026-04-01", "2026-04-26"),
            cycle("2026-04-26", "2026-04-29", null),
        )
        val projections = PhaseCalculator.project(
            cycles = cycles,
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            cyclesAhead = 3,
        )
        assertEquals(3, projections.size)
        // Active starts 2026-04-26; median cycle length from completed = 28d.
        // ovulationDay = 28 - 14 = 14, so ov window = days 12..16 from start.
        assertEquals(LocalDate.parse("2026-04-26"), projections[0].periodRange.start)
        assertEquals(LocalDate.parse("2026-05-07"), projections[0].ovulationRange.start)
        // Second projection stride: +28 days.
        assertEquals(LocalDate.parse("2026-05-24"), projections[1].periodRange.start)
        assertEquals(LocalDate.parse("2026-06-04"), projections[1].ovulationRange.start)
    }

    @Test
    fun `project returns empty on hormonal bc`() {
        val cycles = listOf(
            cycle("2026-03-01", "2026-03-04", "2026-03-29"),
            cycle("2026-03-29", "2026-04-01", null),
        )
        val projections = PhaseCalculator.project(
            cycles = cycles,
            today = LocalDate.parse("2026-04-15"),
            birthControl = BirthControlMethod.PILL,
            cyclesAhead = 3,
        )
        assertEquals(0, projections.size)
    }

    @Test
    fun `project returns empty without active cycle`() {
        val projections = PhaseCalculator.project(
            cycles = emptyList(),
            today = LocalDate.parse("2026-04-15"),
            birthControl = nonHormonal,
            cyclesAhead = 3,
        )
        assertEquals(0, projections.size)
    }
}
