package com.hayate0726.tides.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CyclesDbFile

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PreImportSnapshotFile

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @CyclesDbFile
    fun provideCyclesDbFile(@ApplicationContext ctx: Context): File =
        File(ctx.filesDir, "tides.db")

    @Provides
    @Singleton
    @PreImportSnapshotFile
    fun providePreImportSnapshotFile(@CyclesDbFile dbFile: File): File =
        File(dbFile.parentFile, dbFile.name + ".pre-import")
}
