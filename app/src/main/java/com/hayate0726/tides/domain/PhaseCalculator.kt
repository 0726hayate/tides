package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Phase
import com.hayate0726.tides.domain.model.PredictionRange
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Computes the current cycle phase if it's meaningful to display.
 *
 * Returns null (phase suppressed) when:
 *  - The user is on hormonal contraception (PILL, HORMONAL_IUD, IMPLANT, PATCH, RING).
 *  - None of the user's goals include "Avoid pregnancy" or "Trying to conceive."
 *  - There's no active cycle.
 *  - There's no completed cycle to estimate a median length from.
 *  - The personal median cycle length is outside 21..35 days, where the
 *    fixed-luteal heuristic below is least reliable (per FSRH guidance and
 *    Henry et al. 2024 Hum Reprod showing luteal-phase variation).
 *
 * Ovulation estimate:
 *   ovulationDay = medianCycleLength - LUTEAL_PHASE_DAYS  (clinical fixed-luteal)
 * Window: ±2 days around ovulationDay (5-day fertile-window approximation).
 *
 * Period-last-day:
 *   - If >=3 completed cycles with recorded periodLength: use their median.
 *   - Else: fall back to COLD_START_PERIOD_LAST_DAY.
 */
object PhaseCalculator {

    /** Clinical "fixed" luteal phase. FSRH / Cleveland Clinic standard. */
    private const val LUTEAL_PHASE_DAYS = 14

    /** ±2 days around ovulationDay -> 5-day fertile window. */
    private const val OVULATION_WINDOW_HALF_WIDTH = 2

    /** Cold-start fallback when we have <3 completed period lengths. */
    private const val COLD_START_PERIOD_LAST_DAY = 5
    private const val MIN_CYCLES_FOR_PERSONAL_MEDIAN = 3

    /** Inclusive bounds outside which we suppress phase prediction. */
    private const val MIN_TRUSTED_CYCLE_LENGTH = 21
    private const val MAX_TRUSTED_CYCLE_LENGTH = 35

    data class Result(
        val currentPhase: Phase,
        val ovulationWindow: PredictionRange,
    )

    fun compute(
        cycles: List<Cycle>,
        today: LocalDate,
        birthControl: BirthControlMethod,
        goals: Set<Goal>,
    ): Result? {
        require(cycles.zipWithNext().all { (a, b) -> !b.start.isBefore(a.start) }) {
            "cycles must be sorted ascending by start"
        }
        if (birthControl.isHormonal) return null
        if (goals.intersect(Goal.OVULATION_RELEVANT).isEmpty()) return null

        val active = cycles.firstOrNull { it.isActive } ?: return null
        val completedLengths = cycles.filter { !it.isActive }.mapNotNull { it.length }
        if (completedLengths.isEmpty()) return null

        val medianLength = lowMedian(completedLengths)
        if (medianLength !in MIN_TRUSTED_CYCLE_LENGTH..MAX_TRUSTED_CYCLE_LENGTH) return null

        val ovulationDayOfCycle = medianLength - LUTEAL_PHASE_DAYS

        val cycleDay = ChronoUnit.DAYS.between(active.start, today).toInt() + 1
        if (cycleDay < 1) return null

        val completedPeriodLengths = cycles.filter { !it.isActive }.mapNotNull { it.periodLength }
        val periodLastDayOfCycle =
            if (completedPeriodLengths.size >= MIN_CYCLES_FOR_PERSONAL_MEDIAN) {
                lowMedian(completedPeriodLengths)
            } else {
                COLD_START_PERIOD_LAST_DAY
            }

        val phase = when {
            cycleDay <= periodLastDayOfCycle -> Phase.MENSTRUAL
            cycleDay < ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH -> Phase.FOLLICULAR
            cycleDay <= ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH -> Phase.OVULATION
            else -> Phase.LUTEAL
        }

        val ovStart = active.start.plusDays((ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH - 1).toLong())
        val ovEnd = active.start.plusDays((ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH - 1).toLong())
        val confidence = confidenceFromHistory(completedLengths.size)
        return Result(
            currentPhase = phase,
            ovulationWindow = PredictionRange(start = ovStart, end = ovEnd, confidence = confidence),
        )
    }

    private fun lowMedian(values: List<Int>): Int =
        values.sorted().let { it[(it.size - 1) / 2] }

    private fun confidenceFromHistory(completedCount: Int): PredictionRange.Confidence = when {
        completedCount >= 6 -> PredictionRange.Confidence.HIGH
        completedCount >= 3 -> PredictionRange.Confidence.MEDIUM
        else -> PredictionRange.Confidence.LOW
    }
}
