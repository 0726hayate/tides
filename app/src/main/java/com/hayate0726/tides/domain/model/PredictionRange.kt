package com.hayate0726.tides.domain.model

import java.time.LocalDate

/**
 * A predicted range of dates (inclusive on both ends).
 *
 * `confidence` is a coarse bucket so the UI can render width.
 */
data class PredictionRange(
    val start: LocalDate,
    val end: LocalDate,
    val confidence: Confidence,
) {
    init {
        require(!end.isBefore(start)) { "end must be >= start" }
    }

    enum class Confidence { HIGH, MEDIUM, LOW }
}
