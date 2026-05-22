package com.hayate0726.tides.ui.dataimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.`import`.ClueImporter
import com.hayate0726.tides.data.`import`.DripImporter
import com.hayate0726.tides.data.`import`.ImportFormatDetector
import com.hayate0726.tides.data.`import`.ImportPipeline
import com.hayate0726.tides.data.`import`.ImportResult
import com.hayate0726.tides.data.`import`.ImportSource
import com.hayate0726.tides.data.`import`.ParseResult
import com.hayate0726.tides.data.`import`.PreImportSnapshot
import com.hayate0726.tides.data.`import`.PreviewState
import com.hayate0726.tides.data.`import`.SamsungHealthHtmlImporter
import com.hayate0726.tides.di.CyclesDbFile
import com.hayate0726.tides.di.PreImportSnapshotFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    @CyclesDbFile private val dbFile: File,
    @PreImportSnapshotFile private val snapshotFile: File,
) : ViewModel() {

    sealed interface Status {
        data object Idle : Status
        data object Parsing : Status
        data class PreviewReady(val preview: PreviewState) : Status
        data class Committing(val preview: PreviewState) : Status
        data class Complete(val result: ImportResult) : Status
        data class Error(val message: String) : Status
    }

    companion object {
        const val MAX_FILE_BYTES = 50L * 1024 * 1024 // 50 MB
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    private var pendingParse: ParseResult? = null

    fun pickedFile(uri: Uri, db: TidesDatabase) {
        _status.value = Status.Parsing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val size = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                if (size > MAX_FILE_BYTES) {
                    _status.value = Status.Error("File is unusually large. Make sure it's a period export, not a full data dump.")
                    return@launch
                }

                val format = ctx.contentResolver.openInputStream(uri).use { stream ->
                    if (stream == null) return@use ImportFormatDetector.Format.UNKNOWN
                    val head = ByteArray(4096)
                    val n = stream.read(head)
                    ImportFormatDetector.detect(head.copyOf(n.coerceAtLeast(0)))
                }

                val source: ImportSource = when (format) {
                    ImportFormatDetector.Format.SAMSUNG_HTML -> SamsungHealthHtmlImporter()
                    ImportFormatDetector.Format.CLUE_CSV -> ClueImporter()
                    ImportFormatDetector.Format.DRIP_JSON -> DripImporter()
                    ImportFormatDetector.Format.PDF -> {
                        _status.value = Status.Error("PDF isn't supported. Re-export and choose HTML, CSV, or JSON.")
                        return@launch
                    }
                    ImportFormatDetector.Format.UNKNOWN -> {
                        _status.value = Status.Error("Couldn't recognize this file. Make sure it's from Samsung Health, Clue, or drip.")
                        return@launch
                    }
                }

                val parse = ctx.contentResolver.openInputStream(uri).use { stream ->
                    if (stream == null) {
                        _status.value = Status.Error("Couldn't read the file.")
                        return@launch
                    }
                    source.parse(stream)
                }
                pendingParse = parse
                val preview = ImportPipeline().computePreview(source, parse, db)
                _status.value = Status.PreviewReady(preview)
            } catch (e: Exception) {
                _status.value = Status.Error(friendlyMessage(e))
            }
        }
    }

    fun confirmImport(db: TidesDatabase, onComplete: () -> Unit) {
        val parse = pendingParse ?: return
        val preview = (_status.value as? Status.PreviewReady)?.preview ?: return
        _status.value = Status.Committing(preview)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snap = PreImportSnapshot(dbFile, snapshotFile)
                snap.take()
                val result = ImportPipeline().commit(parse, db)
                _status.value = Status.Complete(result)
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: PreImportSnapshot.InsufficientSpace) {
                _status.value = Status.Error("Not enough space to save a backup before importing. Free up some space and try again.")
            } catch (e: Exception) {
                _status.value = Status.Error("Import failed. No changes were made.")
            }
        }
    }

    fun reset() {
        pendingParse = null
        _status.value = Status.Idle
    }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is DripImporter.EncryptedBackup -> "This file is encrypted. Tides can only read unencrypted drip exports — choose 'Export → JSON' instead of 'Backup'."
        is ClueImporter.MalformedCsv -> "File appears damaged. Try re-exporting from the source app."
        is DripImporter.MalformedJson -> "File appears damaged. Try re-exporting from the source app."
        else -> e.message ?: "Couldn't read the file."
    }
}
