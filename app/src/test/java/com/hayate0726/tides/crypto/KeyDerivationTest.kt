package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * KeyDerivation depends on Argon2 (JNI). The native lib doesn't load on the
 * JVM/Robolectric, so these tests are disabled here and covered by
 * instrumented tests on a real device (Plan 1 Task 8/9).
 */
@Disabled("argon2kt native lib not available on JVM; covered by instrumented tests in Plan 1 Task 8/9")
class KeyDerivationTest {

    @Test
    fun `deriveKey returns a 32-byte DbKey`() {
        val pin = Pin("123456".toCharArray())
        val salt = ByteArray(16) { 1 }
        val key = KeyDerivation.deriveKey(pin, salt)
        assertEquals(32, key.bytes.size)
        assertFalse(key.isZeroed)
    }

    @Test
    fun `same pin and salt produce same DbKey bytes`() {
        val pin1 = Pin("123456".toCharArray())
        val pin2 = Pin("123456".toCharArray())
        val salt = ByteArray(16) { 1 }
        val a = KeyDerivation.deriveKey(pin1, salt)
        val b = KeyDerivation.deriveKey(pin2, salt)
        assertArrayEquals(a.bytes, b.bytes)
    }

    @Test
    fun `different pins produce different keys`() {
        val a = KeyDerivation.deriveKey(Pin("111111".toCharArray()), ByteArray(16))
        val b = KeyDerivation.deriveKey(Pin("222222".toCharArray()), ByteArray(16))
        assertNotEquals(a.bytes.toList(), b.bytes.toList())
    }

    @Test
    fun `derivePinHash produces stable 32-byte hash`() {
        val h1 = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 2 }
        )
        val h2 = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 2 }
        )
        assertEquals(32, h1.size)
        assertArrayEquals(h1, h2)
    }

    @Test
    fun `pin hash and key are different (different salts)`() {
        val pin = Pin("123456".toCharArray())
        val key = KeyDerivation.deriveKey(pin, ByteArray(16) { 1 })
        val hash = KeyDerivation.derivePinHash(Pin("123456".toCharArray()), ByteArray(16) { 2 })
        assertNotEquals(key.bytes.toList(), hash.toList())
    }

    @Test
    fun `validatePin returns true for matching pin`() {
        val storedHash = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 3 }
        )
        assertTrue(
            KeyDerivation.validatePin(
                Pin("123456".toCharArray()),
                ByteArray(16) { 3 },
                storedHash
            )
        )
    }

    @Test
    fun `validatePin returns false for wrong pin`() {
        val storedHash = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 3 }
        )
        assertFalse(
            KeyDerivation.validatePin(
                Pin("999999".toCharArray()),
                ByteArray(16) { 3 },
                storedHash
            )
        )
    }
}
