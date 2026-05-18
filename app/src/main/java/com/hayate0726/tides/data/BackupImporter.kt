package com.hayate0726.tides.data

import android.content.Context
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Arrays

/**
 * Verifies a Tides backup container and extracts its payload to a destination
 * file, returning the [DbKey] needed to open that file via [DatabaseFactory].
 *
 * See [BackupExporter] for the container layout. We always run the verifier
 * check before writing anything to disk, so wrong-password attempts leave no
 * partial state and only cost one Argon2id derivation.
 */
object BackupImporter {

    private val MAGIC = "TBAK".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 0x01
    private const val SALT_SIZE = 16
    private const val VERIFIER_SIZE = 32

    class BadBackup(msg: String) : IOException(msg)
    class WrongPassword : IOException("Wrong backup password")

    /**
     * @return A [DbKey] the caller MUST use to open [outFile]. Caller owns
     *         the key and is responsible for zeroing it after use.
     */
    suspend fun import(
        ctx: Context,
        backupFile: File,
        backupPassword: CharArray,
        outFile: File,
    ): DbKey {
        backupFile.inputStream().use { input ->
            val magic = ByteArray(4)
            if (input.read(magic) != 4 || !magic.contentEquals(MAGIC)) {
                throw BadBackup("Not a Tides backup")
            }
            val version = input.read()
            if (version != VERSION.toInt()) {
                throw BadBackup("Unsupported backup version $version")
            }
            val keySalt = readExact(input, SALT_SIZE, "keySalt")
            val verifierSalt = readExact(input, SALT_SIZE, "verifierSalt")
            val storedVerifier = readExact(input, VERIFIER_SIZE, "verifier")

            val pin = Pin(backupPassword.copyOf())
            val passwordOk: Boolean
            val candidate = KeyDerivation.derivePinHash(pin, verifierSalt)
            try {
                passwordOk = MessageDigest.isEqual(candidate, storedVerifier)
            } finally {
                Arrays.fill(candidate, 0)
            }
            if (!passwordOk) {
                pin.zero()
                throw WrongPassword()
            }
            val dbKey = try {
                KeyDerivation.deriveKey(pin, keySalt)
            } finally {
                pin.zero()
            }

            // Verifier already validated; payload bytes are a self-contained
            // SQLCipher DB encrypted under dbKey.
            outFile.outputStream().use { out -> input.copyTo(out) }
            return dbKey
        }
    }

    private fun readExact(input: java.io.InputStream, n: Int, what: String): ByteArray {
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(buf, read, n - read)
            if (r < 0) throw BadBackup("Truncated backup header at $what")
            read += r
        }
        return buf
    }
}
