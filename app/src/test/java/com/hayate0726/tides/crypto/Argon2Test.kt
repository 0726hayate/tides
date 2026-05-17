package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 * Note: Argon2kt ships native code. These tests run on the JVM via Robolectric
 * if the platform supports it; on hosts without the right native arch they will
 * be skipped. Full coverage relies on instrumented tests on real Android.
 */
@Disabled("argon2kt native lib not available on JVM; covered by instrumented tests in Plan 1 Task 8/9")
@EnabledOnOs(OS.LINUX, OS.MAC, OS.WINDOWS)
class Argon2Test {

    @Test
    fun `derive produces 32-byte output`() {
        val out = Argon2.derive(
            password = "test-pin".toByteArray(),
            salt = ByteArray(16) { 7 },
            params = Argon2.Params.DEFAULT
        )
        assertEquals(32, out.size)
    }

    @Test
    fun `derive is deterministic for same inputs`() {
        val a = Argon2.derive("pin".toByteArray(), ByteArray(16), Argon2.Params.DEFAULT)
        val b = Argon2.derive("pin".toByteArray(), ByteArray(16), Argon2.Params.DEFAULT)
        assertArrayEquals(a, b)
    }

    @Test
    fun `different salts produce different outputs`() {
        val a = Argon2.derive("pin".toByteArray(), ByteArray(16) { 1 }, Argon2.Params.DEFAULT)
        val b = Argon2.derive("pin".toByteArray(), ByteArray(16) { 2 }, Argon2.Params.DEFAULT)
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun `salt shorter than 16 bytes throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Argon2.derive("pin".toByteArray(), ByteArray(8), Argon2.Params.DEFAULT)
        }
    }

    @Test
    fun `default params match spec`() {
        val p = Argon2.Params.DEFAULT
        assertEquals(64 * 1024, p.memoryCostKib)
        assertEquals(3, p.iterations)
        assertEquals(1, p.parallelism)
    }
}
