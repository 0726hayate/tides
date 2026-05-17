package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hayate0726.tides.data.entity.CycleEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CycleEntryDao {

    @Upsert
    suspend fun upsert(entry: CycleEntryEntity)

    @Query("DELETE FROM cycle_entries WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM cycle_entries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): CycleEntryEntity?

    @Query("SELECT * FROM cycle_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun rangeOnce(from: LocalDate, to: LocalDate): List<CycleEntryEntity>

    @Query("SELECT * FROM cycle_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun rangeFlow(from: LocalDate, to: LocalDate): Flow<List<CycleEntryEntity>>

    @Query("SELECT * FROM cycle_entries ORDER BY date ASC")
    suspend fun all(): List<CycleEntryEntity>
}
