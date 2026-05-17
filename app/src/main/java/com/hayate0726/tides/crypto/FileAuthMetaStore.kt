package com.hayate0726.tides.crypto

import com.hayate0726.tides.lock.LockManager
import java.io.File

/**
 * File-backed LockManager.AuthMetaStore. The file is auth_meta.bin in
 * the app-private files directory. Reads are direct; writes go through
 * AuthMeta.write which performs an atomic-rename.
 *
 * Thread-safety: writes synchronize on `this`. Reads are lock-free and
 * may briefly see a stale value during an update; the atomic rename
 * ensures readers always see either the old or new file in full.
 */
class FileAuthMetaStore(private val file: File) : LockManager.AuthMetaStore {

    fun initialize(meta: AuthMeta) {
        synchronized(this) {
            AuthMeta.write(file, meta)
        }
    }

    override fun load(): AuthMeta = AuthMeta.read(file)

    override fun update(updater: (AuthMeta) -> AuthMeta) {
        synchronized(this) {
            val current = AuthMeta.read(file)
            val next = updater(current)
            AuthMeta.write(file, next)
        }
    }
}
