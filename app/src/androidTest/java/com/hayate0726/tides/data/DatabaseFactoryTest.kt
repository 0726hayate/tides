package com.hayate0726.tides.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseFactoryTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "test_roundtrip.db")
        cleanupDbFiles()
    }

    @After
    fun tearDown() {
        cleanupDbFiles()
    }

    private fun cleanupDbFiles() {
        dbFile.delete()
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-wal").delete()
    }

    @Test
    fun create_and_reopen_with_same_key_succeeds(): Unit = runBlocking {
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.cycleEntryDao().upsert(
            CycleEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                flowIntensity = FlowIntensity.MEDIUM,
                painSeverity = 3,
                notes = "hello",
            )
        )
        db.close()
        key.zero()

        val key2 = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val db2 = DatabaseFactory.open(ctx, dbFile, key2)
        val rows = db2.cycleEntryDao().all()
        assertEquals(1, rows.size)
        assertEquals("hello", rows[0].notes)
        assertEquals(FlowIntensity.MEDIUM, rows[0].flowIntensity)
        db2.close()
        key2.zero()
    }

    @Test
    fun reopen_with_wrong_key_fails(): Unit = runBlocking {
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        DatabaseFactory.open(ctx, dbFile, key).also {
            it.cycleEntryDao().upsert(
                CycleEntryEntity(
                    date = LocalDate.parse("2026-05-01"),
                    flowIntensity = FlowIntensity.LIGHT,
                    painSeverity = null,
                    notes = "data",
                )
            )
            it.close()
        }
        key.zero()

        val wrongKey = KeyDerivation.deriveKey(Pin("999999".toCharArray()), ByteArray(16) { 1 })
        try {
            val db2 = DatabaseFactory.open(ctx, dbFile, wrongKey)
            db2.cycleEntryDao().all()
            db2.close()
            fail("expected SQLCipher to reject wrong key")
        } catch (e: Exception) {
            assertNotNull(e)
        } finally {
            wrongKey.zero()
        }
    }
}
