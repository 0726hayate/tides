package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simple key/value table for app settings. Keys are strings to keep migrations
 * trivial (adding a new setting is a write, not a schema change).
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
)
