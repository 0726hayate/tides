package com.hayate0726.tides.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.Placeholder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Invariant: a PIN we feed in, and a row payload we write, MUST NOT
 * appear in logcat. If this test fails, something is logging sensitive
 * content (Room's verbose logging, a debug println, etc).
 */
@RunWith(AndroidJUnit4::class)
class NoSensitiveLogsTest {

    private val secretMarker = "PIN_MARKER_4f8a9e2b"
    private val flowMarker = "FLOW_MARKER_dead1234"

    @Test
    fun logcat_does_not_leak_pin_or_flow_data(): Unit = runBlocking {
        Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor()

        val ctx: Context = ApplicationProvider.getApplicationContext()
        val dbFile = File(ctx.filesDir, "log_leak_check.db")
        dbFile.delete()
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-wal").delete()

        val pin = Pin(secretMarker.toCharArray())
        val key = KeyDerivation.deriveKey(pin, ByteArray(16) { 5 })
        pin.zero()

        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.placeholderDao().insert(Placeholder(1, flowMarker))
        db.close()
        key.zero()

        val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "raw"))
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        val all = reader.readText()
        proc.destroy()

        assertFalse("PIN content appeared in logcat", all.contains(secretMarker))
        assertFalse("Flow content appeared in logcat", all.contains(flowMarker))

        dbFile.delete()
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-wal").delete()
    }
}
