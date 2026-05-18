package com.hayate0726.tides.ui.nav

import com.hayate0726.tides.crypto.BiometricKeyStore
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.di.CyclesDbFile
import com.hayate0726.tides.notifications.NotificationPreferences
import com.hayate0726.tides.notifications.ReminderScheduler
import com.hayate0726.tides.widget.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File

/**
 * Hilt entry point read once at MainHost composition. The Main-route VMs
 * are constructed manually (DB key is opened post-onboarding at runtime)
 * so we can't use [@HiltViewModel] for all of them — but they still need
 * the singletons. This avoids re-injecting the same deps through several
 * factories.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MainGraphEntryPoint {
    fun authMetaStore(): FileAuthMetaStore
    @CyclesDbFile fun cyclesDbFile(): File
    fun biometricKeyStore(): BiometricKeyStore
    fun notificationPreferences(): NotificationPreferences
    fun reminderScheduler(): ReminderScheduler
    fun widgetUpdater(): WidgetUpdater
    fun userPrivacyRepository(): com.hayate0726.tides.data.UserPrivacyRepository
}
