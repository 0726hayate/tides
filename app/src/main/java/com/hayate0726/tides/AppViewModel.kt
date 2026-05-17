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
import com.hayate0726.tides.di.AuthMetaFile
import com.hayate0726.tides.di.CyclesDbFile
import com.hayate0726.tides.lock.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = if (authMetaFile.exists()) AppState.Locked else AppState.Onboarding
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
                    is LockManager.UnlockResult.Success -> {
                        val db = DatabaseFactory.open(ctx, dbFile, result.key)
                        result.key.zero()
                        _state.update { AppState.Unlocked(db) }
                    }
                    LockManager.UnlockResult.WrongPin -> {
                        _unlockError.update { "Wrong PIN" }
                    }
                    is LockManager.UnlockResult.RateLimited -> {
                        _state.update { AppState.LockedCooldown(result.expiryEpochMs) }
                    }
                    LockManager.UnlockResult.Duress -> handleDuress(pinChars)
                }
            } finally {
                java.util.Arrays.fill(pinChars, 0.toChar())
            }
        }
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
                val db = DatabaseFactory.open(ctx, decoyFile, key)
                key.zero()
                _state.update { AppState.UnlockedDecoy(db) }
            }
            AuthMeta.DuressMode.WIPE -> {
                val decoyFile = File(ctx.filesDir, "decoy.db")
                dbFile.delete()
                decoyFile.delete()
                authMetaFile.delete()
                _state.update { AppState.Onboarding }
            }
            AuthMeta.DuressMode.OFF -> Unit
        }
    }

    fun lock() {
        viewModelScope.launch(Dispatchers.IO) {
            (_state.value as? AppState.Unlocked)?.db?.close()
            (_state.value as? AppState.UnlockedDecoy)?.db?.close()
            _state.update { AppState.Locked }
        }
    }

    /** Called by OnboardingViewModel once auth_meta.bin and the DB exist. */
    fun setUnlocked(db: TidesDatabase) {
        _state.update { AppState.Unlocked(db) }
    }
}
