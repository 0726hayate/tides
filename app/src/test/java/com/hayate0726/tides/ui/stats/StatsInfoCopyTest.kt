package com.hayate0726.tides.ui.stats

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatsInfoCopyTest {

    @Test
    fun `every chart has non-empty title and body`() {
        val all = listOf(
            StatsInfoCopy.cycleLength,
            StatsInfoCopy.periodLength,
            StatsInfoCopy.symptomHeatmap,
            StatsInfoCopy.symptomFrequency,
            StatsInfoCopy.insightCard,
        )
        for (entry in all) {
            assertTrue(entry.title.isNotBlank(), "title blank: ${entry.title}")
            assertTrue(entry.body.isNotBlank(), "body blank for: ${entry.title}")
        }
    }

    @Test
    fun `cited charts include at least one source`() {
        assertTrue(StatsInfoCopy.cycleLength.sources.isNotEmpty())
        assertTrue(StatsInfoCopy.periodLength.sources.isNotEmpty())
    }
}
