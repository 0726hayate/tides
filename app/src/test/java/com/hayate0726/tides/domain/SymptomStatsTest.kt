package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Symptom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SymptomStatsTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun entry(date: String, sym: Symptom) =
        SymptomStats.Entry(d(date), sym)

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            d(start),
            d(periodEnd),
            nextStart?.let { d(it) }
        )

    @Test
    fun `frequency counts each symptom`() {
        val entries = listOf(
            entry("2026-05-01", Symptom.CRAMPS),
            entry("2026-05-02", Symptom.CRAMPS),
            entry("2026-05-02", Symptom.HEADACHE),
        )
        val stats = SymptomStats.compute(entries, emptyList())
        assertEquals(2, stats.frequency[Symptom.CRAMPS])
        assertEquals(1, stats.frequency[Symptom.HEADACHE])
    }

    @Test
    fun `topSymptoms returns N most frequent`() {
        val entries = listOf(
            entry("2026-05-01", Symptom.CRAMPS),
            entry("2026-05-02", Symptom.CRAMPS),
            entry("2026-05-03", Symptom.CRAMPS),
            entry("2026-05-01", Symptom.HEADACHE),
            entry("2026-05-02", Symptom.HEADACHE),
            entry("2026-05-01", Symptom.BLOATING),
        )
        val stats = SymptomStats.compute(entries, emptyList())
        val top2 = stats.topSymptoms(2)
        assertEquals(listOf(Symptom.CRAMPS, Symptom.HEADACHE), top2)
    }

    @Test
    fun `OTHER symptoms are excluded from frequency stats`() {
        val entries = listOf(
            entry("2026-05-01", Symptom.OTHER),
            entry("2026-05-01", Symptom.CRAMPS),
        )
        val stats = SymptomStats.compute(entries, emptyList())
        assertTrue(stats.frequency[Symptom.OTHER] == null || stats.frequency[Symptom.OTHER] == 0)
        assertEquals(1, stats.frequency[Symptom.CRAMPS])
    }

    @Test
    fun `cycleDayHeatmap maps symptoms to cycle days`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),  // 28 days
            cycle("2026-04-29", "2026-05-02", "2026-05-27"),  // 28 days
        )
        val entries = listOf(
            // Cramps on day 1 of each cycle (4-1, 4-29)
            entry("2026-04-01", Symptom.CRAMPS),
            entry("2026-04-29", Symptom.CRAMPS),
            // Headache on day 14 of cycle 2 (5-12)
            entry("2026-05-12", Symptom.HEADACHE),
        )
        val stats = SymptomStats.compute(entries, cycles)
        val heat = stats.cycleDayHeatmap
        assertEquals(2, heat[Symptom.CRAMPS]?.get(1))
        assertEquals(1, heat[Symptom.HEADACHE]?.get(14))
    }

    @Test
    fun `entries outside any cycle are not counted in heatmap`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
        )
        val entries = listOf(
            entry("2025-12-01", Symptom.CRAMPS),  // before any cycle
            entry("2026-04-01", Symptom.CRAMPS),  // inside cycle
        )
        val stats = SymptomStats.compute(entries, cycles)
        // Both still count in frequency
        assertEquals(2, stats.frequency[Symptom.CRAMPS])
        // But only the one inside a cycle is in heatmap
        assertEquals(1, stats.cycleDayHeatmap[Symptom.CRAMPS]?.values?.sum())
    }
}
