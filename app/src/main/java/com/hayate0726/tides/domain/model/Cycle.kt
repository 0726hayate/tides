package com.hayate0726.tides.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One completed (or in-progress) menstrual cycle.
 *
 * - `start`: first day of the period that began this cycle
 * - `periodEnd`: last day with non-zero flow in this cycle (null if still bleeding)
 * - `nextStart`: first day of the *next* period (null if current cycle still active)
 *
 * `length` = days from `start` to `nextStart` (exclusive). Null if active.
 * `periodLength` = days from `start` to `periodEnd` inclusive. Null if still bleeding.
 */
data class Cycle(
    val start: LocalDate,
    val periodEnd: LocalDate?,
    val nextStart: LocalDate?,
) {
    init {
        if (periodEnd != null) require(!periodEnd.isBefore(start)) {
            "periodEnd must be >= start"
        }
        if (nextStart != null) require(nextStart.isAfter(start)) {
            "nextStart must be > start"
        }
        if (nextStart != null && periodEnd != null) require(!periodEnd.isAfter(nextStart)) {
            "periodEnd must be <= nextStart"
        }
    }

    val length: Int?
        get() = nextStart?.let { ChronoUnit.DAYS.between(start, it).toInt() }

    val periodLength: Int?
        get() = periodEnd?.let { ChronoUnit.DAYS.between(start, it).toInt() + 1 }

    val isActive: Boolean get() = nextStart == null
}
