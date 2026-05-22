package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SymptomEntryDao {

    @Insert
    suspend fun insert(entry: SymptomEntryEntity): Long

    @Query("DELETE FROM symptom_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM symptom_entries WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM symptom_entries WHERE date = :date ORDER BY symptom ASC")
    suspend fun getByDate(date: LocalDate): List<SymptomEntryEntity>

    @Query("SELECT * FROM symptom_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun rangeOnce(from: LocalDate, to: LocalDate): List<SymptomEntryEntity>

    @Query("SELECT * FROM symptom_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun rangeFlow(from: LocalDate, to: LocalDate): Flow<List<SymptomEntryEntity>>

    @Query("SELECT * FROM symptom_entries ORDER BY date ASC")
    suspend fun all(): List<SymptomEntryEntity>

    @Query("SELECT DISTINCT date FROM symptom_entries")
    suspend fun allDates(): List<LocalDate>
}
