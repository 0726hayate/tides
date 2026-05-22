package com.hayate0726.tides

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.`import`.PreImportSnapshot
import com.hayate0726.tides.di.AuthMetaFile
import com.hayate0726.tides.di.CyclesDbFile
import com.hayate0726.tides.di.PreImportSnapshotFile
import com.hayate0726.tides.lock.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    @CyclesDbFile private val dbFile: File,
    @AuthMetaFile private val authMetaFile: File,
    @PreImportSnapshotFile private val snapshotFile: File,
    private val authMetaStore: FileAuthMetaStore,
    private val lockManager: LockManager,
    private val biometricKeyStore: com.hayate0726.tides.crypto.BiometricKeyStore,
) : ViewModel() {

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    /**
     * Transient unlock error surfaced to the LockScreen. Cleared by the UI
     * after display (call [clearUnlockError]) or implicitly when the user
     * starts a new attempt.
     */
    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError: StateFlow<String?> = _unlockError.asStateFlow()

    fun clearUnlockError() { _unlockError.update { null } }

    private val _hasPreImportSnapshot = MutableStateFlow(false)
    val hasPreImportSnapshotFlow: StateFlow<Boolean> = _hasPreImportSnapshot.asStateFlow()

    fun refreshPreImportSnapshotState() {
        viewModelScope.launch(Dispatchers.IO) {
            _hasPreImportSnapshot.value = PreImportSnapshot(dbFile, snapshotFile).exists()
        }
    }

    /**
     * Restores the pre-import snapshot over the live DB. Closes the live database
     * first (transitions to Locked), then swaps the file. User re-unlocks with
     * their PIN. Returns true on success.
     */
    suspend fun rollbackLastImport(): Boolean {
        return stateMutex.withLock {
            val snap = PreImportSnapshot(dbFile, snapshotFile)
            if (!snap.exists()) return@withLock false
            val current = _state.value
            if (current is AppState.Unlocked) {
                current.db.close()
                _state.value = AppState.Locked
            }
            val ok = snap.restore()
            _hasPreImportSnapshot.value = false
            ok
        }
    }

    /**
     * Serializes state transitions. Without this, lock() and onUnlockAttempt()
     * can race — a concurrent open-then-close can leak a DB or close one
     * that another collector is still reading from.
     */
    private val stateMutex = Mutex()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            stateMutex.withLock {
                if (_state.value is AppState.Loading) {
                    _state.value = if (authMetaFile.exists()) AppState.Locked else AppState.Onboarding
                }
            }
        }
    }

    fun onUnlockAttempt(pin: Pin) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clone the PIN chars so we can keep using them in the duress branch
            // after lockManager.attemptUnlock has had its (zeroed) copy.
            val pinChars = pin.chars.copyOf()
            val result = lockManager.attemptUnlock(pin)
            pin.zero()
            try {
                when (result) {
                    is LockManager.UnlockResult.Success -> openAndUnlock(result.key)
                    LockManager.UnlockResult.WrongPin -> _unlockError.update { "Wrong PIN" }
                    is LockManager.UnlockResult.RateLimited ->
                        stateMutex.withLock {
                            _state.value = AppState.LockedCooldown(result.expiryEpochMs)
                        }
                    LockManager.UnlockResult.Duress -> handleDuress(pinChars)
                }
            } finally {
                java.util.Arrays.fill(pinChars, 0.toChar())
            }
        }
    }

    /**
     * Attempt unlock using the biometric-wrapped DB key. Called from LockHost
     * after BiometricPrompt.AuthenticationCallback.onAuthenticationSucceeded.
     * Bypasses LockManager — BiometricPrompt has its own lockout.
     */
    fun onBiometricSuccess() {
        viewModelScope.launch(Dispatchers.IO) {
            val key = try {
                biometricKeyStore.unwrap()
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                biometricKeyStore.clear()
                _unlockError.update {
                    "Biometric unlock was disabled because your fingerprints changed. " +
                        "Re-enable in Settings."
                }
                return@launch
            } catch (e: android.security.keystore.UserNotAuthenticatedException) {
                // Should not happen post-fix: setUserAuthenticationValidityDuration
                // means the immediately-prior BiometricPrompt success authorized
                // this cipher op. If we hit this, our validity window is too short
                // or the auth state didn't propagate. Surface generically — the
                // PIN path remains available.
                _unlockError.update { "Biometric unlock failed. Try your PIN." }
                return@launch
            } catch (e: Exception) {
                _unlockError.update { "Biometric unlock failed. Try your PIN." }
                return@launch
            }
            openAndUnlock(key)
        }
    }

    /** True if biometric.bin exists. UI uses this to decide whether to show the prompt. */
    fun isBiometricEnrolled(): Boolean = biometricKeyStore.isEnrolled()

    private suspend fun openAndUnlock(key: com.hayate0726.tides.crypto.DbKey) {
        val db = try {
            DatabaseFactory.open(ctx, dbFile, key)
        } catch (e: Exception) {
            _unlockError.update { "Could not open database. Try again or wipe and re-onboard." }
            key.zero()
            return
        }
        key.zero()
        replaceUnlockedState(AppState.Unlocked(db))
    }

    private suspend fun handleDuress(pinChars: CharArray) {
        val meta = authMetaStore.load()
        val duress = meta.duress ?: return
        when (duress.mode) {
            AuthMeta.DuressMode.DECOY -> {
                val decoyFile = File(ctx.filesDir, "decoy.db")
                val duressPin = Pin(pinChars.copyOf())
                val key = KeyDerivation.deriveKey(duressPin, duress.keySalt)
                duressPin.zero()
                val db = try {
                    DatabaseFactory.open(ctx, decoyFile, key)
                } catch (e: Exception) {
                    _unlockError.update { "Could not open decoy database." }
                    key.zero()
                    return
                }
                key.zero()
                replaceUnlockedState(AppState.UnlockedDecoy(db))
            }
            AuthMeta.DuressMode.WIPE -> {
                val decoyFile = File(ctx.filesDir, "decoy.db")
                stateMutex.withLock {
                    closeCurrentDb()
                    dbFile.delete()
                    decoyFile.delete()
                    authMetaFile.delete()
                    biometricKeyStore.clear()
                    com.hayate0726.tides.widget.WidgetSummary.delete(ctx)
                    // Scheduled period reminders would fire post-wipe and
                    // betray prior app use to a duress attacker. Resolve
                    // ReminderScheduler lazily via the entry point rather
                    // than holding a constructor reference — keeps it off
                    // the hot AppViewModel construction path.
                    dagger.hilt.android.EntryPointAccessors
                        .fromApplication(
                            ctx.applicationContext,
                            com.hayate0726.tides.ui.nav.MainGraphEntryPoint::class.java,
                        )
                        .reminderScheduler()
                        .cancelAll()
                    _state.value = AppState.Onboarding
                }
            }
            AuthMeta.DuressMode.OFF -> Unit
        }
    }

    fun lock() {
        viewModelScope.launch(Dispatchers.IO) {
            stateMutex.withLock {
                closeCurrentDb()
                _state.value = AppState.Locked
            }
        }
    }

    /**
     * Atomically replace the live database file with [newDbFile], close any
     * currently-open DB handle, and force the user back through the lock
     * screen. Called from [com.hayate0726.tides.ui.settings.BackupViewModel]
     * after a backup has been verified and rekeyed to the active primary key.
     *
     * The mutex serializes against any concurrent unlock attempt — without
     * it, an in-flight openAndUnlock() could open the half-moved file or the
     * old one and leak that handle.
     *
     * Returns true on success. On move failure, the existing DB is left
     * untouched and the user remains unlocked.
     */
    suspend fun replaceDatabaseFile(newDbFile: File): Boolean {
        return stateMutex.withLock {
            closeCurrentDb()
            try {
                java.nio.file.Files.move(
                    newDbFile.toPath(),
                    dbFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                // Fall back to a non-atomic copy + delete on filesystems where
                // ATOMIC_MOVE isn't supported (rare on Android internal storage).
                java.nio.file.Files.copy(
                    newDbFile.toPath(),
                    dbFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                )
                newDbFile.delete()
            } catch (e: Exception) {
                return@withLock false
            }
            _state.value = AppState.Locked
            true
        }
    }

    /** Called by OnboardingViewModel once auth_meta.bin and the DB exist. */
    fun setUnlocked(db: TidesDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            replaceUnlockedState(AppState.Unlocked(db))
        }
    }

    private suspend fun replaceUnlockedState(newState: AppState) {
        stateMutex.withLock {
            closeCurrentDb()
            _state.value = newState
        }
    }

    private fun closeCurrentDb() {
        (_state.value as? AppState.Unlocked)?.db?.close()
        (_state.value as? AppState.UnlockedDecoy)?.db?.close()
    }
}
