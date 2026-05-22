package com.hayate0726.tides.data.`import`

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * One-slot snapshot of the encrypted Tides DB taken before each import.
 *
 * The snapshot lives in app-private storage alongside the live DB. SQLCipher's
 * encryption applies to the file bytes, so the snapshot is protected by the
 * same DB key as the live database. On restore, the snapshot is copied back
 * over the live DB file and then deleted. On restore failure, the snapshot is
 * preserved so the user can retry.
 */
class PreImportSnapshot(
    private val liveDb: File,
    private val snapshotFile: File,
) {
    companion object {
        const val MIN_FREE_BYTES = 25L * 1024 * 1024 // 25 MB
    }

    class InsufficientSpace(msg: String) : IOException(msg)

    fun exists(): Boolean = snapshotFile.exists() && snapshotFile.length() > 0

    /**
     * Copy the live DB to the snapshot slot. Overwrites any prior snapshot.
     * Returns the snapshot file on success. Caller is responsible for not
     * holding an open SQLCipher connection on the live DB while this runs.
     */
    fun take(): File {
        if (!liveDb.exists()) {
            throw IOException("Live DB does not exist at ${liveDb.absolutePath}")
        }
        val freeBytes = liveDb.parentFile?.usableSpace ?: 0L
        if (freeBytes < MIN_FREE_BYTES) {
            throw InsufficientSpace("Need at least ${MIN_FREE_BYTES / (1024 * 1024)} MB free")
        }
        snapshotFile.parentFile?.mkdirs()
        val tmp = File(snapshotFile.parentFile, snapshotFile.name + ".tmp")
        liveDb.inputStream().use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        moveAtomically(tmp, snapshotFile)
        return snapshotFile
    }

    /**
     * Restore the snapshot over the live DB. Returns true on success. The
     * snapshot file is deleted on success and preserved on failure. Caller MUST
     * ensure the live DB is closed before calling this.
     */
    fun restore(): Boolean {
        if (!exists()) return false
        val tmp = File(liveDb.parentFile, liveDb.name + ".restore.tmp")
        try {
            snapshotFile.inputStream().use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            // Atomic replace eliminates the delete-then-rename window where a
            // process kill could leave the live DB missing.
            moveAtomically(tmp, liveDb)
            snapshotFile.delete()
            return true
        } catch (e: IOException) {
            tmp.delete()
            return false
        }
    }

    private fun moveAtomically(src: File, dst: File) {
        try {
            Files.move(
                src.toPath(), dst.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (e: AtomicMoveNotSupportedException) {
            // Same-filesystem atomic move isn't supported (rare on Android internal
            // storage but possible across mount points). Fall back to replace-only.
            Files.move(
                src.toPath(), dst.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}
