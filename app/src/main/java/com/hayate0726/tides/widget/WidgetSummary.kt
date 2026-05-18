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
 * Format v1 (17 bytes):
 *   bytes 0..3   magic "TWGT"
 *   byte  4      version 0x01
 *   bytes 5..8   cycle day (int32, big-endian; 0 = unknown)
 *   bytes 9..16  last-update epoch millis (int64, big-endian)
 *
 * Format v2 (34 bytes):
 *   bytes 0..3   magic "TWGT"
 *   byte  4      version 0x02
 *   bytes 5..8   cycle day (int32, big-endian; 0 = unknown)
 *   bytes 9..16  last-update epoch millis (int64, big-endian)
 *   bytes 17..24 predicted period start epoch day (int64; Long.MIN_VALUE = absent)
 *   byte  25     showOvulation flag (0x01 = true, 0x00 = false)
 *   bytes 26..33 ovulation date epoch day (int64; Long.MIN_VALUE = absent)
 *
 * The file deliberately contains ONLY the data the user can already see on
 * the widget (spec §3, §5.13). It is unencrypted by necessity — the widget
 * runs in a system-process context without the user's PIN — but the leak
 * surface is small.
 *
 * The summary file is wiped along with the database on a duress-wipe; see
 * [com.hayate0726.tides.AppViewModel.handleDuress].
 */
object WidgetSummary {

    const val FILENAME = "widget_summary.bin"
    private val MAGIC = "TWGT".toByteArray(Charsets.US_ASCII)
    private const val V1: Byte = 0x01
    private const val V2: Byte = 0x02
    private const val V1_LEN = 17
    private const val V2_LEN = 34

    data class Snapshot(
        val cycleDay: Int,
        val updatedAtEpochMs: Long,
        val predictedPeriodStartEpochDay: Long? = null,
        val showOvulation: Boolean = false,
        val ovulationDateEpochDay: Long? = null,
    )

    fun file(ctx: Context): File = File(ctx.filesDir, FILENAME)

    fun computeCycleDay(today: LocalDate, cycles: List<Cycle>): Int {
        val active = cycles.filter { !it.start.isAfter(today) }.maxByOrNull { it.start }
            ?: return 0
        return ChronoUnit.DAYS.between(active.start, today).toInt() + 1
    }

    fun write(ctx: Context, snapshot: Snapshot) = writeTo(file(ctx), snapshot)
    fun read(ctx: Context): Snapshot? = readFrom(file(ctx))
    fun delete(ctx: Context) { file(ctx).delete() }

    fun writeTo(target: File, snapshot: Snapshot) {
        val buf = ByteBuffer.allocate(V2_LEN).order(ByteOrder.BIG_ENDIAN)
        buf.put(MAGIC)
        buf.put(V2)
        buf.putInt(snapshot.cycleDay)
        buf.putLong(snapshot.updatedAtEpochMs)
        buf.putLong(snapshot.predictedPeriodStartEpochDay ?: Long.MIN_VALUE)
        buf.put(if (snapshot.showOvulation) 0x01 else 0x00)
        buf.putLong(snapshot.ovulationDateEpochDay ?: Long.MIN_VALUE)
        target.writeBytes(buf.array())
    }

    fun readFrom(source: File): Snapshot? {
        if (!source.exists()) return null
        val bytes = source.readBytes()
        if (bytes.size < V1_LEN) return null
        if (!bytes.copyOfRange(0, 4).contentEquals(MAGIC)) return null
        return when (bytes[4]) {
            V1 -> {
                if (bytes.size != V1_LEN) return null
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                buf.position(5)
                Snapshot(cycleDay = buf.int, updatedAtEpochMs = buf.long)
            }
            V2 -> {
                if (bytes.size != V2_LEN) return null
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                buf.position(5)
                val day = buf.int
                val ts = buf.long
                val predStartRaw = buf.long
                val showOvuByte = buf.get()
                val ovuRaw = buf.long
                Snapshot(
                    cycleDay = day,
                    updatedAtEpochMs = ts,
                    predictedPeriodStartEpochDay = predStartRaw.takeIf { it != Long.MIN_VALUE },
                    showOvulation = showOvuByte == 0x01.toByte(),
                    ovulationDateEpochDay = ovuRaw.takeIf { it != Long.MIN_VALUE },
                )
            }
            else -> null
        }
    }
}
