package com.hayate0726.tides.crypto

import java.nio.CharBuffer
import java.util.Arrays

/**
 * Wraps a PIN or passphrase as a CharArray (chars, not String, because
 * String is immutable and lingers in the JVM string pool).
 *
 * `zero()` overwrites the underlying chars and detaches the reference.
 */
class Pin(chars: CharArray) {
    init {
        require(chars.isNotEmpty()) { "PIN must not be empty" }
    }

    private var _chars: CharArray? = chars

    val chars: CharArray
        get() = _chars ?: throw IllegalStateException("Pin has been zeroed")

    /**
     * UTF-8 bytes of the PIN. Caller is responsible for zeroing the
     * returned array if it holds it.
     */
    fun toUtf8Bytes(): ByteArray {
        val chars = _chars ?: throw IllegalStateException("Pin has been zeroed")
        val byteBuffer = Charsets.UTF_8.encode(CharBuffer.wrap(chars))
        val out = ByteArray(byteBuffer.remaining())
        byteBuffer.get(out)
        Arrays.fill(byteBuffer.array(), 0)
        return out
    }

    fun zero() {
        _chars?.let { Arrays.fill(it, 0.toChar()) }
        _chars = null
    }

    val isZeroed: Boolean get() = _chars == null
}
