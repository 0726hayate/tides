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

    fun detect(bytes: ByteArray): Format {
        if (bytes.isEmpty()) return Format.UNKNOWN

        if (bytes.size >= 4 && bytes.sliceArray(0..3).contentEquals(PDF_MAGIC)) {
            return Format.PDF
        }

        val sample = String(bytes, 0, minOf(bytes.size, 4096), Charsets.UTF_8)
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
