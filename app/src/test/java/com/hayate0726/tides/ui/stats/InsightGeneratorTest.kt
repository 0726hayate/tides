package com.hayate0726.tides.ui.stats

import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.model.Cycle
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InsightGeneratorTest {

    @Test
    fun no_completed_cycles_no_insight() {
        val stats = CycleStats.compute(emptyList())
        assertNull(InsightGenerator.generate(stats, emptyMap()))
    }

    @Test
    fun very_regular_cycles_produces_regularity_insight() {
        val cycles = (0..5).map {
            Cycle(
                start = LocalDate.parse("2026-01-01").plusDays(28L * it),
                periodEnd = LocalDate.parse("2026-01-04").plusDays(28L * it),
                nextStart = LocalDate.parse("2026-01-29").plusDays(28L * it),
            )
        }
        val stats = CycleStats.compute(cycles)
        val insight = InsightGenerator.generate(stats, emptyMap())
        assertNotNull(insight)
    }
}
