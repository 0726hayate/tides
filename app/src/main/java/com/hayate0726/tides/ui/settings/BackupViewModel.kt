package com.hayate0726.tides.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.BackupExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Drives backup export on top of [BackupExporter].
 *
 * The live DB key is zeroed after unlock (Plan 1 design — no long-lived
 * key in memory). To rekey the DB into the backup container we re-derive
 * the source key from the user's primary PIN against the live AuthMeta
 * salt — the key exists only for the duration of the export and is zeroed
 * immediately after.
 *
 * If the user enters the wrong primary PIN we surface a clean error
 * before [BackupExporter.export] is called, so we never produce a
 * partially-rekeyed file.
 *
 * Import is not wired in v1.1 — see [importNotYetWired]. It requires
 * additional UX work (replace tides.db atomically, force re-unlock,
 * confirm "this overwrites everything") that's tracked as a v1.2 item.
 */
class BackupViewModel(
    private val ctx: Context,
    private val authMetaStore: FileAuthMetaStore,
    private val dbFile: File,
) : ViewModel() {

    sealed interface Status {
        data object Idle : Status
        data object Working : Status
        data class Ready(val uri: Uri, val filename: String) : Status
        data class Error(val message: String) : Status
        data object NotWired : Status
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

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

    fun importNotYetWired() {
        _status.value = Status.NotWired
    }

    fun clearStatus() { _status.value = Status.Idle }
}
