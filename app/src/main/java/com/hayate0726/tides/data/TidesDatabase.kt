package com.hayate0726.tides.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Plan 1 placeholder schema — proves the SQLCipher integration round-trips.
 * Plan 2 replaces the entity set with the real domain schema.
 *
 * exportSchema is disabled here because the placeholder schema is throwaway;
 * Plan 2 enables schema export with a dedicated schemas directory once the
 * real entities land.
 */
@Database(
    entities = [Placeholder::class],
    version = 1,
    exportSchema = false,
)
abstract class TidesDatabase : RoomDatabase() {
    abstract fun placeholderDao(): PlaceholderDao
}
