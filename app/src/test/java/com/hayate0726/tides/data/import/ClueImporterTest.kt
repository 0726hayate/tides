package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ClueImporterTest {

    private val importer = ClueImporter()

    private fun loadFixture(name: String) =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("import-fixtures/$name")) {
            "Fixture not found: $name"
        }

    @Test
    fun displayName_is_clue() {
        assertEquals("Clue", importer.displayName)
    }

    @Test
    fun parses_canonical_fixture_into_five_entries() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        assertEquals(5, result.entries.size)
    }

    @Test
    fun maps_flow_words_to_FlowIntensity() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        val byDate = result.entries.associateBy { it.date }
        assertEquals(FlowIntensity.MEDIUM, byDate[LocalDate.parse("2026-04-01")]!!.flow)
        assertEquals(FlowIntensity.LIGHT, byDate[LocalDate.parse("2026-04-02")]!!.flow)
        assertEquals(FlowIntensity.SPOTTING, byDate[LocalDate.parse("2026-04-03")]!!.flow)
        assertEquals(FlowIntensity.HEAVY, byDate[LocalDate.parse("2026-04-29")]!!.flow)
    }

    @Test
    fun row_without_flow_has_null_flow() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        val byDate = result.entries.associateBy { it.date }
        assertNull(byDate[LocalDate.parse("2026-04-15")]!!.flow)
    }

    @Test
    fun maps_symptom_columns_to_Symptom_enum() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        val day1 = result.entries.first { it.date == LocalDate.parse("2026-04-01") }
        assertTrue(day1.symptoms.contains(Symptom.CRAMPS))
        assertTrue(day1.symptoms.contains(Symptom.BREAST_TENDERNESS))
        assertTrue(day1.symptoms.contains(Symptom.CRAVINGS))
    }

    @Test
    fun maps_sensitive_and_happy_and_sad() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        val byDate = result.entries.associateBy { it.date }
        val day15 = byDate[LocalDate.parse("2026-04-15")]!!
        assertTrue(day15.symptoms.contains(Symptom.BLOATING))
        assertTrue(day15.symptoms.contains(Symptom.HAPPY))
        val day29 = byDate[LocalDate.parse("2026-04-29")]!!
        assertTrue(day29.symptoms.contains(Symptom.SAD))
    }

    @Test
    fun preserves_note_verbatim() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        val day1 = result.entries.first { it.date == LocalDate.parse("2026-04-01") }
        assertEquals("Heaviest morning", day1.notes)
    }

    @Test
    fun counts_exercise_and_sleep_as_unmapped() = runTest {
        val result = importer.parse(loadFixture("clue_export.csv"))
        val exercise = result.unmapped.firstOrNull { it.sourceName == "exercise" }
        assertNotNull(exercise)
        assertTrue(exercise!!.sampleCount >= 2)
        assertNotNull(result.unmapped.firstOrNull { it.sourceName == "sleep" })
    }

    @Test
    fun handles_quoted_note_with_embedded_comma() = runTest {
        val csv = """
            day,period,flow,cramps,note
            2026-05-01,true,medium,true,"Painful, woke up early"
        """.trimIndent()
        val result = importer.parse(csv.byteInputStream())
        assertEquals("Painful, woke up early", result.entries.first().notes)
    }

    @Test
    fun emits_warning_for_unparseable_date_and_continues() = runTest {
        val csv = """
            day,period,flow
            not-a-date,true,medium
            2026-05-02,true,light
        """.trimIndent()
        val result = importer.parse(csv.byteInputStream())
        assertEquals(1, result.entries.size)
        assertTrue(result.warnings.any { it.contains("not-a-date") })
    }

    @Test
    fun rejects_csv_without_day_column() = runTest {
        val csv = """
            foo,bar
            1,2
        """.trimIndent()
        val ex = org.junit.jupiter.api.Assertions.assertThrows(ClueImporter.MalformedCsv::class.java) {
            kotlinx.coroutines.runBlocking { importer.parse(csv.byteInputStream()) }
        }
        assertTrue(ex.message!!.contains("day", ignoreCase = true))
    }

    @Test
    fun skips_dates_before_1900_and_future() = runTest {
        val future = LocalDate.now().plusDays(7)
        val csv = """
            day,period,flow
            1899-12-31,true,medium
            1900-01-01,true,medium
            $future,true,light
        """.trimIndent()
        val result = importer.parse(csv.byteInputStream())
        assertEquals(1, result.entries.size)
        assertEquals(LocalDate.parse("1900-01-01"), result.entries.first().date)
        assertTrue(result.warnings.any { it.contains("1899") })
        assertTrue(result.warnings.any { it.contains(future.toString()) })
    }
}
