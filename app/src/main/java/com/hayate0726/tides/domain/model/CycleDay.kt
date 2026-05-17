package com.hayate0726.tides.domain.model

import java.time.LocalDate

@JvmInline
value class CycleDay(val value: Int) {
    init {
        require(value >= 1) { "cycle day is 1-indexed, got $value" }
    }

    companion object {
        fun of(startDate: LocalDate, date: LocalDate): CycleDay =
            CycleDay(java.time.temporal.ChronoUnit.DAYS.between(startDate, date).toInt() + 1)
    }
}
