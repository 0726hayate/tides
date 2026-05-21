package com.hayate0726.tides.data.`import`

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportFormatDetectorTest {

    @Test
    fun detects_samsung_html_by_doctype_and_marker() {
        val bytes = "<!DOCTYPE html><html><head><title>Samsung Health</title></head><body></body></html>".toByteArray()
        assertEquals(ImportFormatDetector.Format.SAMSUNG_HTML, ImportFormatDetector.detect(bytes))
    }

    @Test
    fun detects_drip_json_by_root_key() {
        val bytes = """{"cycles":[],"version":1}""".toByteArray()
        assertEquals(ImportFormatDetector.Format.DRIP_JSON, ImportFormatDetector.detect(bytes))
    }

    @Test
    fun detects_clue_csv_by_header_columns() {
        val bytes = "day,period,flow,cramps,headache,note\n2026-01-01,true,medium,,,\n".toByteArray()
        assertEquals(ImportFormatDetector.Format.CLUE_CSV, ImportFormatDetector.detect(bytes))
    }

    @Test
    fun detects_pdf_by_magic_bytes() {
        val bytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34)
        assertEquals(ImportFormatDetector.Format.PDF, ImportFormatDetector.detect(bytes))
    }

    @Test
    fun returns_unknown_for_random_bytes() {
        val bytes = "this is not a recognized export".toByteArray()
        assertEquals(ImportFormatDetector.Format.UNKNOWN, ImportFormatDetector.detect(bytes))
    }

    @Test
    fun returns_unknown_for_empty_input() {
        assertEquals(ImportFormatDetector.Format.UNKNOWN, ImportFormatDetector.detect(ByteArray(0)))
    }

    @Test
    fun json_with_commas_in_values_is_not_misdetected_as_clue_csv() {
        // Regression guard: JSON check must run before CSV check so a JSON
        // document whose first line contains 'date'/'period'/'flow' as keys
        // (with commas separating fields) isn't classified as CSV.
        val bytes = """{"date":"2026-01-01","period":true,"flow":"medium","cycles":[]}""".toByteArray()
        assertEquals(ImportFormatDetector.Format.DRIP_JSON, ImportFormatDetector.detect(bytes))
    }
}
