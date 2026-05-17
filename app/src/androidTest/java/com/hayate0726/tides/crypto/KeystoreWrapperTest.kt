package com.hayate0726.tides.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreWrapperTest {

    @After
    fun cleanup() {
        runCatching { KeystoreWrapper.deleteKey(NON_BIOMETRIC_ALIAS) }
        runCatching { KeystoreWrapper.deleteKey(SECONDARY_ALIAS) }
    }

    @Test
    fun wrap_and_unwrap_round_trips_a_32_byte_key() {
        val original = ByteArray(32) { (it + 1).toByte() }
        val wrapped = KeystoreWrapper.wrap(
            alias = NON_BIOMETRIC_ALIAS,
            requireBiometric = false,
            plaintext = original,
        )
        val unwrapped = KeystoreWrapper.unwrap(NON_BIOMETRIC_ALIAS, wrapped)
        assertArrayEquals(original, unwrapped)
    }

    @Test
    fun wrap_with_different_aliases_produces_different_outputs() {
        val plaintext = ByteArray(32) { 7 }
        val w1 = KeystoreWrapper.wrap(NON_BIOMETRIC_ALIAS, false, plaintext)
        val w2 = KeystoreWrapper.wrap(SECONDARY_ALIAS, false, plaintext)
        assertNotEquals(w1.toList(), w2.toList())
    }

    @Test
    fun deleteKey_removes_the_alias() {
        KeystoreWrapper.wrap(NON_BIOMETRIC_ALIAS, false, ByteArray(32) { 1 })
        KeystoreWrapper.deleteKey(NON_BIOMETRIC_ALIAS)
        assertFalse(KeystoreWrapper.aliasExists(NON_BIOMETRIC_ALIAS))
    }

    companion object {
        private const val NON_BIOMETRIC_ALIAS = "tides.test.non_biometric"
        private const val SECONDARY_ALIAS = "tides.test.secondary"
    }
}
