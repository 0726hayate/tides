package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

class ClueImporter : ImportSource {

    override val displayName: String = "Clue"

    class MalformedCsv(msg: String) : IOException(msg)

    companion object {
        private val SYMPTOM_MAP: Map<String, Symptom> = mapOf(
            "cramps" to Symptom.CRAMPS,
            "headache" to Symptom.HEADACHE,
            "migraine" to Symptom.MIGRAINE,
            "back_pain" to Symptom.BACK_PAIN,
            "breast_tenderness" to Symptom.BREAST_TENDERNESS,
            "bloating" to Symptom.BLOATING,
            "acne" to Symptom.ACNE,
            "fatigue" to Symptom.TIRED,
            "cravings" to Symptom.CRAVINGS,
            "nausea" to Symptom.NAUSEA,
            "happy" to Symptom.HAPPY,
            "sad" to Symptom.SAD,
            "sensitive" to Symptom.SENSITIVE,
            "anxious" to Symptom.ANXIOUS,
            "irritable" to Symptom.IRRITABLE,
            "calm" to Symptom.CALM,
            "energetic" to Symptom.ENERGETIC,
        )

        private val RESERVED_COLUMNS = setOf("day", "date", "period", "flow", "note")
        private const val MIN_YEAR = 1900
    }

    private fun mapFlow(raw: String): FlowIntensity? = when (raw.trim().lowercase()) {
        "spotting" -> FlowIntensity.SPOTTING
        "light" -> FlowIntensity.LIGHT
        "medium" -> FlowIntensity.MEDIUM
        "heavy" -> FlowIntensity.HEAVY
        "" -> null
        else -> null
    }

    override suspend fun parse(stream: InputStream): ParseResult {
        val lines = stream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) throw MalformedCsv("CSV is empty")

        val header = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        if ("day" !in header && "date" !in header) {
            throw MalformedCsv("CSV header missing 'day' or 'date' column")
        }

        val dayIdx = header.indexOf("day").takeIf { it >= 0 } ?: header.indexOf("date")
        val flowIdx = header.indexOf("flow")
        val noteIdx = header.indexOf("note")

        val symptomColumns: Map<Int, Symptom> = header.withIndex()
            .mapNotNull { (i, name) -> SYMPTOM_MAP[name]?.let { i to it } }
            .toMap()

        val unmappedColumns: List<Pair<Int, String>> = header.withIndex()
            .filter { (i, name) ->
                i !in symptomColumns &&
                    name !in RESERVED_COLUMNS &&
                    name.isNotEmpty()
            }
            .map { (i, name) -> i to name }

        val entries = mutableListOf<ImportedEntry>()
        val unmappedCounts = mutableMapOf<String, Int>()
        val warnings = mutableListOf<String>()
        val today = LocalDate.now()

        for ((lineIdx, line) in lines.drop(1).withIndex()) {
            val fields = parseCsvLine(line)
            if (fields.size <= dayIdx) continue

            val dateStr = fields[dayIdx].trim()
            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: DateTimeParseException) {
                warnings += "Unparseable date on row ${lineIdx + 2}: $dateStr"
                continue
            }
            if (date.year < MIN_YEAR || date.isAfter(today)) {
                warnings += "Date out of range on row ${lineIdx + 2}: $dateStr"
                continue
            }

            val flow = if (flowIdx >= 0 && fields.size > flowIdx) mapFlow(fields[flowIdx]) else null

            val symptoms = symptomColumns
                .filter { (i, _) -> i < fields.size && fields[i].trim().isNotEmpty() }
                .values
                .toList()

            for ((i, name) in unmappedColumns) {
                if (i < fields.size && fields[i].trim().isNotEmpty()) {
                    unmappedCounts[name] = (unmappedCounts[name] ?: 0) + 1
                }
            }

            val notes = if (noteIdx >= 0 && fields.size > noteIdx) {
                fields[noteIdx].takeIf { it.isNotEmpty() }
            } else null

            entries += ImportedEntry(
                date = date,
                flow = flow,
                painSeverity = null,
                symptoms = symptoms,
                notes = notes,
            )
        }

        val unmapped = unmappedCounts.map { (k, v) -> UnmappedField(k, v) }
            .sortedByDescending { it.sampleCount }

        return ParseResult(entries, unmapped, warnings)
    }

    // Minimal CSV parser: handles double-quoted fields with embedded commas
    // and "" escapes. Does not handle newlines inside quotes (Clue's export
    // never wraps a note across lines).
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"'); i += 2
                }
                c == '"' -> {
                    inQuotes = !inQuotes; i++
                }
                c == ',' && !inQuotes -> {
                    out += cur.toString(); cur.setLength(0); i++
                }
                else -> {
                    cur.append(c); i++
                }
            }
        }
        out += cur.toString()
        return out
    }
}
