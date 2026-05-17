package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate

/**
 * One day-level entry for flow intensity, optional pain (NRS 0-10),
 * and optional free-text notes. The primary key is the date — one entry
 * per day, last-write-wins.
 */
@Entity(tableName = "cycle_entries")
data class CycleEntryEntity(
    @PrimaryKey val date: LocalDate,
    val flowIntensity: FlowIntensity,
    val painSeverity: Int?,  // 0-10 NRS, nullable
    val notes: String?,
)
