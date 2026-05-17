package com.hayate0726.tides.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hayate0726.tides.data.dao.BirthControlDao
import com.hayate0726.tides.data.dao.CycleEntryDao
import com.hayate0726.tides.data.dao.GoalDao
import com.hayate0726.tides.data.dao.SettingsDao
import com.hayate0726.tides.data.dao.SymptomEntryDao
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.GoalEntity
import com.hayate0726.tides.data.entity.SettingsEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity

@Database(
    entities = [
        CycleEntryEntity::class,
        SymptomEntryEntity::class,
        BirthControlEntity::class,
        SettingsEntity::class,
        GoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TidesDatabase : RoomDatabase() {
    abstract fun cycleEntryDao(): CycleEntryDao
    abstract fun symptomEntryDao(): SymptomEntryDao
    abstract fun birthControlDao(): BirthControlDao
    abstract fun settingsDao(): SettingsDao
    abstract fun goalDao(): GoalDao
}
