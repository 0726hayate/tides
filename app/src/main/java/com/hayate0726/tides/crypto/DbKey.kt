package com.hayate0726.tides.crypto

import java.util.Arrays

/**
 * Wraps a 32-byte database encryption key. The underlying byte array is
 * the caller's; `zero()` wipes it in place. After `zero()`, `bytes` throws.
 *
 * Required size: 32 bytes (256 bits).
 */
class DbKey(bytes: ByteArray) {
    init {
        require(bytes.size == REQUIRED_SIZE) {
            "DbKey must be exactly $REQUIRED_SIZE bytes, got ${bytes.size}"
        }
    }

    private var _bytes: ByteArray? = bytes

    val bytes: ByteArray
        get() = _bytes ?: throw IllegalStateException("DbKey has been zeroed")

    fun zero() {
        _bytes?.let { Arrays.fill(it, 0) }
        _bytes = null
    }

    val isZeroed: Boolean get() = _bytes == null

    companion object {
        const val REQUIRED_SIZE = 32
    }
}
