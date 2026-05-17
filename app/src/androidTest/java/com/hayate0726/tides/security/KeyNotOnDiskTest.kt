package com.hayate0726.tides.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Invariant: the 32-byte derived database key MUST NOT appear in clear
 * inside any app-private file. If this test ever fails, we've leaked
 * the key to disk.
 */
@RunWith(AndroidJUnit4::class)
class KeyNotOnDiskTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @After
    fun cleanup() {
        if (::dbFile.isInitialized) {
            dbFile.delete()
            File(dbFile.absolutePath + "-shm").delete()
            File(dbFile.absolutePath + "-wal").delete()
        }
    }

    @Test
    fun no_app_private_file_contains_the_derived_key_bytes(): Unit = runBlocking {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "key_leak_check.db")
        dbFile.delete()

        val key: DbKey = KeyDerivation.deriveKey(
            Pin("123456".toCharArray()),
            ByteArray(16) { 11 }
        )
        val needle = key.bytes.copyOf()

        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.cycleEntryDao().upsert(
            CycleEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                flowIntensity = FlowIntensity.LIGHT,
                painSeverity = null,
                notes = "x",
            )
        )
        db.close()
        key.zero()

        val roots = listOf(ctx.filesDir, ctx.cacheDir, ctx.dataDir)
        for (root in roots) {
            scanFiles(root) { f ->
                val data = f.readBytes()
                assertFalse(
                    "Key bytes found in app-private file: ${f.absolutePath}",
                    contains(data, needle),
                )
            }
        }
    }

    private fun scanFiles(root: File, body: (File) -> Unit) {
        if (!root.exists()) return
        root.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0L) body(f)
        }
    }

    private fun contains(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }
}
