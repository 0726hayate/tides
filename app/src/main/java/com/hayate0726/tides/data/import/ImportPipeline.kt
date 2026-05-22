package com.hayate0726.tides.data.`import`

import androidx.room.withTransaction
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate

data class PreviewState(
    val sourceName: String,
    val totalEntries: Int,
    val newDates: Int,
    val conflictingDates: Int,
    val unmapped: List<UnmappedField>,
    val warnings: List<String>,
)

data class ImportResult(
    val addedDates: Int,
    val skippedDates: Int,
)

class ImportPipeline {

    suspend fun computePreview(
        source: ImportSource,
        parseResult: ParseResult,
        db: TidesDatabase,
    ): PreviewState {
        val existingDates: Set<LocalDate> =
            (db.cycleEntryDao().allDates() + db.symptomEntryDao().allDates()).toSet()
        val parsedDates = parseResult.entries.map { it.date }.distinct()
        val conflicting = parsedDates.count { it in existingDates }
        return PreviewState(
            sourceName = source.displayName,
            totalEntries = parseResult.entries.size,
            newDates = parsedDates.size - conflicting,
            conflictingDates = conflicting,
            unmapped = parseResult.unmapped,
            warnings = parseResult.warnings,
        )
    }

    /**
     * Commits non-conflicting entries inside a single Room transaction. A date
     * already present in either cycle_entries or symptom_entries is skipped
     * entirely (both the CycleEntry and any symptoms for that date), making
     * re-imports idempotent.
     *
     * Precondition: `parseResult.entries` must not contain more than one
     * `ImportedEntry` per date. Each parser today emits exactly one row per
     * date; if a future caller composes multiple sources they must deduplicate
     * first. Violating this writes duplicate symptom rows for the second entry
     * and reports an `addedDates` count lower than the actual write count.
     */
    suspend fun commit(
        parseResult: ParseResult,
        db: TidesDatabase,
    ): ImportResult {
        val existingDates: Set<LocalDate> =
            (db.cycleEntryDao().allDates() + db.symptomEntryDao().allDates()).toSet()

        var added = 0
        var skipped = 0
        val skippedDates = mutableSetOf<LocalDate>()
        val addedDates = mutableSetOf<LocalDate>()

        db.withTransaction {
            for (entry in parseResult.entries) {
                if (entry.date in existingDates) {
                    if (skippedDates.add(entry.date)) skipped++
                    continue
                }
                val hasCycleData =
                    entry.flow != null || entry.painSeverity != null || !entry.notes.isNullOrEmpty()
                if (hasCycleData) {
                    db.cycleEntryDao().upsert(
                        CycleEntryEntity(
                            date = entry.date,
                            flowIntensity = entry.flow ?: FlowIntensity.NONE,
                            painSeverity = entry.painSeverity,
                            notes = entry.notes,
                        )
                    )
                }
                for (sym in entry.symptoms) {
                    db.symptomEntryDao().insert(
                        SymptomEntryEntity(
                            date = entry.date,
                            symptom = sym,
                            severity = 1,
                            otherText = null,
                        )
                    )
                }
                if (hasCycleData || entry.symptoms.isNotEmpty()) {
                    if (addedDates.add(entry.date)) added++
                }
            }
        }
        return ImportResult(added, skipped)
    }
}
