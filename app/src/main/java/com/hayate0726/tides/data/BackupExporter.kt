package com.hayate0726.tides.data

import android.content.Context
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.security.SecureRandom
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Exports a Tides backup: a re-encrypted copy of the live SQLCipher database
 * under a fresh backup password, wrapped in a Tides container format.
 *
 * Container format (v1):
 *   bytes  0..3   magic "TBAK"
 *   byte   4      version (0x01)
 *   bytes  5..20  16-byte key salt   (Argon2id input -> 32-byte backup DB key)
 *   bytes 21..36  16-byte verifier salt (separate from key salt by design)
 *   bytes 37..68  32-byte Argon2id verifier hash of (password, verifierSalt)
 *   bytes 69..    rekeyed SQLCipher payload (encrypted with backup DB key)
 *
 * The two salts are independent so an attacker who exfiltrates the header
 * cannot equate "I have a matching verifier" with "I have the DB key" — both
 * require running Argon2id separately at full cost.
 *
 * The verifier lets [BackupImporter] reject wrong passwords up front in one
 * Argon2id cost, instead of letting SQLCipher fail mid-open and leaving a
 * partially-written destination DB on disk.
 */
object BackupExporter {

    private val MAGIC = "TBAK".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 0x01
    private const val SALT_SIZE = 16
    private const val VERIFIER_SIZE = 32
    const val HEADER_SIZE = 4 + 1 + SALT_SIZE + SALT_SIZE + VERIFIER_SIZE

    private val nativeLibLoaded = AtomicBoolean(false)

    /**
     * @param srcFile The unlocked, live SQLCipher database file.
     * @param srcKey  The active DB key for [srcFile]. NOT zeroed by this call —
     *                caller still owns it (the app is unlocked).
     * @param backupPassword Chars chosen by the user. Copied and zeroed internally.
     * @param outFile Destination container file (overwritten).
     */
    suspend fun export(
        ctx: Context,
        srcFile: File,
        srcKey: DbKey,
        backupPassword: CharArray,
        outFile: File,
    ) {
        if (nativeLibLoaded.compareAndSet(false, true)) {
            System.loadLibrary("sqlcipher")
        }

        val rng = SecureRandom()
        val keySalt = ByteArray(SALT_SIZE).also { rng.nextBytes(it) }
        val verifierSalt = ByteArray(SALT_SIZE).also { rng.nextBytes(it) }

        val pin = Pin(backupPassword.copyOf())
        val backupKey: DbKey
        val verifier: ByteArray
        try {
            backupKey = KeyDerivation.deriveKey(pin, keySalt)
            verifier = KeyDerivation.derivePinHash(pin, verifierSalt)
        } finally {
            pin.zero()
        }

        val tmpFile = File(ctx.cacheDir, "backup_tmp_${System.nanoTime()}.db")
        try {
            srcFile.copyTo(tmpFile, overwrite = true)

            // Re-encrypt tmpFile in place: open with srcKey, PRAGMA rekey to backupKey, close.
            val srcPwd = srcKey.bytes.copyOf()
            try {
                val db = SQLiteDatabase.openOrCreateDatabase(tmpFile, srcPwd, null, null)
                try {
                    db.rawExecSQL("PRAGMA rekey = \"x'${backupKey.bytes.toHex()}'\"")
                } finally {
                    db.close()
                }
            } finally {
                Arrays.fill(srcPwd, 0)
            }

            outFile.outputStream().use { out ->
                out.write(MAGIC)
                out.write(byteArrayOf(VERSION))
                out.write(keySalt)
                out.write(verifierSalt)
                out.write(verifier)
                tmpFile.inputStream().use { it.copyTo(out) }
            }
        } finally {
            backupKey.zero()
            Arrays.fill(verifier, 0)
            tmpFile.delete()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
