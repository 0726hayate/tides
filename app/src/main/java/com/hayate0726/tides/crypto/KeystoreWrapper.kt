package com.hayate0726.tides.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps and unwraps short byte arrays (the DB key) using a key held in
 * Android Keystore. Uses AES-256-GCM. Output format: 12-byte IV || ciphertext+tag.
 *
 * Two flavors:
 *  - requireBiometric = true  -> wrapping key requires BiometricPrompt to unlock
 *  - requireBiometric = false -> wrapping key is bound to this app on this device,
 *    no per-use auth (used for "Just for me" preset).
 *
 * The wrapping key itself never leaves the Keystore.
 */
object KeystoreWrapper {

    private const val PROVIDER = "AndroidKeyStore"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun wrap(alias: String, requireBiometric: Boolean, plaintext: ByteArray): ByteArray {
        val key = getOrCreateKey(alias, requireBiometric)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "expected $IV_BYTES-byte IV, got ${iv.size}" }
        return ByteBuffer.allocate(IV_BYTES + ciphertext.size)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun unwrap(alias: String, wrapped: ByteArray): ByteArray {
        require(wrapped.size > IV_BYTES) { "wrapped data too short" }
        val key = loadKey(alias)
            ?: throw IllegalStateException("no Keystore entry for alias $alias")
        val iv = wrapped.copyOfRange(0, IV_BYTES)
        val ciphertext = wrapped.copyOfRange(IV_BYTES, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun aliasExists(alias: String): Boolean {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return ks.containsAlias(alias)
    }

    fun deleteKey(alias: String) {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private fun getOrCreateKey(alias: String, requireBiometric: Boolean): SecretKey {
        loadKey(alias)?.let { return it }
        val gen = KeyGenerator.getInstance(ALGORITHM, PROVIDER)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (requireBiometric) {
            specBuilder.setUserAuthenticationRequired(true)
            specBuilder.setInvalidatedByBiometricEnrollment(true)
        }
        gen.init(specBuilder.build())
        return gen.generateKey()
    }

    private fun loadKey(alias: String): SecretKey? {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return ks.getKey(alias, null) as? SecretKey
    }
}
