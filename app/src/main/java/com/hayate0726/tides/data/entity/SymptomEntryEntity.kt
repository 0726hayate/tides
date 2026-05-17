package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

/**
 * One logged symptom for a date. A user can log many symptoms per day,
 * so primary key is autoIncrement int. The (date, symptom) pair is indexed
 * for efficient frequency queries.
 *
 * `severity` is 0..2 (mild/moderate/severe).
 * `otherText` is null unless `symptom == OTHER`.
 */
@Entity(
    tableName = "symptom_entries",
    indices = [Index(value = ["date", "symptom"])],
)
data class SymptomEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val symptom: Symptom,
    val severity: Int,
    val otherText: String?,
)
