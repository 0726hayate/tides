package com.hayate0726.tides.data.`import`

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PreImportSnapshotTest {

    private lateinit var dir: File
    private lateinit var liveDb: File
    private lateinit var snapshotFile: File
    private lateinit var snapshot: PreImportSnapshot

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        dir = File(ctx.cacheDir, "snapshot-test-${System.nanoTime()}").apply { mkdirs() }
        liveDb = File(dir, "live.db")
        snapshotFile = File(dir, "live.db.snapshot")
        snapshot = PreImportSnapshot(liveDb, snapshotFile)
    }

    @After
    fun tearDown() { dir.deleteRecursively() }

    @Test
    fun take_copies_live_db_to_snapshot() {
        liveDb.writeBytes(ByteArray(1024) { it.toByte() })
        snapshot.take()
        assertTrue(snapshotFile.exists())
        assertArrayEquals(liveDb.readBytes(), snapshotFile.readBytes())
    }

    @Test
    fun exists_returns_false_when_no_snapshot_taken() {
        liveDb.writeBytes(ByteArray(16))
        assertFalse(snapshot.exists())
    }

    @Test
    fun exists_returns_true_after_take() {
        liveDb.writeBytes(ByteArray(16))
        snapshot.take()
        assertTrue(snapshot.exists())
    }

    @Test
    fun restore_replaces_live_with_snapshot_then_deletes_snapshot() {
        val original = ByteArray(2048) { (it * 7).toByte() }
        liveDb.writeBytes(original)
        snapshot.take()
        liveDb.writeBytes(ByteArray(2048) { 0xAA.toByte() })

        val ok = snapshot.restore()
        assertTrue(ok)
        assertArrayEquals(original, liveDb.readBytes())
        assertFalse(snapshotFile.exists())
    }

    @Test
    fun take_overwrites_prior_snapshot() {
        liveDb.writeBytes(ByteArray(512) { 0x11 })
        snapshot.take()
        val older = snapshotFile.readBytes()
        liveDb.writeBytes(ByteArray(512) { 0x22 })
        snapshot.take()
        val newer = snapshotFile.readBytes()
        assertFalse(older.contentEquals(newer))
    }

    @Test
    fun restore_returns_false_when_no_snapshot_present() {
        assertFalse(snapshot.restore())
    }
}
