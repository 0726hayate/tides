package com.hayate0726.tides.widget

import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class WidgetSummaryTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun round_trip_preserves_cycle_day_and_timestamp() {
        val f = tmp.newFile("widget_summary.bin")
        val snap = WidgetSummary.Snapshot(cycleDay = 17, updatedAtEpochMs = 1_730_000_000_000L)
        WidgetSummary.writeTo(f, snap)
        assertEquals(snap, WidgetSummary.readFrom(f))
    }

    @Test
    fun read_returns_null_for_missing_file() {
        val f = tmp.newFile("absent.bin").also { it.delete() }
        assertNull(WidgetSummary.readFrom(f))
    }

    @Test
    fun read_returns_null_for_wrong_magic() {
        val f = tmp.newFile("junk.bin")
        f.writeBytes(ByteArray(17) { 0x42 })
        assertNull(WidgetSummary.readFrom(f))
    }

    @Test
    fun read_returns_null_for_truncated_file() {
        val f = tmp.newFile("short.bin")
        f.writeBytes("TWGT".toByteArray() + byteArrayOf(0x01) + byteArrayOf(0, 0))
        assertNull(WidgetSummary.readFrom(f))
    }

    @Test
    fun cycle_day_is_one_at_period_start() {
        val cycles = detectFrom(listOf("2026-05-01"))
        val day = WidgetSummary.computeCycleDay(LocalDate.parse("2026-05-01"), cycles)
        assertEquals(1, day)
    }

    @Test
    fun cycle_day_counts_from_most_recent_start() {
        val cycles = detectFrom(listOf("2026-04-01", "2026-04-29"))
        val day = WidgetSummary.computeCycleDay(LocalDate.parse("2026-05-10"), cycles)
        assertEquals(12, day) // 2026-04-29 .. 2026-05-10 inclusive == 12 days
    }

    @Test
    fun cycle_day_is_zero_when_no_cycles_logged() {
        assertEquals(0, WidgetSummary.computeCycleDay(LocalDate.parse("2026-05-10"), emptyList()))
    }

    @Test
    fun cycle_day_is_zero_when_today_predates_first_cycle() {
        val cycles = detectFrom(listOf("2026-06-01"))
        assertEquals(0, WidgetSummary.computeCycleDay(LocalDate.parse("2026-05-10"), cycles))
    }

    @Test
    fun v2_round_trip_preserves_all_fields() {
        val f = tmp.newFile("v2.bin")
        val snap = WidgetSummary.Snapshot(
            cycleDay = 12,
            updatedAtEpochMs = 1_730_000_000_000L,
            predictedPeriodStartEpochDay = 20_241L,
            showOvulation = true,
            ovulationDateEpochDay = 20_230L,
        )
        WidgetSummary.writeTo(f, snap)
        assertEquals(snap, WidgetSummary.readFrom(f))
    }

    @Test
    fun v1_blob_still_readable_with_nulled_v2_fields() {
        // Synthesize a v1 blob by hand (17 bytes).
        val f = tmp.newFile("v1.bin")
        val buf = java.nio.ByteBuffer.allocate(17).order(java.nio.ByteOrder.BIG_ENDIAN)
        buf.put("TWGT".toByteArray())
        buf.put(0x01.toByte())
        buf.putInt(9)
        buf.putLong(1_730_000_000_000L)
        f.writeBytes(buf.array())

        val snap = WidgetSummary.readFrom(f)!!
        assertEquals(9, snap.cycleDay)
        assertEquals(1_730_000_000_000L, snap.updatedAtEpochMs)
        assertEquals(null, snap.predictedPeriodStartEpochDay)
        assertEquals(false, snap.showOvulation)
        assertEquals(null, snap.ovulationDateEpochDay)
    }

    private fun detectFrom(periodStartDates: List<String>): List<Cycle> {
        val entries = periodStartDates.map {
            CycleDetector.Entry(LocalDate.parse(it), FlowIntensity.MEDIUM)
        }
        return CycleDetector.detect(entries)
    }
}
