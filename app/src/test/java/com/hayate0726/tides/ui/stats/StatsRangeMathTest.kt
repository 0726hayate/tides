package com.hayate0726.tides.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsRangeMathTest {

    @Test fun three_mo_window_subtracts_3_months_from_today() {
        val to = LocalDate.of(2026, 5, 17)
        val from = StatsViewModel.Range.THREE_MO.months?.let { to.minusMonths(it.toLong()) }
        assertEquals(LocalDate.of(2026, 2, 17), from)
    }

    @Test fun six_mo_window_subtracts_6_months_from_today() {
        val to = LocalDate.of(2026, 5, 17)
        val from = StatsViewModel.Range.SIX_MO.months?.let { to.minusMonths(it.toLong()) }
        assertEquals(LocalDate.of(2025, 11, 17), from)
    }

    @Test fun one_yr_window_subtracts_12_months_from_today() {
        val to = LocalDate.of(2026, 5, 17)
        val from = StatsViewModel.Range.ONE_YR.months?.let { to.minusMonths(it.toLong()) }
        assertEquals(LocalDate.of(2025, 5, 17), from)
    }

    @Test fun all_window_has_null_months() {
        assertEquals(null, StatsViewModel.Range.ALL.months)
    }
}
