package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.BirthControlMethod
import java.time.LocalDate

/**
 * The user's birth control method over time. Each entry is a span:
 * `startDate` (inclusive) .. `endDate` (exclusive). The active row has
 * `endDate == null`.
 */
@Entity(tableName = "birth_control")
data class BirthControlEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: BirthControlMethod,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)
