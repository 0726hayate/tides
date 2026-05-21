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
    fun all_flow_values_are_within_FlowIntensity_enum() = runTest {
        val result = importer.parse(loadFixture("samsung_health_period.html"))
        for (entry in result.entries) {
            assertTrue(
                entry.flow == null || entry.flow in FlowIntensity.entries,
                "Unexpected flow: ${entry.flow}"
            )
        }
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
