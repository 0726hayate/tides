package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle

/**
 * Aggregate statistics over a series of cycles.
 *
 * "Median" uses the lower-of-two-middles convention for even-length lists.
 * Regularity buckets are FIGO-aligned per spec §5.6.
 */
data class CycleStats(
    val medianCycleLength: Int?,
    val cycleLengthMin: Int?,
    val cycleLengthMax: Int?,
    /** Spread = max - min cycle length, in days. Not statistical variance. */
    val cycleLengthRange: Int?,
    val medianPeriodLength: Int?,
    val regularity: Regularity?,
    val periodLengthTrend: Trend,
    val completedCycleCount: Int,
    val hasActiveCycle: Boolean,
) {
    enum class Regularity { VERY_REGULAR, MODERATELY_VARIABLE, HIGHLY_VARIABLE }
    enum class Trend { INCREASING, DECREASING, STABLE, UNKNOWN }

    companion object {

        fun compute(cycles: List<Cycle>): CycleStats {
            require(cycles.zipWithNext().all { (a, b) -> !b.start.isBefore(a.start) }) {
                "cycles must be sorted ascending by start"
            }
            val completed = cycles.filter { !it.isActive }
            val active = cycles.firstOrNull { it.isActive }

            val cycleLens = completed.mapNotNull { it.length }
            val periodLens = completed.mapNotNull { it.periodLength }

            val medianCycle = cycleLens.medianOrNull()
            val minCycle = cycleLens.minOrNull()
            val maxCycle = cycleLens.maxOrNull()
            val variance = if (minCycle != null && maxCycle != null) maxCycle - minCycle else null

            val regularity = variance?.let {
                when {
                    it <= 2 -> Regularity.VERY_REGULAR
                    it <= 7 -> Regularity.MODERATELY_VARIABLE
                    else -> Regularity.HIGHLY_VARIABLE
                }
            }

            return CycleStats(
                medianCycleLength = medianCycle,
                cycleLengthMin = minCycle,
                cycleLengthMax = maxCycle,
                cycleLengthRange = variance,
                medianPeriodLength = periodLens.medianOrNull(),
                regularity = regularity,
                periodLengthTrend = trendOf(periodLens),
                completedCycleCount = completed.size,
                hasActiveCycle = active != null,
            )
        }

        /**
         * Simple two-half comparison. If the back half average is >0.5 day lower than
         * the front half, that's DECREASING; >0.5 day higher is INCREASING; else STABLE.
         * Requires >=4 data points.
         */
        private fun trendOf(values: List<Int>): Trend {
            if (values.size < 4) return Trend.UNKNOWN
            val mid = values.size / 2
            val front = values.subList(0, mid).average()
            val back = values.subList(values.size - mid, values.size).average()
            val delta = back - front
            return when {
                delta < -0.5 -> Trend.DECREASING
                delta > 0.5 -> Trend.INCREASING
                else -> Trend.STABLE
            }
        }

        private fun List<Int>.medianOrNull(): Int? {
            if (isEmpty()) return null
            val sorted = sorted()
            return sorted[(sorted.size - 1) / 2]
        }
    }
}
