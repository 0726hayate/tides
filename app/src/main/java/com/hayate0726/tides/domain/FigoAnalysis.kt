package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Detects FIGO-aligned clinical patterns to surface in the doctor PDF.
 *
 * Per spec §5.7 and ACOG #651 / Munro 2018:
 *  - CYCLE_FREQUENT: median cycle <24 days
 *  - CYCLE_INFREQUENT: median cycle >38 days
 *  - CYCLE_IRREGULAR: max-min variation >7 days across the analyzed range
 *  - PERIOD_PROLONGED: period duration >8 days in any cycle
 *  - HEAVY_FLOW: HEAVY flow logged in ≥2 cycles
 *  - INTERMENSTRUAL_BLEEDING: any bleeding logged between defined period ranges
 *  - SEVERE_DYSMENORRHEA: any pain entry ≥7/10 (NRS)
 *  - AMENORRHEA: ≥90 days since last period start with no new period
 *
 * All findings are descriptive — never diagnostic. The PDF wraps these in
 * neutral language per the spec.
 */
object FigoAnalysis {

    data class FlowEntry(val date: LocalDate, val flow: FlowIntensity)
    data class PainEntry(val date: LocalDate, val severityNrs: Int) {
        init { require(severityNrs in 0..10) }
    }

    enum class Pattern {
        CYCLE_FREQUENT,
        CYCLE_INFREQUENT,
        CYCLE_IRREGULAR,
        PERIOD_PROLONGED,
        HEAVY_FLOW,
        INTERMENSTRUAL_BLEEDING,
        SEVERE_DYSMENORRHEA,
        AMENORRHEA,
    }

    fun analyze(
        cycles: List<Cycle>,
        cycleFlowEntries: List<FlowEntry>,
        painEntries: List<PainEntry>,
        intermenstrualBleedingDates: List<LocalDate>,
        today: LocalDate,
    ): Set<Pattern> {
        require(cycles.zipWithNext().all { (a, b) -> !b.start.isBefore(a.start) }) {
            "cycles must be sorted ascending by start"
        }
        val found = mutableSetOf<Pattern>()
        val completed = cycles.filter { !it.isActive }
        val lengths = completed.mapNotNull { it.length }

        if (lengths.isNotEmpty()) {
            val median = lengths.sorted()[(lengths.size - 1) / 2]
            if (median < 24) found += Pattern.CYCLE_FREQUENT
            if (median > 38) found += Pattern.CYCLE_INFREQUENT
            val variance = lengths.max() - lengths.min()
            if (variance > 7) found += Pattern.CYCLE_IRREGULAR
        }

        if (completed.any { (it.periodLength ?: 0) > 8 }) {
            found += Pattern.PERIOD_PROLONGED
        }

        val heavyCycles = cycles.count { c ->
            cycleFlowEntries.any { e ->
                !e.date.isBefore(c.start) &&
                    (c.nextStart == null || e.date.isBefore(c.nextStart)) &&
                    FlowIntensity.isHeavy(e.flow)
            }
        }
        if (heavyCycles >= 2) found += Pattern.HEAVY_FLOW

        if (intermenstrualBleedingDates.isNotEmpty()) {
            found += Pattern.INTERMENSTRUAL_BLEEDING
        }

        if (painEntries.any { it.severityNrs >= 7 }) {
            found += Pattern.SEVERE_DYSMENORRHEA
        }

        val mostRecentStart = cycles.maxOfOrNull { it.start }
        if (mostRecentStart != null) {
            val daysSince = ChronoUnit.DAYS.between(mostRecentStart, today)
            val activeAndNoNew = cycles.any { it.start == mostRecentStart && it.isActive }
            if (activeAndNoNew && daysSince >= 90) {
                found += Pattern.AMENORRHEA
            }
        }

        return found
    }
}
