package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.dao.CycleEntryDao
import com.hayate0726.tides.data.dao.SymptomEntryDao
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ImportPipelineTest {

    private val pipeline = ImportPipeline()

    private fun mockSource(name: String = "Test"): ImportSource = object : ImportSource {
        override val displayName = name
        override suspend fun parse(stream: java.io.InputStream) = error("not used")
    }

    private fun mockDb(cycleDates: List<LocalDate>, symptomDates: List<LocalDate>): TidesDatabase {
        val cycleDao = mockk<CycleEntryDao>()
        val symptomDao = mockk<SymptomEntryDao>()
        coEvery { cycleDao.allDates() } returns cycleDates
        coEvery { symptomDao.allDates() } returns symptomDates
        val db = mockk<TidesDatabase>()
        coEvery { db.cycleEntryDao() } returns cycleDao
        coEvery { db.symptomEntryDao() } returns symptomDao
        return db
    }

    @Test
    fun preview_counts_new_and_conflicting_dates() = runTest {
        val entries = listOf(
            ImportedEntry(LocalDate.parse("2026-01-01"), FlowIntensity.MEDIUM, null, emptyList(), null),
            ImportedEntry(LocalDate.parse("2026-01-02"), FlowIntensity.LIGHT, null, emptyList(), null),
            ImportedEntry(LocalDate.parse("2026-01-03"), null, null, listOf(Symptom.CRAMPS), null),
        )
        val parse = ParseResult(entries, emptyList(), emptyList())
        val db = mockDb(
            cycleDates = listOf(LocalDate.parse("2026-01-02")),
            symptomDates = emptyList(),
        )
        val preview = pipeline.computePreview(mockSource("X"), parse, db)
        assertEquals(3, preview.totalEntries)
        assertEquals(2, preview.newDates)
        assertEquals(1, preview.conflictingDates)
        assertEquals("X", preview.sourceName)
    }

    @Test
    fun preview_treats_symptom_only_existing_date_as_conflict() = runTest {
        val entries = listOf(
            ImportedEntry(LocalDate.parse("2026-01-01"), FlowIntensity.MEDIUM, null, emptyList(), null),
        )
        val parse = ParseResult(entries, emptyList(), emptyList())
        val db = mockDb(
            cycleDates = emptyList(),
            symptomDates = listOf(LocalDate.parse("2026-01-01")),
        )
        val preview = pipeline.computePreview(mockSource(), parse, db)
        assertEquals(0, preview.newDates)
        assertEquals(1, preview.conflictingDates)
    }

    @Test
    fun preview_passes_through_unmapped_and_warnings() = runTest {
        val parse = ParseResult(
            entries = emptyList(),
            unmapped = listOf(UnmappedField("temperature", 5)),
            warnings = listOf("Unparseable date: foo"),
        )
        val db = mockDb(emptyList(), emptyList())
        val preview = pipeline.computePreview(mockSource(), parse, db)
        assertEquals(1, preview.unmapped.size)
        assertEquals("temperature", preview.unmapped.first().sourceName)
        assertEquals(1, preview.warnings.size)
    }
}
