package com.hayate0726.tides.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CycleRepositoryTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File
    private lateinit var db: TidesDatabase
    private lateinit var repo: CycleRepository

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "repo_test.db")
        cleanupDbFiles()
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16))
        db = DatabaseFactory.open(ctx, dbFile, key)
        repo = CycleRepository(
            db.cycleEntryDao(),
            db.symptomEntryDao(),
            db.birthControlDao(),
            db.goalDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
        cleanupDbFiles()
    }

    private fun cleanupDbFiles() {
        dbFile.delete()
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-wal").delete()
    }

    @Test
    fun cycles_built_from_real_entries(): Unit = runBlocking {
        db.cycleEntryDao().upsert(CycleEntryEntity(LocalDate.parse("2026-05-01"), FlowIntensity.MEDIUM, null, null))
        db.cycleEntryDao().upsert(CycleEntryEntity(LocalDate.parse("2026-05-02"), FlowIntensity.LIGHT, null, null))
        db.cycleEntryDao().upsert(CycleEntryEntity(LocalDate.parse("2026-05-29"), FlowIntensity.MEDIUM, null, null))

        val cycles = repo.detectCycles(
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2027-01-01"),
        )
        assertEquals(2, cycles.size)
        assertEquals(LocalDate.parse("2026-05-01"), cycles[0].start)
        assertEquals(LocalDate.parse("2026-05-29"), cycles[1].start)
    }

    @Test
    fun symptom_entries_mapped_correctly(): Unit = runBlocking {
        db.symptomEntryDao().insert(
            SymptomEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                symptom = Symptom.CRAMPS,
                severity = 2,
                otherText = null,
            )
        )
        val entries = repo.symptomEntriesInRange(
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-31"),
        )
        assertEquals(1, entries.size)
        assertEquals(Symptom.CRAMPS, entries[0].symptom)
    }

    @Test
    fun other_text_preserved_only_for_OTHER_symptom(): Unit = runBlocking {
        db.symptomEntryDao().insert(
            SymptomEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                symptom = Symptom.OTHER,
                severity = 1,
                otherText = "weird tingling",
            )
        )
        val entries = repo.symptomEntriesInRange(
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-31"),
        )
        assertEquals("weird tingling", entries[0].otherText)
    }

    @Test
    fun active_bc_method_returned(): Unit = runBlocking {
        db.birthControlDao().insert(
            BirthControlEntity(
                method = BirthControlMethod.NONE,
                startDate = LocalDate.parse("2026-01-01"),
                endDate = null,
            )
        )
        val active = repo.activeBirthControl()
        assertTrue(active != null)
        assertEquals(BirthControlMethod.NONE, active!!.method)
    }
}
