package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

class DripImporter : ImportSource {

    override val displayName: String = "drip"

    class EncryptedBackup(msg: String) : IOException(msg)
    class MalformedJson(msg: String, cause: Throwable? = null) : IOException(msg, cause)

    companion object {
        private val PAIN_MAP: Map<String, Symptom> = mapOf(
            "cramps" to Symptom.CRAMPS,
            "headache" to Symptom.HEADACHE,
            "backache" to Symptom.BACK_PAIN,
            "breast_tenderness" to Symptom.BREAST_TENDERNESS,
            "nausea" to Symptom.NAUSEA,
        )

        private val MOOD_MAP: Map<String, Symptom> = mapOf(
            "happy" to Symptom.HAPPY,
            "sad" to Symptom.SAD,
            "anxious" to Symptom.ANXIOUS,
            "irritable" to Symptom.IRRITABLE,
            "calm" to Symptom.CALM,
            "sensitive" to Symptom.SENSITIVE,
        )

        private val BODY_MAP: Map<String, Symptom> = mapOf(
            "bloating" to Symptom.BLOATING,
            "acne" to Symptom.ACNE,
            "fatigue" to Symptom.TIRED,
            "cravings" to Symptom.CRAVINGS,
        )

        private val UNMAPPED_GROUPS = listOf("temperature", "mucus", "cervix", "sex", "desire")

        private const val MIN_YEAR = 1900
    }

    // drip bleeding intCode: 0 = "logged but no flow" -> null, 1 = SPOTTING,
    // 2 = LIGHT (drip's "medium" is closer to Tides' light per their UI scale),
    // 3 = HEAVY. drip uses a 4-step scale; we map to Tides' 5-step preserving
    // semantic ordering (drip 0 surfaces as null so the entry is symptom-only).
    private fun mapBleeding(value: Int): FlowIntensity? = when (value) {
        0 -> null
        1 -> FlowIntensity.SPOTTING
        2 -> FlowIntensity.LIGHT
        3 -> FlowIntensity.HEAVY
        else -> null
    }

    override suspend fun parse(stream: InputStream): ParseResult {
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw MalformedJson("Couldn't parse drip JSON", e)
        }

        if (root.has("@@__\$drip\$__")) {
            throw EncryptedBackup("drip backup is encrypted; export as JSON instead")
        }

        val cyclesArr = root.optJSONArray("cycles")
            ?: throw MalformedJson("Missing 'cycles' array")

        val entries = mutableListOf<ImportedEntry>()
        val unmappedCounts = mutableMapOf<String, Int>()
        val warnings = mutableListOf<String>()
        val today = LocalDate.now()

        for (i in 0 until cyclesArr.length()) {
            val day = cyclesArr.optJSONObject(i) ?: continue
            val dateStr = day.optString("date").takeIf { it.isNotEmpty() } ?: continue
            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: DateTimeParseException) {
                warnings += "Unparseable date: $dateStr"
                continue
            }
            if (date.year < MIN_YEAR || date.isAfter(today)) {
                warnings += "Date out of range, skipping: $dateStr"
                continue
            }

            val flow = day.optJSONObject("bleeding")?.takeIf { !it.optBoolean("exclude", false) }
                ?.let { mapBleeding(it.optInt("value", -1)) }

            val symptoms = mutableListOf<Symptom>()
            extractSymptomGroup(day.optJSONObject("pain"), PAIN_MAP, symptoms, unmappedCounts, "pain")
            extractSymptomGroup(day.optJSONObject("mood"), MOOD_MAP, symptoms, unmappedCounts, "mood")
            extractSymptomGroup(day.optJSONObject("body"), BODY_MAP, symptoms, unmappedCounts, "body")

            val notes = day.optJSONObject("note")?.optString("value")?.takeIf { it.isNotEmpty() }

            for (key in UNMAPPED_GROUPS) {
                if (day.has(key)) {
                    unmappedCounts[key] = (unmappedCounts[key] ?: 0) + 1
                }
            }

            entries += ImportedEntry(
                date = date,
                flow = flow,
                painSeverity = null,
                symptoms = symptoms.distinct(),
                notes = notes,
            )
        }

        val unmapped = unmappedCounts.map { (k, v) -> UnmappedField(k, v) }
            .sortedByDescending { it.sampleCount }

        return ParseResult(entries, unmapped, warnings)
    }

    private fun extractSymptomGroup(
        group: JSONObject?,
        translation: Map<String, Symptom>,
        sink: MutableList<Symptom>,
        unmappedCounts: MutableMap<String, Int>,
        groupName: String,
    ) {
        if (group == null) return
        val keys = group.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = group.optJSONObject(key) ?: continue
            if (entry.optBoolean("exclude", false)) continue
            val sym = translation[key]
            if (sym != null) {
                sink += sym
            } else {
                val tag = "$groupName.$key"
                unmappedCounts[tag] = (unmappedCounts[tag] ?: 0) + 1
            }
        }
    }
}
