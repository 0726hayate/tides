package com.hayate0726.tides.data.`import`

import androidx.room.withTransaction
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.dao.CycleEntryDao
import com.hayate0726.tides.data.dao.SymptomEntryDao
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
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

    // ---- commit() tests ----
    //
    // Tides uses Room/SQLCipher and there's no straightforward in-memory mode
    // for SQLCipher in JVM unit tests, so we mock the DAOs and stub the
    // `withTransaction` extension to pass-through. Transaction semantics
    // themselves are guaranteed by Room; we verify our iteration + skip logic.

    private fun mockCommitDb(
        cycleDates: List<LocalDate>,
        symptomDates: List<LocalDate>,
    ): Triple<TidesDatabase, CycleEntryDao, SymptomEntryDao> {
        val cycleDao = mockk<CycleEntryDao>(relaxed = true)
        val symptomDao = mockk<SymptomEntryDao>(relaxed = true)
        coEvery { cycleDao.allDates() } returns cycleDates
        coEvery { symptomDao.allDates() } returns symptomDates
        val db = mockk<TidesDatabase>(relaxed = true)
        coEvery { db.cycleEntryDao() } returns cycleDao
        coEvery { db.symptomEntryDao() } returns symptomDao
        return Triple(db, cycleDao, symptomDao)
    }

    @BeforeEach
    fun stubRoomTransaction() {
        // Make `db.withTransaction { block }` simply invoke `block()`.
        // Verified FQN for Room 2.6.1; re-verify if bumping room-ktx.
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery {
            any<TidesDatabase>().withTransaction(any<suspend () -> Any>())
        } coAnswers {
            @Suppress("UNCHECKED_CAST")
            (secondArg<suspend () -> Any>()).invoke()
        }
    }

    @AfterEach
    fun unstubRoomTransaction() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun commit_skips_dates_already_in_cycle_entries() = runTest {
        val (db, cycleDao, symptomDao) = mockCommitDb(
            cycleDates = listOf(LocalDate.parse("2026-01-02")),
            symptomDates = emptyList(),
        )
        val parse = ParseResult(
            entries = listOf(
                ImportedEntry(LocalDate.parse("2026-01-01"), FlowIntensity.LIGHT, null, emptyList(), null),
                ImportedEntry(LocalDate.parse("2026-01-02"), FlowIntensity.MEDIUM, null, emptyList(), null),
            ),
            unmapped = emptyList(),
            warnings = emptyList(),
        )
        val result = pipeline.commit(parse, db)
        assertEquals(1, result.addedDates)
        assertEquals(1, result.skippedDates)
        // Only the non-conflicting row should have been written.
        coVerify(exactly = 1) { cycleDao.upsert(any()) }
        coVerify(exactly = 0) { symptomDao.insert(any()) }
    }

    @Test
    fun commit_skips_dates_already_in_symptom_entries() = runTest {
        val (db, cycleDao, symptomDao) = mockCommitDb(
            cycleDates = emptyList(),
            symptomDates = listOf(LocalDate.parse("2026-02-10")),
        )
        val parse = ParseResult(
            entries = listOf(
                ImportedEntry(LocalDate.parse("2026-02-10"), FlowIntensity.LIGHT, null, listOf(Symptom.CRAMPS), null),
            ),
            unmapped = emptyList(),
            warnings = emptyList(),
        )
        val result = pipeline.commit(parse, db)
        assertEquals(0, result.addedDates)
        assertEquals(1, result.skippedDates)
        coVerify(exactly = 0) { cycleDao.upsert(any()) }
        coVerify(exactly = 0) { symptomDao.insert(any()) }
    }

    @Test
    fun commit_writes_symptoms_even_when_no_flow() = runTest {
        val (db, cycleDao, symptomDao) = mockCommitDb(emptyList(), emptyList())
        val parse = ParseResult(
            entries = listOf(
                ImportedEntry(
                    date = LocalDate.parse("2026-03-15"),
                    flow = null, painSeverity = null,
                    symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE),
                    notes = null,
                ),
            ),
            unmapped = emptyList(),
            warnings = emptyList(),
        )
        val result = pipeline.commit(parse, db)
        assertEquals(1, result.addedDates)
        // No CycleEntry because flow/pain/notes all null/empty.
        coVerify(exactly = 0) { cycleDao.upsert(any()) }
        // Two SymptomEntry rows.
        coVerify(exactly = 2) { symptomDao.insert(any()) }
    }

    @Test
    fun commit_uses_room_transaction() = runTest {
        val (db, _, _) = mockCommitDb(emptyList(), emptyList())
        val parse = ParseResult(
            entries = listOf(
                ImportedEntry(LocalDate.parse("2026-04-01"), FlowIntensity.LIGHT, null, emptyList(), null),
            ),
            unmapped = emptyList(),
            warnings = emptyList(),
        )
        pipeline.commit(parse, db)
        coVerify(exactly = 1) { db.withTransaction(any<suspend () -> Any>()) }
    }

    @Test
    fun commit_writes_cycle_with_NONE_flow_when_only_pain_or_notes_present() = runTest {
        val (db, cycleDao, _) = mockCommitDb(emptyList(), emptyList())
        val capturedEntity = slot<CycleEntryEntity>()
        coEvery { cycleDao.upsert(capture(capturedEntity)) } just Runs
        val parse = ParseResult(
            entries = listOf(
                ImportedEntry(
                    date = LocalDate.parse("2026-05-01"),
                    flow = null, painSeverity = 5,
                    symptoms = emptyList(),
                    notes = "Heavy day",
                ),
            ),
            unmapped = emptyList(),
            warnings = emptyList(),
        )
        pipeline.commit(parse, db)
        assertEquals(FlowIntensity.NONE, capturedEntity.captured.flowIntensity)
        assertEquals(5, capturedEntity.captured.painSeverity)
        assertEquals("Heavy day", capturedEntity.captured.notes)
    }
}
