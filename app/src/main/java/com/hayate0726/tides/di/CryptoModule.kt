package com.hayate0726.tides.di

import android.content.Context
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.lock.LockManager
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
annotation class AuthMetaFile

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    @AuthMetaFile
    fun provideAuthMetaFile(@ApplicationContext ctx: Context): File =
        File(ctx.filesDir, "auth_meta.bin")

    @Provides
    @Singleton
    fun provideAuthMetaStore(@AuthMetaFile file: File): FileAuthMetaStore =
        FileAuthMetaStore(file)

    @Provides
    @Singleton
    fun provideClock(): LockManager.Clock = LockManager.Clock { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun provideLockManager(
        store: FileAuthMetaStore,
        clock: LockManager.Clock,
    ): LockManager = LockManager(store, clock)

    @Provides
    @Singleton
    fun provideUserPrivacyRepository(): com.hayate0726.tides.data.UserPrivacyRepository =
        com.hayate0726.tides.data.UserPrivacyRepository()
}
