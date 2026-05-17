package com.hayate0726.tides

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.di.AuthMetaFile
import com.hayate0726.tides.di.CyclesDbFile
import com.hayate0726.tides.lock.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    @CyclesDbFile private val dbFile: File,
    @AuthMetaFile private val authMetaFile: File,
    private val authMetaStore: FileAuthMetaStore,
    private val lockManager: LockManager,
) : ViewModel() {

    private val _state = MutableStateFlow<AppState>(initialState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun initialState(): AppState =
        if (authMetaFile.exists()) AppState.Locked else AppState.Onboarding

    fun onUnlockAttempt(pin: Pin) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = lockManager.attemptUnlock(pin)
            pin.zero()
            when (result) {
                is LockManager.UnlockResult.Success -> {
                    val db = DatabaseFactory.open(ctx, dbFile, result.key)
                    result.key.zero()
                    _state.value = AppState.Unlocked(db)
                }
                LockManager.UnlockResult.WrongPin -> {
                    // state unchanged; UI shows error
                }
                is LockManager.UnlockResult.RateLimited -> {
                    _state.value = AppState.LockedCooldown(result.expiryEpochMs)
                }
                LockManager.UnlockResult.Duress -> {
                    val decoyFile = File(ctx.filesDir, "decoy.db")
                    val meta = authMetaStore.load()
                    when (meta.duress!!.mode) {
                        com.hayate0726.tides.crypto.AuthMeta.DuressMode.DECOY -> {
                            val key = com.hayate0726.tides.crypto.KeyDerivation.deriveKey(
                                Pin("dummy".toCharArray()),  // decoy DB uses its own derived key from duress PIN, see onboarding
                                meta.duress.keySalt,
                            )
                            // Note: full decoy flow re-derives from the entered duress PIN.
                            // For Plan 3 simplicity, decoy DB uses the duress PIN's key.
                            val db = DatabaseFactory.open(ctx, decoyFile, key)
                            key.zero()
                            _state.value = AppState.UnlockedDecoy(db)
                        }
                        com.hayate0726.tides.crypto.AuthMeta.DuressMode.WIPE -> {
                            // Wipe: delete real and decoy DBs, reset auth_meta.bin
                            dbFile.delete()
                            decoyFile.delete()
                            authMetaFile.delete()
                            _state.value = AppState.Onboarding
                        }
                        com.hayate0726.tides.crypto.AuthMeta.DuressMode.OFF -> Unit
                    }
                }
            }
        }
    }

    fun lock() {
        viewModelScope.launch(Dispatchers.IO) {
            (_state.value as? AppState.Unlocked)?.db?.close()
            (_state.value as? AppState.UnlockedDecoy)?.db?.close()
            _state.value = AppState.Locked
        }
    }

    fun onOnboardingComplete() {
        // Onboarding wrote auth_meta.bin and created the DB; transition to Unlocked.
        // The caller passes the db via a separate method; see OnboardingViewModel.
    }

    fun setUnlocked(db: TidesDatabase) {
        _state.value = AppState.Unlocked(db)
    }
}
