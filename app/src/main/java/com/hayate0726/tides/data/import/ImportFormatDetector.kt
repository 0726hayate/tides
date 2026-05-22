package com.hayate0726.tides.data.`import`

object ImportFormatDetector {

    enum class Format {
        SAMSUNG_HTML,
        CLUE_CSV,
        DRIP_JSON,
        PDF,
        UNKNOWN,
    }

    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46)
    // Samsung Health HTML exports start with ~4 KB of inline CSS and a base64-
    // encoded logo image before the "Samsung Health" marker text appears.
    // 16 KB is a safe sniff window for real exports while still bounded enough
    // to not waste I/O on huge files.
    private const val SNIFF_BYTES = 16384

    fun detect(bytes: ByteArray): Format {
        if (bytes.isEmpty()) return Format.UNKNOWN

        if (bytes.size >= 4 && bytes.copyOfRange(0, 4).contentEquals(PDF_MAGIC)) {
            return Format.PDF
        }

        // 4 KB cut on a byte boundary is safe: all sniff tokens are ASCII.
        val sample = String(bytes, 0, minOf(bytes.size, SNIFF_BYTES), Charsets.UTF_8)
        val lower = sample.lowercase()

        if (lower.contains("<!doctype html") && lower.contains("samsung health")) {
            return Format.SAMSUNG_HTML
        }

        val trimmed = sample.trimStart()
        if (trimmed.startsWith("{") && (lower.contains("\"cycles\"") || lower.contains("\"drip\""))) {
            return Format.DRIP_JSON
        }

        val firstLine = sample.lineSequence().firstOrNull { it.isNotBlank() }?.lowercase().orEmpty()
        if (firstLine.contains(",") &&
            (firstLine.contains("day") || firstLine.contains("date")) &&
            (firstLine.contains("period") || firstLine.contains("flow"))
        ) {
            return Format.CLUE_CSV
        }

        return Format.UNKNOWN
    }
}
