package com.hayate0726.tides.ui.export

import com.hayate0726.tides.domain.model.Cycle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CsvBuilderTest {

    @Test
    fun csv_has_header_row_and_one_data_row_per_cycle() {
        val cycles = listOf(
            Cycle(
                start = LocalDate.parse("2026-01-01"),
                periodEnd = LocalDate.parse("2026-01-05"),
                nextStart = LocalDate.parse("2026-01-29"),
            ),
            Cycle(
                start = LocalDate.parse("2026-01-29"),
                periodEnd = LocalDate.parse("2026-02-02"),
                nextStart = null,
            ),
        )
        val csv = CsvBuilder.build(cycles, emptyMap(), emptyMap())
        val lines = csv.split("\n")
        assertEquals(3, lines.filter { it.isNotBlank() }.size)
        assertTrue(lines[0].startsWith("cycle_start,"))
    }

    @Test
    fun csv_escapes_commas_in_notes() {
        val cycles = listOf(
            Cycle(
                start = LocalDate.parse("2026-01-01"),
                periodEnd = LocalDate.parse("2026-01-05"),
                nextStart = null,
            )
        )
        val notes = mapOf(LocalDate.parse("2026-01-01") to "had a, weird day")
        val csv = CsvBuilder.build(cycles, emptyMap(), notes)
        assertTrue(csv.contains("\"had a, weird day\""))
    }
}
