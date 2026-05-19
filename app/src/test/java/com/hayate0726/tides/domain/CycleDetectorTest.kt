package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CycleDetectorTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun entry(date: String, flow: FlowIntensity) =
        CycleDetector.Entry(d(date), flow)

    @Test
    fun `empty input produces empty list`() {
        val result = CycleDetector.detect(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single bleeding day produces one active cycle with periodEnd=that day`() {
        val entries = listOf(entry("2026-05-01", FlowIntensity.MEDIUM))
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        val c = result.single()
        assertEquals(d("2026-05-01"), c.start)
        assertEquals(d("2026-05-01"), c.periodEnd)
        assertNull(c.nextStart)
        assertTrue(c.isActive)
    }

    @Test
    fun `contiguous bleeding days form one period`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-02", FlowIntensity.MEDIUM),
            entry("2026-05-03", FlowIntensity.LIGHT),
            entry("2026-05-04", FlowIntensity.SPOTTING),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        assertEquals(d("2026-05-04"), result.single().periodEnd)
        assertEquals(4, result.single().periodLength)
    }

    @Test
    fun `gap of one day still counts as same period`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-02", FlowIntensity.LIGHT),
            // no entry on 5-3
            entry("2026-05-04", FlowIntensity.SPOTTING),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        assertEquals(d("2026-05-04"), result.single().periodEnd)
    }

    @Test
    fun `gap of two or more days starts a new cycle`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-02", FlowIntensity.LIGHT),
            // gap of 2 days (5-3 and 5-4 both empty)
            entry("2026-05-29", FlowIntensity.MEDIUM),
            entry("2026-05-30", FlowIntensity.LIGHT),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(2, result.size)
        assertEquals(d("2026-05-01"), result[0].start)
        assertEquals(d("2026-05-29"), result[0].nextStart)
        assertEquals(28, result[0].length)
        assertEquals(d("2026-05-29"), result[1].start)
        assertTrue(result[1].isActive)
    }

    @Test
    fun `NONE entries do not start or extend a period`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.NONE),
            entry("2026-05-02", FlowIntensity.MEDIUM),
            entry("2026-05-03", FlowIntensity.NONE),
            entry("2026-05-04", FlowIntensity.LIGHT),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        assertEquals(d("2026-05-02"), result.single().start)
        assertEquals(d("2026-05-04"), result.single().periodEnd)
    }

    @Test
    fun `spotting on hormonal bc does not seed cycle`() {
        // 5 spotting events 5 days apart — without filtering this would seed
        // phantom short cycles. On hormonal BC, spotting must be ignored.
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.SPOTTING),
            entry("2026-05-06", FlowIntensity.SPOTTING),
            entry("2026-05-11", FlowIntensity.SPOTTING),
            entry("2026-05-16", FlowIntensity.SPOTTING),
            entry("2026-05-21", FlowIntensity.SPOTTING),
        )
        val result = CycleDetector.detect(
            entries,
            activeBirthControl = BirthControlMethod.HORMONAL_IUD,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `spotting without bc still seeds cycle`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.SPOTTING),
            entry("2026-05-06", FlowIntensity.SPOTTING),
            entry("2026-05-11", FlowIntensity.SPOTTING),
            entry("2026-05-16", FlowIntensity.SPOTTING),
            entry("2026-05-21", FlowIntensity.SPOTTING),
        )
        val result = CycleDetector.detect(entries, activeBirthControl = null)
        assertFalse(result.isEmpty())
    }

    @Test
    fun `entries are sorted internally so order in does not matter`() {
        val a = CycleDetector.detect(listOf(
            entry("2026-05-04", FlowIntensity.LIGHT),
            entry("2026-05-01", FlowIntensity.MEDIUM),
        ))
        val b = CycleDetector.detect(listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-04", FlowIntensity.LIGHT),
        ))
        assertEquals(a.size, b.size)
        assertEquals(a[0].start, b[0].start)
        assertEquals(a[0].periodEnd, b[0].periodEnd)
    }
}
