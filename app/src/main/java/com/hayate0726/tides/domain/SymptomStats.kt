package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Aggregates symptom data into frequency counts and cycle-day heatmaps.
 *
 * Spec §4: OTHER symptoms are excluded from `frequency` and `cycleDayHeatmap`
 * because their `other_text` content is free-form. They surface only in
 * the doctor PDF appendix (handled in Plan 4).
 */
data class SymptomStats(
    /** Symptom -> number of log entries. Excludes OTHER. */
    val frequency: Map<Symptom, Int>,
    /** Symptom -> (cycle day 1-based -> count of entries on that cycle day). */
    val cycleDayHeatmap: Map<Symptom, Map<Int, Int>>,
) {
    fun topSymptoms(n: Int): List<Symptom> =
        frequency.entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key }

    data class Entry(val date: LocalDate, val symptom: Symptom)

    companion object {

        fun compute(entries: List<Entry>, cycles: List<Cycle>): SymptomStats {
            val freq = mutableMapOf<Symptom, Int>()
            for (e in entries) {
                if (e.symptom.isFreeText) continue
                freq[e.symptom] = (freq[e.symptom] ?: 0) + 1
            }

            val heat = mutableMapOf<Symptom, MutableMap<Int, Int>>()
            for (e in entries) {
                if (e.symptom.isFreeText) continue
                val containing = cycles.firstOrNull { c ->
                    val end = c.nextStart?.minusDays(1) ?: e.date
                    !e.date.isBefore(c.start) && !e.date.isAfter(end)
                } ?: continue
                val cycleDay = ChronoUnit.DAYS.between(containing.start, e.date).toInt() + 1
                val inner = heat.getOrPut(e.symptom) { mutableMapOf() }
                inner[cycleDay] = (inner[cycleDay] ?: 0) + 1
            }

            return SymptomStats(frequency = freq, cycleDayHeatmap = heat)
        }
    }
}
