package com.hayate0726.tides.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.AppViewModel
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.BackupExporter
import com.hayate0726.tides.data.BackupImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Drives backup export and restore on top of [BackupExporter] / [BackupImporter].
 *
 * Both flows re-derive the live DB key from the user's primary PIN on demand
 * (the live key is zeroed after unlock per the Plan 1 invariant) — the key
 * exists only for the duration of one Argon2id derivation + one SQLCipher
 * `PRAGMA rekey` and is zeroed immediately after.
 *
 * Restore design (spec §5.8):
 *   1. Copy the SAF-picked Uri to a local temp.
 *   2. Validate primary PIN against the active [com.hayate0726.tides.crypto.AuthMeta].
 *   3. Validate backup container header + verifier (constant-time).
 *   4. Extract the rekeyed payload to a second temp.
 *   5. Open that temp with the backup-password-derived key and `PRAGMA rekey`
 *      back to the primary key. After this, the temp is encrypted with the
 *      same key the user's current PIN derives.
 *   6. Hand the temp to [AppViewModel.replaceDatabaseFile], which closes the
 *      live DB under its state mutex and atomically moves the temp into place.
 *   7. AppViewModel transitions to Locked; user re-unlocks with primary PIN.
 *
 * AuthMeta is not rewritten. The primary PIN does not change. From the user's
 * perspective the data is replaced and the unlock experience is unchanged.
 */
class BackupViewModel(
    private val ctx: Context,
    private val authMetaStore: FileAuthMetaStore,
    private val dbFile: File,
    private val appViewModel: AppViewModel,
) : ViewModel() {

    sealed interface Status {
        data object Idle : Status
        data object Working : Status
        data class Ready(val uri: Uri, val filename: String) : Status
        data class Error(val message: String) : Status
        data object RestoreComplete : Status
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val nativeLibLoaded = AtomicBoolean(false)

    fun export(primaryPin: String, backupPassword: String) {
        if (primaryPin.isEmpty() || backupPassword.isEmpty()) return
        if (_status.value is Status.Working) return
        _status.value = Status.Working

        val primaryChars = primaryPin.toCharArray()
        val backupChars = backupPassword.toCharArray()
        viewModelScope.launch {
            try {
                val outFile = withContext(Dispatchers.IO) {
                    val meta = authMetaStore.load()
                    val pin = Pin(primaryChars.copyOf())
                    val ok = KeyDerivation.validatePin(pin, meta.pinHashSalt, meta.pinHash)
                    if (!ok) {
                        pin.zero()
                        throw IllegalArgumentException("Wrong primary PIN.")
                    }
                    val srcKey = KeyDerivation.deriveKey(pin, meta.keySalt)
                    pin.zero()
                    try {
                        val ts = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        val filename = "tides-backup-$ts.tides"
                        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                        val out = File(dir, filename)
                        BackupExporter.export(ctx, dbFile, srcKey, backupChars.copyOf(), out)
                        out to filename
                    } finally {
                        srcKey.zero()
                    }
                }
                val uri = FileProvider.getUriForFile(
                    ctx, "${ctx.packageName}.fileprovider", outFile.first,
                )
                _status.value = Status.Ready(uri, outFile.second)
            } catch (e: Exception) {
                _status.value = Status.Error(e.message ?: "Backup failed.")
            } finally {
                java.util.Arrays.fill(primaryChars, 0.toChar())
                java.util.Arrays.fill(backupChars, 0.toChar())
            }
        }
    }

    fun share(uri: Uri) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun restore(primaryPin: String, backupPassword: String, source: Uri) {
        if (primaryPin.isEmpty() || backupPassword.isEmpty()) return
        if (_status.value is Status.Working) return
        _status.value = Status.Working

        val primaryChars = primaryPin.toCharArray()
        val backupChars = backupPassword.toCharArray()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { runRestore(primaryChars, backupChars, source) }
                _status.value = Status.RestoreComplete
            } catch (e: Exception) {
                _status.value = Status.Error(e.message ?: "Restore failed.")
            } finally {
                java.util.Arrays.fill(primaryChars, 0.toChar())
                java.util.Arrays.fill(backupChars, 0.toChar())
            }
        }
    }

    private suspend fun runRestore(primaryChars: CharArray, backupChars: CharArray, source: Uri) {
        if (nativeLibLoaded.compareAndSet(false, true)) {
            System.loadLibrary("sqlcipher")
        }

        // 1. Validate primary PIN and derive primary key once.
        val meta = authMetaStore.load()
        val primaryPin = Pin(primaryChars.copyOf())
        val primaryOk = KeyDerivation.validatePin(primaryPin, meta.pinHashSalt, meta.pinHash)
        if (!primaryOk) {
            primaryPin.zero()
            throw IllegalArgumentException("Wrong primary PIN.")
        }
        val primaryKey = KeyDerivation.deriveKey(primaryPin, meta.keySalt)
        primaryPin.zero()

        val workDir = File(ctx.cacheDir, "restore").apply { mkdirs() }
        val srcCopy = File(workDir, "src_${System.nanoTime()}.tides")
        val payload = File(workDir, "payload_${System.nanoTime()}.db")
        try {
            // 2. Pull SAF content into a private file we own.
            ctx.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { "Could not read backup file." }
                srcCopy.outputStream().use { input.copyTo(it) }
            }

            // 3-4. Verify container + extract payload (still encrypted under backup key).
            val backupKey = BackupImporter.import(ctx, srcCopy, backupChars.copyOf(), payload)
            try {
                // 5. Rekey payload from backup key to primary key in place.
                val backupPwd = backupKey.bytes.copyOf()
                try {
                    val db = SQLiteDatabase.openOrCreateDatabase(payload, backupPwd, null, null)
                    try {
                        db.rawExecSQL("PRAGMA rekey = \"x'${primaryKey.bytes.toHex()}'\"")
                    } finally {
                        db.close()
                    }
                } finally {
                    java.util.Arrays.fill(backupPwd, 0)
                }
            } finally {
                backupKey.zero()
            }

            // 6. Hand the rekeyed payload to AppViewModel for atomic swap.
            val ok = appViewModel.replaceDatabaseFile(payload)
            if (!ok) throw IllegalStateException("Could not replace database file.")
        } finally {
            primaryKey.zero()
            srcCopy.delete()
            // payload is consumed by replaceDatabaseFile on success; defensive delete on failure
            if (payload.exists()) payload.delete()
        }
    }

    fun clearStatus() { _status.value = Status.Idle }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
