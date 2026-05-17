package com.hayate0726.tides.crypto

import java.security.MessageDigest
import java.util.Arrays

/**
 * The only public API for turning a PIN into key material.
 *
 * Two distinct derivations, each with its own salt:
 *
 *  - deriveKey: PIN + key-salt -> 32-byte database key (used by SQLCipher).
 *  - derivePinHash: PIN + hash-salt -> 32-byte hash (stored to validate the
 *    PIN before attempting DB open; lets us detect wrong PINs in O(Argon2)
 *    rather than O(SQLCipher-open), and lets us check for the duress PIN
 *    without ever opening the wrong database).
 *
 * Both derivations use the same Argon2 params. The two salts must differ —
 * using the same salt for both would let an attacker who steals the disk
 * image equate "I have the hash" with "I have the key."
 *
 * Caller is responsible for managing the lifetime of the returned bytes.
 * derive* accept a Pin but do not zero it — the caller decides when.
 */
object KeyDerivation {

    fun deriveKey(pin: Pin, keySalt: ByteArray): DbKey {
        val pwd = pin.toUtf8Bytes()
        try {
            val raw = Argon2.derive(pwd, keySalt, Argon2.Params.DEFAULT)
            return DbKey(raw)
        } finally {
            Arrays.fill(pwd, 0)
        }
    }

    fun derivePinHash(pin: Pin, hashSalt: ByteArray): ByteArray {
        val pwd = pin.toUtf8Bytes()
        try {
            return Argon2.derive(pwd, hashSalt, Argon2.Params.DEFAULT)
        } finally {
            Arrays.fill(pwd, 0)
        }
    }

    fun validatePin(pin: Pin, hashSalt: ByteArray, storedHash: ByteArray): Boolean {
        val candidate = derivePinHash(pin, hashSalt)
        try {
            return MessageDigest.isEqual(candidate, storedHash)
        } finally {
            Arrays.fill(candidate, 0)
        }
    }
}
