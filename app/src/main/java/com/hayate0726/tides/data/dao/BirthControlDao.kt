package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hayate0726.tides.data.entity.BirthControlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthControlDao {

    @Insert
    suspend fun insert(row: BirthControlEntity): Long

    @Update
    suspend fun update(row: BirthControlEntity)

    @Query("SELECT * FROM birth_control WHERE endDate IS NULL LIMIT 1")
    suspend fun activeOnce(): BirthControlEntity?

    @Query("SELECT * FROM birth_control WHERE endDate IS NULL LIMIT 1")
    fun activeFlow(): Flow<BirthControlEntity?>

    @Query("SELECT * FROM birth_control ORDER BY startDate ASC")
    suspend fun all(): List<BirthControlEntity>
}
