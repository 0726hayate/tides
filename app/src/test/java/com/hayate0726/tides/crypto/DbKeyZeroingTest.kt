package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows

class DbKeyZeroingTest {

    @Test
    fun `zero wipes the underlying bytes`() {
        val original = ByteArray(32) { (it + 1).toByte() }
        val key = DbKey(original)
        key.zero()
        // After zero, the bytes the caller still holds a reference to are also wiped
        assertArrayEquals(ByteArray(32), original)
    }

    @Test
    fun `accessing bytes after zero throws`() {
        val key = DbKey(ByteArray(32) { 1 })
        key.zero()
        assertThrows(IllegalStateException::class.java) {
            key.bytes
        }
    }

    @Test
    fun `pin zero wipes underlying char array`() {
        val original = "123456".toCharArray()
        val pin = Pin(original)
        pin.zero()
        assertArrayEquals(CharArray(6) { 0.toChar() }, original)
    }
}
