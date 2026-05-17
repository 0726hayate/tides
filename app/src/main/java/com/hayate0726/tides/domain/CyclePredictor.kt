package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.PredictionRange
import java.time.LocalDate

/**
 * Predicts the next period as a range, with a coarse confidence bucket
 * derived from cycle-length variance.
 *
 * Rules (spec §5.5, §10):
 *  - Requires ≥2 completed cycles plus an active cycle (or just ≥2 completed
 *    cycles if the most recent has finished).
 *  - Predicted median next-start = active cycle start + median completed length.
 *  - Range width = max(2, ceil(variance / 2)) days on each side.
 *  - Confidence: variance ≤2 → HIGH, ≤7 → MEDIUM, >7 → LOW.
 */
object CyclePredictor {

    fun predictNextPeriod(cycles: List<Cycle>): PredictionRange? {
        val completed = cycles.filter { !it.isActive }.mapNotNull { c -> c.length?.let { c to it } }
        if (completed.size < 2) return null

        val lengths = completed.map { it.second }.sorted()
        val median = lengths[(lengths.size - 1) / 2]
        val variance = lengths.max() - lengths.min()

        val active = cycles.firstOrNull { it.isActive } ?: completed.last().first

        val center = active.start.plusDays(median.toLong())
        val halfWidth = maxOf(2, (variance + 1) / 2)
        val start = center.minusDays(halfWidth.toLong())
        val end = center.plusDays(halfWidth.toLong())

        val confidence = when {
            variance <= 2 -> PredictionRange.Confidence.HIGH
            variance <= 7 -> PredictionRange.Confidence.MEDIUM
            else -> PredictionRange.Confidence.LOW
        }
        return PredictionRange(start, end, confidence)
    }
}
