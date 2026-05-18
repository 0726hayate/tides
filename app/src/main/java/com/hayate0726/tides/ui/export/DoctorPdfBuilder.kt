package com.hayate0726.tides.ui.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.FigoAnalysis
import com.hayate0726.tides.domain.model.Cycle
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DoctorPdfBuilder {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 48
    private const val LINE_HEIGHT = 16

    /** Public so tests can verify phrasing without rendering. */
    fun descriptiveText(p: FigoAnalysis.Pattern): String = when (p) {
        FigoAnalysis.Pattern.CYCLE_FREQUENT ->
            "Cycles outside the typical 24–38 day range (FIGO: frequent)"
        FigoAnalysis.Pattern.CYCLE_INFREQUENT ->
            "Cycles outside the typical 24–38 day range (FIGO: infrequent)"
        FigoAnalysis.Pattern.CYCLE_IRREGULAR ->
            "Cycle variation greater than 7 days (FIGO: irregular)"
        FigoAnalysis.Pattern.PERIOD_PROLONGED ->
            "Period duration exceeded 8 days in one or more cycles (FIGO: prolonged)"
        FigoAnalysis.Pattern.HEAVY_FLOW ->
            "Heavy flow logged in two or more cycles (FIGO: heavy menstrual bleeding self-report)"
        FigoAnalysis.Pattern.INTERMENSTRUAL_BLEEDING ->
            "Intermenstrual bleeding logged"
        FigoAnalysis.Pattern.SEVERE_DYSMENORRHEA ->
            "Severe dysmenorrhea (NRS ≥ 7/10) logged"
        FigoAnalysis.Pattern.AMENORRHEA ->
            "No period logged for 90 or more days"
    }

    fun build(
        cycles: List<Cycle>,
        stats: CycleStats,
        figoPatterns: Set<FigoAnalysis.Pattern>,
        userName: String?,
        userDob: LocalDate?,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        appVersion: String,
        output: OutputStream,
    ) {
        val doc = PdfDocument()

        // Page 1 — summary
        val page1 = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        renderSummaryPage(
            page1.canvas, cycles, stats, figoPatterns, userName, userDob,
            rangeStart, rangeEnd, appVersion,
        )
        doc.finishPage(page1)

        // Page 2 — cycle log table
        val page2 = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
        renderCycleTablePage(page2.canvas, cycles)
        doc.finishPage(page2)

        doc.writeTo(output)
        doc.close()
    }

    private fun renderSummaryPage(
        canvas: android.graphics.Canvas,
        cycles: List<Cycle>,
        stats: CycleStats,
        figoPatterns: Set<FigoAnalysis.Pattern>,
        userName: String?,
        userDob: LocalDate?,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        appVersion: String,
    ) {
        val title = Paint().apply {
            color = Color.BLACK; textSize = 20f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val label = Paint().apply {
            color = Color.BLACK; textSize = 9f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val body = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        // Spec §5.4: black ink on white only. DKGRAY would be visually softer
        // but is not allowed by the clinical-print spec.
        val small = Paint().apply { color = Color.BLACK; textSize = 8f; isAntiAlias = true }

        var y = MARGIN + 22
        canvas.drawText("Menstrual Tracking Summary", MARGIN.toFloat(), y.toFloat(), title)
        y += 20
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        if (userName != null) {
            canvas.drawText("Name: $userName", MARGIN.toFloat(), y.toFloat(), body); y += 14
        }
        if (userDob != null) {
            canvas.drawText("DOB: ${userDob.format(fmt)}", MARGIN.toFloat(), y.toFloat(), body); y += 14
        }
        canvas.drawText(
            "Range: ${rangeStart.format(fmt)} to ${rangeEnd.format(fmt)}",
            MARGIN.toFloat(), y.toFloat(), body,
        ); y += 14
        canvas.drawText(
            "Generated: ${LocalDate.now().format(fmt)}",
            MARGIN.toFloat(), y.toFloat(), body,
        ); y += 24

        val lmp = cycles.maxByOrNull { it.start }?.start
        if (lmp != null) {
            canvas.drawText("LMP", MARGIN.toFloat(), y.toFloat(), label); y += LINE_HEIGHT
            canvas.drawText(
                lmp.format(fmt), MARGIN.toFloat(), y.toFloat(),
                Paint(title).apply { textSize = 14f },
            ); y += 20
        }

        canvas.drawText("CYCLE STATISTICS", MARGIN.toFloat(), y.toFloat(), label); y += LINE_HEIGHT
        canvas.drawText(
            "Median length: ${stats.medianCycleLength ?: "—"} days   " +
                "Range: ${stats.cycleLengthMin ?: "—"}–${stats.cycleLengthMax ?: "—"} days   " +
                "Variation: ${stats.cycleLengthRange ?: "—"} days",
            MARGIN.toFloat(), y.toFloat(), body,
        ); y += LINE_HEIGHT
        canvas.drawText(
            "Cycles tracked: ${stats.completedCycleCount}",
            MARGIN.toFloat(), y.toFloat(), body,
        ); y += 20

        canvas.drawText("PERIOD STATISTICS", MARGIN.toFloat(), y.toFloat(), label); y += LINE_HEIGHT
        canvas.drawText(
            "Median duration: ${stats.medianPeriodLength ?: "—"} days",
            MARGIN.toFloat(), y.toFloat(), body,
        ); y += 20

        if (figoPatterns.isNotEmpty()) {
            canvas.drawText("PATTERNS OF NOTE", MARGIN.toFloat(), y.toFloat(), label); y += LINE_HEIGHT
            for (p in figoPatterns) {
                canvas.drawText("• ${descriptiveText(p)}", MARGIN.toFloat(), y.toFloat(), body)
                y += LINE_HEIGHT
            }
        }

        // Footer disclaimer
        canvas.drawText(
            "Patient-recorded data. Not a medical record. Not medical advice.",
            MARGIN.toFloat(), (PAGE_HEIGHT - MARGIN).toFloat(), small,
        )
        canvas.drawText(
            "Generated by Tides v$appVersion",
            MARGIN.toFloat(), (PAGE_HEIGHT - MARGIN + 12).toFloat(), small,
        )
    }

    private fun renderCycleTablePage(canvas: android.graphics.Canvas, cycles: List<Cycle>) {
        val label = Paint().apply {
            color = Color.BLACK; textSize = 9f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val body = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        val mono = Paint(body).apply { typeface = Typeface.MONOSPACE }

        var y = MARGIN + 22
        canvas.drawText("CYCLE LOG", MARGIN.toFloat(), y.toFloat(), label); y += LINE_HEIGHT
        canvas.drawText(
            "Start         Length  Period  Period end",
            MARGIN.toFloat(), y.toFloat(), label,
        ); y += LINE_HEIGHT
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        for (c in cycles) {
            val line = "%s   %4s    %4s    %s".format(
                c.start.format(fmt),
                c.length?.toString() ?: "—",
                c.periodLength?.toString() ?: "—",
                c.periodEnd?.format(fmt) ?: "—",
            )
            canvas.drawText(line, MARGIN.toFloat(), y.toFloat(), mono)
            y += LINE_HEIGHT
            if (y > PAGE_HEIGHT - MARGIN - 20) break
        }
    }
}
