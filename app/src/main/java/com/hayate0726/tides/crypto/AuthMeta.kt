package com.hayate0726.tides.crypto

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Fixed-size binary container for non-secret authentication metadata.
 *
 * Layout (147 bytes total, big-endian):
 *
 *  offset  size  field
 *  ------  ----  -------------------------------
 *       0     4  magic = 0x54494445  ("TIDE")
 *       4     1  version = 0x01
 *       5     1  has_duress (0 or 1)
 *       6     1  duress_mode (0=off, 1=decoy, 2=wipe)
 *       7    16  primary key_salt
 *      23    16  primary pin_hash_salt
 *      39    32  primary pin_hash
 *      71    16  duress key_salt (zeros if has_duress=0)
 *      87    16  duress pin_hash_salt (zeros if has_duress=0)
 *     103    32  duress pin_hash (zeros if has_duress=0)
 *     135     4  fail_count (uint32, big-endian)
 *     139     8  cooldown_expiry_epoch_ms (int64, big-endian)
 *
 * The file is allowed to exist on disk in plaintext: it contains
 * no key material, only salts (random by construction) and Argon2 hashes
 * of the PINs (computationally infeasible to invert).
 */
class AuthMeta(
    val keySalt: ByteArray,
    val pinHashSalt: ByteArray,
    val pinHash: ByteArray,
    val duress: Duress?,
    val failCount: Int,
    val cooldownExpiryEpochMs: Long,
) {
    init {
        require(keySalt.size == 16) { "keySalt must be 16 bytes" }
        require(pinHashSalt.size == 16) { "pinHashSalt must be 16 bytes" }
        require(pinHash.size == 32) { "pinHash must be 32 bytes" }
    }

    class Duress(
        val keySalt: ByteArray,
        val pinHashSalt: ByteArray,
        val pinHash: ByteArray,
        val mode: DuressMode,
    ) {
        init {
            require(keySalt.size == 16) { "duress keySalt must be 16 bytes" }
            require(pinHashSalt.size == 16) { "duress pinHashSalt must be 16 bytes" }
            require(pinHash.size == 32) { "duress pinHash must be 32 bytes" }
            require(mode != DuressMode.OFF) { "duress != null implies mode != OFF" }
        }
    }

    enum class DuressMode(val byte: Byte) {
        OFF(0), DECOY(1), WIPE(2);
        companion object {
            fun fromByte(b: Byte): DuressMode =
                entries.firstOrNull { it.byte == b }
                    ?: throw IllegalStateException("Unknown duress mode byte: $b")
        }
    }

    companion object {
        private const val MAGIC: Int = 0x54494445  // "TIDE"
        private const val VERSION: Byte = 0x01
        const val FILE_SIZE: Int = 147

        fun write(file: File, meta: AuthMeta) {
            val buf = ByteBuffer.allocate(FILE_SIZE).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(MAGIC)
            buf.put(VERSION)
            buf.put(if (meta.duress != null) 1.toByte() else 0.toByte())
            buf.put(meta.duress?.mode?.byte ?: DuressMode.OFF.byte)
            buf.put(meta.keySalt)
            buf.put(meta.pinHashSalt)
            buf.put(meta.pinHash)
            if (meta.duress != null) {
                buf.put(meta.duress.keySalt)
                buf.put(meta.duress.pinHashSalt)
                buf.put(meta.duress.pinHash)
            } else {
                buf.put(ByteArray(16))
                buf.put(ByteArray(16))
                buf.put(ByteArray(32))
            }
            buf.putInt(meta.failCount)
            buf.putLong(meta.cooldownExpiryEpochMs)

            val parent = file.parentFile ?: file.absoluteFile.parentFile
            val tmp = File(parent, "${file.name}.tmp")
            tmp.writeBytes(buf.array())
            check(tmp.renameTo(file)) { "atomic rename failed" }
        }

        fun read(file: File): AuthMeta {
            val bytes = file.readBytes()
            check(bytes.size == FILE_SIZE) {
                "auth_meta.bin has wrong length: got ${bytes.size}, expected $FILE_SIZE"
            }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val magic = buf.int
            check(magic == MAGIC) {
                "auth_meta.bin has bad magic: got %08x".format(magic)
            }
            val version = buf.get()
            check(version == VERSION) { "unsupported version: $version" }
            val hasDuress = buf.get() == 1.toByte()
            val duressMode = DuressMode.fromByte(buf.get())

            val keySalt = ByteArray(16).also { buf.get(it) }
            val pinHashSalt = ByteArray(16).also { buf.get(it) }
            val pinHash = ByteArray(32).also { buf.get(it) }

            val duressKeySalt = ByteArray(16).also { buf.get(it) }
            val duressPinHashSalt = ByteArray(16).also { buf.get(it) }
            val duressPinHash = ByteArray(32).also { buf.get(it) }

            val failCount = buf.int
            val cooldownExpiryEpochMs = buf.long

            val duress = if (hasDuress) {
                Duress(duressKeySalt, duressPinHashSalt, duressPinHash, duressMode)
            } else {
                null
            }
            return AuthMeta(keySalt, pinHashSalt, pinHash, duress, failCount, cooldownExpiryEpochMs)
        }
    }
}
