package com.hayate0726.tides.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Wraps file-share via `Intent.ACTION_SEND` so the user picks the destination
 * via the system share sheet. We expose a single function per content type.
 *
 * Files are written under `ctx.cacheDir/exports/`. They are temporary —
 * Android cleans this directory periodically. Add a FileProvider for the
 * `<authorities>${applicationId}.fileprovider</authorities>` in the manifest.
 */
object Sharer {

    fun sharePdf(ctx: Context, bytes: ByteArray, displayName: String = "tides_export.pdf") {
        share(ctx, bytes, displayName, mime = "application/pdf")
    }

    fun shareCsv(ctx: Context, csv: String, displayName: String = "tides_export.csv") {
        share(ctx, csv.toByteArray(Charsets.UTF_8), displayName, mime = "text/csv")
    }

    private fun share(ctx: Context, bytes: ByteArray, displayName: String, mime: String) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, displayName)
        file.writeBytes(bytes)

        val uri: Uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            file,
        )

        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
