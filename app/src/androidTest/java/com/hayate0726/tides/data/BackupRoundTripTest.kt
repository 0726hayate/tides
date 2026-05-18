package com.hayate0726.tides.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    @Test
    fun export_and_import_round_trip_with_correct_password() = runBlocking {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val srcFile = File(ctx.filesDir, "src.db")
        val bakFile = File(ctx.filesDir, "backup.tides")
        val dstFile = File(ctx.filesDir, "dst.db")
        listOf(srcFile, bakFile, dstFile).forEach { it.delete() }

        val srcKey = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val src = DatabaseFactory.open(ctx, srcFile, srcKey)
        src.cycleEntryDao().upsert(
            CycleEntryEntity(LocalDate.parse("2026-05-01"), FlowIntensity.MEDIUM, null, "alpha")
        )
        src.close()

        BackupExporter.export(ctx, srcFile, srcKey, "backup-pw".toCharArray(), bakFile)
        srcKey.zero()

        // Backup file is the container, so byte 0 must be 'T' (magic "TBAK"),
        // and the body must differ from the source DB (it was rekeyed).
        assertEquals('T'.code.toByte(), bakFile.readBytes()[0])
        assertNotEquals(srcFile.readBytes().toList(), bakFile.readBytes().toList())

        val dstKey = BackupImporter.import(ctx, bakFile, "backup-pw".toCharArray(), dstFile)
        val dst = DatabaseFactory.open(ctx, dstFile, dstKey)
        val rows = dst.cycleEntryDao().all()
        assertEquals(1, rows.size)
        assertEquals("alpha", rows[0].notes)
        dst.close()
        dstKey.zero()

        listOf(srcFile, bakFile, dstFile).forEach { it.delete() }
    }

    @Test
    fun import_with_wrong_password_rejected_at_verifier() = runBlocking {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val srcFile = File(ctx.filesDir, "src2.db")
        val bakFile = File(ctx.filesDir, "backup2.tides")
        val dstFile = File(ctx.filesDir, "dst2.db")
        listOf(srcFile, bakFile, dstFile).forEach { it.delete() }

        val srcKey = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        DatabaseFactory.open(ctx, srcFile, srcKey).also {
            it.cycleEntryDao().upsert(
                CycleEntryEntity(LocalDate.parse("2026-05-01"), FlowIntensity.MEDIUM, null, "secret")
            )
            it.close()
        }

        BackupExporter.export(ctx, srcFile, srcKey, "right-pw".toCharArray(), bakFile)
        srcKey.zero()

        try {
            BackupImporter.import(ctx, bakFile, "WRONG-pw".toCharArray(), dstFile)
            fail("expected WrongPassword")
        } catch (e: BackupImporter.WrongPassword) {
            // expected — verifier mismatch, dstFile should not exist
            assertEquals(false, dstFile.exists())
        }

        listOf(srcFile, bakFile, dstFile).forEach { it.delete() }
    }

    @Test
    fun import_then_rekey_to_primary_yields_a_db_unlockable_by_primary_key() = runBlocking {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val srcFile = File(ctx.filesDir, "src_restore.db")
        val bakFile = File(ctx.filesDir, "backup_restore.tides")
        val payloadFile = File(ctx.filesDir, "payload_restore.db")
        listOf(srcFile, bakFile, payloadFile).forEach { it.delete() }

        // Source DB encrypted with the user's primary key.
        val primaryKey = KeyDerivation.deriveKey(Pin("111111".toCharArray()), ByteArray(16) { 7 })
        DatabaseFactory.open(ctx, srcFile, primaryKey).also {
            it.cycleEntryDao().upsert(
                CycleEntryEntity(LocalDate.parse("2026-04-15"), FlowIntensity.HEAVY, null, "before")
            )
            it.close()
        }

        BackupExporter.export(ctx, srcFile, primaryKey, "off-device-pw".toCharArray(), bakFile)

        // Importer extracts the payload (still encrypted under backup key).
        val backupKey = BackupImporter.import(
            ctx, bakFile, "off-device-pw".toCharArray(), payloadFile,
        )
        try {
            // Rekey payload from backup key to primary key.
            System.loadLibrary("sqlcipher")
            val backupPwd = backupKey.bytes.copyOf()
            try {
                val db = net.zetetic.database.sqlcipher.SQLiteDatabase
                    .openOrCreateDatabase(payloadFile, backupPwd, null, null)
                try {
                    val hex = primaryKey.bytes.joinToString("") { "%02x".format(it) }
                    db.rawExecSQL("PRAGMA rekey = \"x'$hex'\"")
                } finally {
                    db.close()
                }
            } finally {
                java.util.Arrays.fill(backupPwd, 0)
            }
        } finally {
            backupKey.zero()
        }

        // The rekeyed payload should now open with the primary key.
        val restored = DatabaseFactory.open(ctx, payloadFile, primaryKey)
        val rows = restored.cycleEntryDao().all()
        assertEquals(1, rows.size)
        assertEquals("before", rows[0].notes)
        restored.close()
        primaryKey.zero()

        listOf(srcFile, bakFile, payloadFile).forEach { it.delete() }
    }

    @Test
    fun import_rejects_non_tides_file() = runBlocking {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val bakFile = File(ctx.filesDir, "junk.tides")
        val dstFile = File(ctx.filesDir, "dst3.db")
        listOf(bakFile, dstFile).forEach { it.delete() }
        bakFile.writeBytes(ByteArray(200) { 0x42 })

        try {
            BackupImporter.import(ctx, bakFile, "anything".toCharArray(), dstFile)
            fail("expected BadBackup")
        } catch (e: BackupImporter.BadBackup) {
            // expected
        }

        listOf(bakFile, dstFile).forEach { it.delete() }
    }
}
