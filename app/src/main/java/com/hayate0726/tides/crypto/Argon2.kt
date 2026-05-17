package com.hayate0726.tides.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode

/**
 * Thin wrapper around argon2kt. All key/hash derivation in the app must use
 * this; never call argon2kt directly. Centralizing makes audit easier.
 *
 * Output is always 32 bytes (256 bits), enough for AES-256 / SQLCipher.
 */
object Argon2 {

    data class Params(
        val memoryCostKib: Int,
        val iterations: Int,
        val parallelism: Int,
    ) {
        companion object {
            /**
             * Tuned for ~300–500ms unlock latency on a midrange Android phone.
             * Memory cost = 64 MiB (well above OWASP 2024 floor of 19 MiB).
             * Iterations = 3, Parallelism = 1.
             * See spec §4.
             */
            val DEFAULT = Params(
                memoryCostKib = 64 * 1024,
                iterations = 3,
                parallelism = 1,
            )
        }
    }

    private const val SALT_MIN_BYTES = 16
    private const val HASH_BYTES = 32

    private val argon2 = Argon2Kt()

    /**
     * Derive a 32-byte key from `password` and `salt` using Argon2id.
     * `salt` must be at least 16 bytes. Caller is responsible for zeroing
     * `password` after this call returns.
     */
    fun derive(password: ByteArray, salt: ByteArray, params: Params): ByteArray {
        require(salt.size >= SALT_MIN_BYTES) {
            "salt must be at least $SALT_MIN_BYTES bytes, got ${salt.size}"
        }
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = salt,
            tCostInIterations = params.iterations,
            mCostInKibibyte = params.memoryCostKib,
            parallelism = params.parallelism,
            hashLengthInBytes = HASH_BYTES,
        )
        return result.rawHashAsByteArray()
    }
}
