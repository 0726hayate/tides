package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SamsungHealthHtmlImporterTest {

    private val importer = SamsungHealthHtmlImporter()

    private fun loadFixture(name: String) =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("import-fixtures/$name")) {
            "Fixture not found: $name"
        }

    @Test
    fun displayName_is_samsung_health() {
        assertEquals("Samsung Health", importer.displayName)
    }

    @Test
    fun parses_real_samsung_export_into_nonempty_entries() = runTest {
        val result = importer.parse(loadFixture("samsung_health_period.html"))
        assertTrue(result.entries.isNotEmpty(), "Expected ≥1 entry, got 0")
    }

    @Test
    fun extracts_dates_in_ascending_order() = runTest {
        val result = importer.parse(loadFixture("samsung_health_period.html"))
        val dates = result.entries.map { it.date }
        assertEquals(dates.sorted(), dates, "Entries should be returned in ascending date order")
    }

    @Test
    fun all_entries_have_flow_LIGHT() = runTest {
        // Samsung's "Share data" HTML doesn't carry per-day intensity, so the
        // parser emits LIGHT as a "period present" baseline. This pins that
        // contract so a future change must update both code and test.
        val result = importer.parse(loadFixture("samsung_health_period.html"))
        for (entry in result.entries) {
            assertEquals(
                FlowIntensity.LIGHT,
                entry.flow,
                "Samsung parser should emit LIGHT for every entry, got ${entry.flow} on ${entry.date}",
            )
        }
    }

    @Test
    fun warns_when_no_period_column_found_in_tables() = runTest {
        // Simulates a non-English Samsung export: tables present but none has
        // a "Period" header. We surface a warning so the user gets a clue.
        val html = """
            <html><body>
              <table>
                <tr><td>Cycle length</td><td>Bleeding period</td></tr>
                <tr><td>28 days</td><td>Apr 1–5</td></tr>
              </table>
            </body></html>
        """.trimIndent()
        val result = importer.parse(html.byteInputStream())
        assertTrue(result.entries.isEmpty())
        assertTrue(result.warnings.any { it.contains("Period", ignoreCase = true) })
    }

    @Test
    fun returns_empty_when_html_has_no_period_section() = runTest {
        val html = "<html><body><h1>Steps report</h1><p>No period data</p></body></html>"
        val result = importer.parse(html.byteInputStream())
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun does_not_throw_on_truncated_html() = runTest {
        val html = "<html><body><h2>Period</h2><table><tr><td>2026"
        val result = importer.parse(html.byteInputStream())
        assertNotNull(result)
    }
}
