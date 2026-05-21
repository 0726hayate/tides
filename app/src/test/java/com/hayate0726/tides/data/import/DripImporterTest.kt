package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DripImporterTest {

    private val importer = DripImporter()

    private fun loadFixture(name: String) =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("import-fixtures/$name")) {
            "Fixture not found: $name"
        }

    @Test
    fun displayName_is_drip() {
        assertEquals("drip", importer.displayName)
    }

    @Test
    fun parses_canonical_export() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        assertEquals(4, result.entries.size)
        assertEquals(LocalDate.parse("2026-04-01"), result.entries[0].date)
    }

    @Test
    fun maps_bleeding_value_to_flow_intensity() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val byDate = result.entries.associateBy { it.date }
        assertEquals(FlowIntensity.LIGHT, byDate[LocalDate.parse("2026-04-01")]!!.flow)
        assertEquals(FlowIntensity.SPOTTING, byDate[LocalDate.parse("2026-04-02")]!!.flow)
        assertNull(byDate[LocalDate.parse("2026-04-03")]!!.flow)
        assertEquals(FlowIntensity.HEAVY, byDate[LocalDate.parse("2026-04-29")]!!.flow)
    }

    @Test
    fun maps_pain_symptoms_to_Symptom_enum() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val day1 = result.entries.first { it.date == LocalDate.parse("2026-04-01") }
        assertTrue(day1.symptoms.contains(Symptom.CRAMPS))
        assertTrue(day1.symptoms.contains(Symptom.HEADACHE))
    }

    @Test
    fun maps_mood_sad_to_Symptom_SAD() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val day1 = result.entries.first { it.date == LocalDate.parse("2026-04-01") }
        assertTrue(day1.symptoms.contains(Symptom.SAD))
    }

    @Test
    fun preserves_note_value_verbatim() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val day1 = result.entries.first { it.date == LocalDate.parse("2026-04-01") }
        assertEquals("First day, rough morning", day1.notes)
    }

    @Test
    fun counts_temperature_as_unmapped() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val temp = result.unmapped.firstOrNull { it.sourceName == "temperature" }
        assertNotNull(temp)
        assertEquals(1, temp!!.sampleCount)
    }

    @Test
    fun counts_mucus_as_unmapped() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val mucus = result.unmapped.firstOrNull { it.sourceName == "mucus" }
        assertNotNull(mucus)
    }

    @Test
    fun counts_sex_as_unmapped() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        assertNotNull(result.unmapped.firstOrNull { it.sourceName == "sex" })
    }

    @Test
    fun maps_mood_happy_to_Symptom_HAPPY() = runTest {
        val result = importer.parse(loadFixture("drip_export.json"))
        val day4 = result.entries.first { it.date == LocalDate.parse("2026-04-29") }
        assertTrue(day4.symptoms.contains(Symptom.HAPPY))
    }

    @Test
    fun rejects_encrypted_backup_with_specific_error() {
        val ex = assertThrows(DripImporter.EncryptedBackup::class.java) {
            kotlinx.coroutines.runBlocking {
                importer.parse(loadFixture("drip_encrypted.json"))
            }
        }
        assertTrue(ex.message!!.contains("encrypted", ignoreCase = true))
    }

    @Test
    fun parses_minimal_fixture() = runTest {
        val result = importer.parse(loadFixture("drip_minimal.json"))
        assertEquals(2, result.entries.size)
        assertTrue(result.unmapped.isEmpty())
    }

    @Test
    fun skips_excluded_pain_entries() = runTest {
        val json = """
            {
              "cycles": [
                { "date": "2026-05-01",
                  "pain": { "cramps": { "value": 2, "exclude": true } }
                }
              ]
            }
        """.trimIndent()
        val result = importer.parse(json.byteInputStream())
        assertTrue(result.entries.first().symptoms.isEmpty())
    }

    @Test
    fun skips_dates_before_1900() = runTest {
        val json = """
            { "cycles": [
                { "date": "1899-12-31", "bleeding": { "value": 2, "exclude": false } },
                { "date": "1900-01-01", "bleeding": { "value": 2, "exclude": false } }
            ] }
        """.trimIndent()
        val result = importer.parse(json.byteInputStream())
        assertEquals(1, result.entries.size)
        assertEquals(LocalDate.parse("1900-01-01"), result.entries.first().date)
        assertTrue(result.warnings.any { it.contains("1899") })
    }

    @Test
    fun skips_future_dates() = runTest {
        val future = LocalDate.now().plusDays(7)
        val json = """
            { "cycles": [
                { "date": "$future", "bleeding": { "value": 2, "exclude": false } }
            ] }
        """.trimIndent()
        val result = importer.parse(json.byteInputStream())
        assertTrue(result.entries.isEmpty())
        assertTrue(result.warnings.any { it.contains(future.toString()) })
    }
}
