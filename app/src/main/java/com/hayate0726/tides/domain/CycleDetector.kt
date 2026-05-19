package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
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
 *  - When the user is on a hormonal birth control method (PILL, HORMONAL_IUD,
 *    IMPLANT, PATCH, RING), SPOTTING entries are treated as non-bleeding so
 *    breakthrough spotting does not seed phantom short cycles. LIGHT/MEDIUM/
 *    HEAVY flow continues to count as bleeding (withdrawal bleeds remain visible).
 */
object CycleDetector {

    data class Entry(val date: LocalDate, val flow: FlowIntensity)

    /** Max allowed gap (days) within a single period — gaps longer split the period. */
    private const val MAX_INTRA_PERIOD_GAP_DAYS = 1L

    /**
     * @param activeBirthControl the user's currently-active BC method, if known. When
     *   the method is hormonal, SPOTTING entries are dropped before cycle detection
     *   so breakthrough spotting on hormonal contraception does not generate phantom
     *   cycles. Pass `null` when the BC context is unknown (preserves legacy behavior).
     */
    fun detect(
        entries: List<Entry>,
        activeBirthControl: BirthControlMethod? = null,
    ): List<Cycle> {
        val filteredEntries = if (activeBirthControl?.isHormonal == true) {
            entries.filter { it.flow != FlowIntensity.SPOTTING }
        } else {
            entries
        }

        val bleedingDays = filteredEntries
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
