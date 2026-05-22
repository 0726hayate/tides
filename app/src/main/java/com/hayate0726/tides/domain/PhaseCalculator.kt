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
 *  - There's no active cycle.
 *  - There's no completed cycle to estimate a median length from.
 *  - The personal median cycle length is outside 21..35 days, where the
 *    fixed-luteal heuristic below is least reliable (per FSRH guidance and
 *    Henry et al. 2024 Hum Reprod showing luteal-phase variation).
 *  - `assumeCycleLength` (optional) provides a fallback median length when no
 *    completed cycles exist yet. Used by the onboarding prediction preview.
 *
 * As of v1.3 the goal-based gate (AVOID_PREGNANCY / TRYING_TO_CONCEIVE) is
 * removed — predictions are shown for everyone not on hormonal BC. The
 * `goals` parameter remains on the signature for callsite stability.
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
        assumeCycleLength: Int? = null,
    ): Result? {
        require(cycles.zipWithNext().all { (a, b) -> !b.start.isBefore(a.start) }) {
            "cycles must be sorted ascending by start"
        }
        if (birthControl.isHormonal) return null

        val active = cycles.firstOrNull { it.isActive } ?: return null
        val completedLengths = cycles.filter { !it.isActive }.mapNotNull { it.length }
        val medianLength = when {
            completedLengths.isNotEmpty() -> lowMedian(completedLengths)
            assumeCycleLength != null -> assumeCycleLength
            else -> return null
        }
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
        val confidence = if (completedLengths.isEmpty()) {
            PredictionRange.Confidence.LOW
        } else {
            confidenceFromHistory(completedLengths.size)
        }
        return Result(
            currentPhase = phase,
            ovulationWindow = PredictionRange(start = ovStart, end = ovEnd, confidence = confidence),
        )
    }

    /**
     * Forward-projects [cyclesAhead] cycles starting from the user's active
     * cycle, returning each cycle's predicted period range and ovulation
     * window. Returns an empty list when phase prediction would be suppressed
     * (hormonal BC, no active cycle, no median length, length out of trust
     * range). Confidence drops with each subsequent projected cycle since
     * each projection compounds the median-length estimate's error.
     */
    fun project(
        cycles: List<Cycle>,
        today: LocalDate,
        birthControl: BirthControlMethod,
        cyclesAhead: Int = 3,
        assumeCycleLength: Int? = null,
    ): List<ProjectedCycle> {
        if (cyclesAhead <= 0) return emptyList()
        if (birthControl.isHormonal) return emptyList()
        val active = cycles.firstOrNull { it.isActive } ?: return emptyList()

        val completedLengths = cycles.filter { !it.isActive }.mapNotNull { it.length }
        val medianLength = when {
            completedLengths.isNotEmpty() -> lowMedian(completedLengths)
            assumeCycleLength != null -> assumeCycleLength
            else -> return emptyList()
        }
        if (medianLength !in MIN_TRUSTED_CYCLE_LENGTH..MAX_TRUSTED_CYCLE_LENGTH) {
            return emptyList()
        }

        val completedPeriodLengths = cycles.filter { !it.isActive }.mapNotNull { it.periodLength }
        val periodLastDayOfCycle =
            if (completedPeriodLengths.size >= MIN_CYCLES_FOR_PERSONAL_MEDIAN) {
                lowMedian(completedPeriodLengths)
            } else {
                COLD_START_PERIOD_LAST_DAY
            }
        val ovulationDayOfCycle = medianLength - LUTEAL_PHASE_DAYS

        val baseConfidence = if (completedLengths.isEmpty()) {
            PredictionRange.Confidence.LOW
        } else {
            confidenceFromHistory(completedLengths.size)
        }

        val out = mutableListOf<ProjectedCycle>()
        var cycleStart = active.start
        repeat(cyclesAhead) { i ->
            val periodLast = cycleStart.plusDays((periodLastDayOfCycle - 1).toLong())
            val ovStart = cycleStart.plusDays(
                (ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH - 1).toLong(),
            )
            val ovEnd = cycleStart.plusDays(
                (ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH - 1).toLong(),
            )
            // Confidence degrades with each forward projection — by cycle 3
            // we're stacking three median-length estimates.
            val confidence = when (i) {
                0 -> baseConfidence
                else -> PredictionRange.Confidence.LOW
            }
            out += ProjectedCycle(
                periodRange = cycleStart..periodLast,
                ovulationRange = ovStart..ovEnd,
                confidence = confidence,
            )
            cycleStart = cycleStart.plusDays(medianLength.toLong())
        }
        return out
    }

    data class ProjectedCycle(
        val periodRange: ClosedRange<LocalDate>,
        val ovulationRange: ClosedRange<LocalDate>,
        val confidence: PredictionRange.Confidence,
    )

    private fun lowMedian(values: List<Int>): Int =
        values.sorted().let { it[(it.size - 1) / 2] }

    private fun confidenceFromHistory(completedCount: Int): PredictionRange.Confidence = when {
        completedCount >= 6 -> PredictionRange.Confidence.HIGH
        completedCount >= 3 -> PredictionRange.Confidence.MEDIUM
        else -> PredictionRange.Confidence.LOW
    }
}
