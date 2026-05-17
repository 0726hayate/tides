package com.hayate0726.tides.ui.stats

import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.model.Symptom

/**
 * Generates at most one reflective observation per call. Returns null when
 * there's nothing notable to say (per spec §5.6 — never surface filler).
 */
object InsightGenerator {

    fun generate(stats: CycleStats, topSymptomCycleDays: Map<Symptom, Int>): String? {
        if (stats.completedCycleCount < 3) return null
        when (stats.regularity) {
            CycleStats.Regularity.VERY_REGULAR ->
                return "Your cycles have been very regular over the last ${stats.completedCycleCount}."
            CycleStats.Regularity.HIGHLY_VARIABLE ->
                return "Your cycle length has varied by ${stats.cycleLengthRange} days. This can be normal but worth tracking."
            else -> {}
        }
        when (stats.periodLengthTrend) {
            CycleStats.Trend.DECREASING ->
                return "Your period has been getting shorter over time."
            CycleStats.Trend.INCREASING ->
                return "Your period has been getting longer over time."
            else -> {}
        }
        topSymptomCycleDays.entries.firstOrNull()?.let { (sym, day) ->
            return "${sym.name.lowercase().replace('_', ' ')
                .replaceFirstChar { it.uppercase() }} is most often logged around cycle day $day."
        }
        return null
    }
}
