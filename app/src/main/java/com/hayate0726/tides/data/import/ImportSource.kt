package com.hayate0726.tides.data.`import`

import java.io.InputStream

interface ImportSource {
    val displayName: String
    suspend fun parse(stream: InputStream): ParseResult
}
