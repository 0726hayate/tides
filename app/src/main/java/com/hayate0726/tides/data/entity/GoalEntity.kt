package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.Goal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val goal: Goal,
)
