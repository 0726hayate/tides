package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hayate0726.tides.data.entity.GoalEntity
import com.hayate0726.tides.domain.model.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE goal = :goal")
    suspend fun delete(goal: Goal)

    @Query("DELETE FROM goals")
    suspend fun clearAll()

    @Query("SELECT goal FROM goals")
    suspend fun all(): List<Goal>

    @Query("SELECT goal FROM goals")
    fun observeAll(): Flow<List<Goal>>
}
