package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class AuthMetaTest {

    @Test
    fun `round-trip without duress`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        val original = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )
        AuthMeta.write(file, original)
        val read = AuthMeta.read(file)
        assertArrayEquals(original.keySalt, read.keySalt)
        assertArrayEquals(original.pinHashSalt, read.pinHashSalt)
        assertArrayEquals(original.pinHash, read.pinHash)
        assertNull(read.duress)
        assertEquals(0, read.failCount)
        assertEquals(0L, read.cooldownExpiryEpochMs)
    }

    @Test
    fun `round-trip with duress decoy`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        val original = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = AuthMeta.Duress(
                keySalt = ByteArray(16) { 4 },
                pinHashSalt = ByteArray(16) { 5 },
                pinHash = ByteArray(32) { 6 },
                mode = AuthMeta.DuressMode.DECOY,
            ),
            failCount = 2,
            cooldownExpiryEpochMs = 1_700_000_000_000L,
        )
        AuthMeta.write(file, original)
        val read = AuthMeta.read(file)
        assertNotNull(read.duress)
        assertEquals(AuthMeta.DuressMode.DECOY, read.duress!!.mode)
        assertArrayEquals(original.duress!!.pinHash, read.duress!!.pinHash)
        assertEquals(2, read.failCount)
        assertEquals(1_700_000_000_000L, read.cooldownExpiryEpochMs)
    }

    @Test
    fun `round-trip with duress wipe`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        val original = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = AuthMeta.Duress(
                keySalt = ByteArray(16) { 4 },
                pinHashSalt = ByteArray(16) { 5 },
                pinHash = ByteArray(32) { 6 },
                mode = AuthMeta.DuressMode.WIPE,
            ),
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )
        AuthMeta.write(file, original)
        val read = AuthMeta.read(file)
        assertEquals(AuthMeta.DuressMode.WIPE, read.duress!!.mode)
    }

    @Test
    fun `corrupted magic throws`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        file.writeBytes(ByteArray(147) { 0xFF.toByte() })
        try {
            AuthMeta.read(file)
            assertFalse(true, "expected throw")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("magic"))
        }
    }

    @Test
    fun `wrong length throws`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        file.writeBytes(ByteArray(50))
        try {
            AuthMeta.read(file)
            assertFalse(true, "expected throw")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("length"))
        }
    }
}
