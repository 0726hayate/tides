package com.hayate0726.tides.crypto

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the SQLCipher DB key wrapped under a biometric-bound Keystore alias.
 *
 * File at `filesDir/biometric.bin`:
 *   bytes 0..3   magic "TBIO"
 *   byte  4      version 0x01
 *   bytes 5..    KeystoreWrapper.wrap() output (12-byte IV || ciphertext || GCM tag)
 *
 * Separate from auth_meta.bin so a corrupt or invalidated biometric blob
 * doesn't risk the PIN-unlock path.
 */
@Singleton
class BiometricKeyStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val file: File get() = File(ctx.filesDir, "biometric.bin")
    private val magic = "TBIO".toByteArray(Charsets.US_ASCII)
    private val version: Byte = 0x01

    fun isEnrolled(): Boolean = file.exists()

    /** Wrap the DB key under the biometric alias and persist. Overwrites prior blob. */
    fun enroll(dbKey: DbKey) {
        val wrapped = KeystoreWrapper.wrap(ALIAS, requireBiometric = true, plaintext = dbKey.bytes)
        val buf = ByteBuffer.allocate(4 + 1 + wrapped.size)
        buf.put(magic)
        buf.put(version)
        buf.put(wrapped)
        file.writeBytes(buf.array())
    }

    /**
     * Unwrap the stored DB key. Throws if biometric.bin is missing, malformed,
     * or the Keystore key is permanently invalidated (re-enrolled fingerprints).
     */
    fun unwrap(): DbKey {
        check(file.exists()) { "no biometric enrollment" }
        val bytes = file.readBytes()
        require(bytes.size > 5) { "biometric.bin truncated" }
        require(bytes.copyOfRange(0, 4).contentEquals(magic)) { "bad magic" }
        require(bytes[4] == version) { "unsupported version ${bytes[4]}" }
        val plaintext = KeystoreWrapper.unwrap(ALIAS, bytes.copyOfRange(5, bytes.size))
        return DbKey(plaintext)
    }

    /** Remove biometric enrollment (Keystore alias + on-disk blob). Idempotent. */
    fun clear() {
        file.delete()
        KeystoreWrapper.deleteKey(ALIAS)
    }

    companion object {
        const val ALIAS = "tides.biometric.v1"
    }
}
