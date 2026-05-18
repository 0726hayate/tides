package com.hayate0726.tides.ui.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.model.Cycle
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PdfBuilderTest {

    @Test
    fun cycle_summary_pdf_has_non_zero_bytes() {
        val cycles = listOf(
            Cycle(
                start = LocalDate.parse("2026-01-01"),
                periodEnd = LocalDate.parse("2026-01-05"),
                nextStart = LocalDate.parse("2026-01-29"),
            )
        )
        val stats = CycleStats.compute(cycles)
        val out = ByteArrayOutputStream()
        PdfBuilder.buildCycleSummary(
            cycles = cycles,
            stats = stats,
            symptomTopN = emptyList(),
            appVersion = "0.1.0",
            output = out,
        )
        assertTrue(out.size() > 0)
        val header = out.toByteArray().take(4).joinToString("") { "%02x".format(it) }
        assertTrue(
            "expected PDF header (25 50 44 46 = '%PDF'), got $header",
            header == "25504446",
        )
    }
}
