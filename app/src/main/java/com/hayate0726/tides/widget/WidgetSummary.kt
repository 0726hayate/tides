package com.hayate0726.tides.widget

import android.content.Context
import com.hayate0726.tides.domain.model.Cycle
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The widget's data source. Stored at `filesDir/widget_summary.bin`, MODE_PRIVATE.
 *
 * Format (v1, fixed 17 bytes):
 *   bytes 0..3   magic "TWGT"
 *   byte  4      version 0x01
 *   bytes 5..8   cycle day (int32, big-endian; 0 = unknown)
 *   bytes 9..16  last-update epoch millis (int64, big-endian)
 *
 * The file deliberately contains ONLY the data the user can already see on
 * the widget (spec §3, §5.13). It is unencrypted by necessity — the widget
 * runs in a system-process context without the user's PIN — but the leak
 * surface is one small int.
 *
 * The summary file is wiped along with the database on a duress-wipe; see
 * [com.hayate0726.tides.AppViewModel.handleDuress].
 */
object WidgetSummary {

    const val FILENAME = "widget_summary.bin"
    private val MAGIC = "TWGT".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 0x01
    private const val LENGTH = 4 + 1 + 4 + 8

    data class Snapshot(val cycleDay: Int, val updatedAtEpochMs: Long)

    fun file(ctx: Context): File = File(ctx.filesDir, FILENAME)

    fun computeCycleDay(today: LocalDate, cycles: List<Cycle>): Int {
        // Most recent cycle whose start is on/before today wins. If today is
        // after that cycle's predicted end (next start - 1), we still report
        // the running count — predictions can be wrong and users find "day
        // 42" more honest than a sudden reset to "—".
        val active = cycles.filter { !it.start.isAfter(today) }.maxByOrNull { it.start }
            ?: return 0
        return ChronoUnit.DAYS.between(active.start, today).toInt() + 1
    }

    fun write(ctx: Context, snapshot: Snapshot) = writeTo(file(ctx), snapshot)
    fun read(ctx: Context): Snapshot? = readFrom(file(ctx))
    fun delete(ctx: Context) { file(ctx).delete() }

    fun writeTo(target: File, snapshot: Snapshot) {
        val buf = ByteBuffer.allocate(LENGTH).order(ByteOrder.BIG_ENDIAN)
        buf.put(MAGIC)
        buf.put(VERSION)
        buf.putInt(snapshot.cycleDay)
        buf.putLong(snapshot.updatedAtEpochMs)
        target.writeBytes(buf.array())
    }

    fun readFrom(source: File): Snapshot? {
        if (!source.exists() || source.length() != LENGTH.toLong()) return null
        val bytes = source.readBytes()
        if (!bytes.copyOfRange(0, 4).contentEquals(MAGIC)) return null
        if (bytes[4] != VERSION) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        buf.position(5)
        val day = buf.int
        val ts = buf.long
        return Snapshot(cycleDay = day, updatedAtEpochMs = ts)
    }
}
