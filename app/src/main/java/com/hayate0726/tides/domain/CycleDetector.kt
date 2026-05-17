package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Detects [Cycle] boundaries from a sparse list of daily flow entries.
 *
 * Rules (spec §5.2 + domain conventions):
 *  - Only entries with non-NONE flow count as "bleeding."
 *  - A cycle starts on the first bleeding day after a gap of >=2 days without bleeding.
 *  - A period ends on the last consecutive bleeding day (allowing a one-day gap).
 *  - The most recent cycle is "active" (nextStart = null) until a new cycle starts.
 */
object CycleDetector {

    data class Entry(val date: LocalDate, val flow: FlowIntensity)

    /** Max allowed gap (days) within a single period — gaps longer split the period. */
    private const val MAX_INTRA_PERIOD_GAP_DAYS = 1L

    fun detect(entries: List<Entry>): List<Cycle> {
        val bleedingDays = entries
            .asSequence()
            .filter { FlowIntensity.isBleeding(it.flow) }
            .map { it.date }
            .distinct()
            .sorted()
            .toList()

        if (bleedingDays.isEmpty()) return emptyList()

        // Group bleeding days into "period runs" allowing 1-day gaps.
        val periodRuns = mutableListOf<MutableList<LocalDate>>()
        var current = mutableListOf<LocalDate>().apply { add(bleedingDays[0]) }
        for (i in 1 until bleedingDays.size) {
            val gap = ChronoUnit.DAYS.between(bleedingDays[i - 1], bleedingDays[i])
            if (gap <= MAX_INTRA_PERIOD_GAP_DAYS + 1) {
                current.add(bleedingDays[i])
            } else {
                periodRuns.add(current)
                current = mutableListOf(bleedingDays[i])
            }
        }
        periodRuns.add(current)

        // Convert period runs into Cycle objects.
        val cycles = mutableListOf<Cycle>()
        for ((i, run) in periodRuns.withIndex()) {
            val start = run.first()
            val periodEnd = run.last()
            val nextStart = periodRuns.getOrNull(i + 1)?.first()
            cycles.add(Cycle(start = start, periodEnd = periodEnd, nextStart = nextStart))
        }
        return cycles
    }
}
