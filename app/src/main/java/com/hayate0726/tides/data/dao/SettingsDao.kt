package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hayate0726.tides.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Upsert
    suspend fun upsert(row: SettingsEntity)

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)
}
