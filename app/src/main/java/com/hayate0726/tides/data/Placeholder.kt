package com.hayate0726.tides.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "placeholder")
data class Placeholder(
    @PrimaryKey val id: Int,
    val payload: String,
)

@Dao
interface PlaceholderDao {
    @Insert
    suspend fun insert(row: Placeholder)

    @Query("SELECT * FROM placeholder")
    suspend fun all(): List<Placeholder>
}
