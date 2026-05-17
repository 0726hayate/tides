package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Phase
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
 *
 * Phase boundaries (default 28-day cycle; scales proportionally for other lengths):
 *  - MENSTRUAL: cycle days 1 .. periodEnd-day-of-cycle (typically 1–5)
 *  - FOLLICULAR: periodEnd+1 .. ovulationStart-1
 *  - OVULATION: ovulationStart .. ovulationStart+2  (3-day window centered on day 14 of 28)
 *  - LUTEAL: ovulationEnd+1 .. cycle end
 */
object PhaseCalculator {

    private const val DEFAULT_CYCLE_LENGTH = 28
    private const val OVULATION_DAY_OF_DEFAULT = 14
    private const val OVULATION_WINDOW_HALF_WIDTH = 1

    data class Result(
        val currentPhase: Phase,
        val ovulationWindow: ClosedRange<LocalDate>,
    )

    fun compute(
        cycles: List<Cycle>,
        today: LocalDate,
        birthControl: BirthControlMethod,
        goals: Set<Goal>,
    ): Result? {
        if (birthControl.isHormonal) return null
        if (goals.intersect(Goal.OVULATION_RELEVANT).isEmpty()) return null

        val active = cycles.firstOrNull { it.isActive } ?: return null
        val completed = cycles.filter { !it.isActive }.mapNotNull { it.length }
        if (completed.isEmpty()) return null

        val medianLength = completed.sorted().let { it[(it.size - 1) / 2] }
        val ovulationDayOfCycle = (OVULATION_DAY_OF_DEFAULT.toDouble() * medianLength / DEFAULT_CYCLE_LENGTH).toInt()
            .coerceIn(1, medianLength)

        val cycleDay = ChronoUnit.DAYS.between(active.start, today).toInt() + 1
        if (cycleDay < 1) return null

        val periodLastDayOfCycle = active.periodLength ?: 5  // default if still bleeding

        val phase = when {
            cycleDay <= periodLastDayOfCycle -> Phase.MENSTRUAL
            cycleDay < ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH -> Phase.FOLLICULAR
            cycleDay <= ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH -> Phase.OVULATION
            else -> Phase.LUTEAL
        }

        val ovStart = active.start.plusDays((ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH - 1).toLong())
        val ovEnd = active.start.plusDays((ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH - 1).toLong())
        return Result(currentPhase = phase, ovulationWindow = ovStart..ovEnd)
    }
}
