package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class FileAuthMetaStoreTest {

    @Test
    fun `load throws when file missing`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "missing_auth_meta.bin")
        val store = FileAuthMetaStore(file)
        assertThrows(Exception::class.java) { store.load() }
    }

    @Test
    fun `initialize creates file with correct data`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "init_auth_meta.bin")
        val store = FileAuthMetaStore(file)
        val meta = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )
        store.initialize(meta)
        val read = store.load()
        assertArrayEquals(meta.pinHash, read.pinHash)
    }

    @Test
    fun `update writes atomically and can be re-read`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "update_auth_meta.bin")
        val store = FileAuthMetaStore(file)
        store.initialize(
            AuthMeta(
                keySalt = ByteArray(16),
                pinHashSalt = ByteArray(16),
                pinHash = ByteArray(32),
                duress = null,
                failCount = 0,
                cooldownExpiryEpochMs = 0L,
            )
        )
        store.update { m ->
            AuthMeta(m.keySalt, m.pinHashSalt, m.pinHash, m.duress, 7, 999L)
        }
        val read = store.load()
        assertEquals(7, read.failCount)
        assertEquals(999L, read.cooldownExpiryEpochMs)
    }
}
