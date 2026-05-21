package com.hayate0726.tides.data.`import`

data class UnmappedField(
    val sourceName: String,
    val sampleCount: Int,
)

data class ParseResult(
    val entries: List<ImportedEntry>,
    val unmapped: List<UnmappedField>,
    val warnings: List<String>,
)
