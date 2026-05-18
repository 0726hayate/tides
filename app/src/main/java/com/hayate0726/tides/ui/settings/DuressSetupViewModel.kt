package com.hayate0726.tides.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Saves a duress PIN into the AuthMeta duress slot.
 *
 * Available only when the user's threat preset is ALWAYS_LOCKED
 * (per ThreatPreset.duressAvailable); the caller (settings nav) is
 * responsible for gating entry to [DuressSetupScreen].
 *
 * In DECOY mode the duress PIN's derived key seeds an empty
 * `decoy.db` so AppViewModel.handleDuress can re-derive that same
 * key from the duress slot's keySalt at unlock time and open the
 * decoy DB. In WIPE mode no decoy DB is created — AppViewModel
 * just deletes everything.
 *
 * The duress PIN must not collide with the primary PIN: if it did,
 * LockManager would match the primary first and the duress path
 * would be unreachable, which is worse than no duress at all.
 */
@HiltViewModel
class DuressSetupViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val authMetaStore: FileAuthMetaStore,
) : ViewModel() {

    sealed interface SaveResult {
        data object Idle : SaveResult
        data object Saving : SaveResult
        data object Success : SaveResult
        data class Error(val message: String) : SaveResult
    }

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    fun save(duressPinText: String, mode: DuressMode) {
        if (_saveResult.value == SaveResult.Saving) return
        _saveResult.value = SaveResult.Saving

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meta = authMetaStore.load()

                // Collision check: duress PIN must differ from primary PIN.
                // Otherwise LockManager matches primary first and duress is
                // unreachable.
                val collisionPin = Pin(duressPinText.toCharArray())
                val collides = try {
                    KeyDerivation.validatePin(collisionPin, meta.pinHashSalt, meta.pinHash)
                } finally {
                    collisionPin.zero()
                }
                if (collides) {
                    _saveResult.value = SaveResult.Error(
                        "Duress PIN must differ from your primary PIN."
                    )
                    return@launch
                }

                val rng = SecureRandom()
                val duressKeySalt = ByteArray(16).also(rng::nextBytes)
                val duressPinHashSalt = ByteArray(16).also(rng::nextBytes)

                val pin = Pin(duressPinText.toCharArray())
                val duressPinHash = KeyDerivation.derivePinHash(pin, duressPinHashSalt)
                val duressKey = KeyDerivation.deriveKey(pin, duressKeySalt)
                pin.zero()

                val authMode = when (mode) {
                    DuressMode.DECOY -> AuthMeta.DuressMode.DECOY
                    DuressMode.WIPE -> AuthMeta.DuressMode.WIPE
                }

                // For DECOY: open (and thus create, if absent) the decoy DB
                // with the duress-derived key. AppViewModel.handleDuress will
                // re-derive the same key from duress.keySalt on unlock.
                //
                // For WIPE: no decoy DB needed; if one exists from a previous
                // DECOY configuration, leave it — handleDuress deletes it on
                // wipe path anyway.
                if (mode == DuressMode.DECOY) {
                    val decoyFile = File(ctx.filesDir, "decoy.db")
                    val db = DatabaseFactory.open(ctx, decoyFile, duressKey)
                    db.close()
                }
                duressKey.zero()

                authMetaStore.update { current ->
                    AuthMeta(
                        keySalt = current.keySalt,
                        pinHashSalt = current.pinHashSalt,
                        pinHash = current.pinHash,
                        duress = AuthMeta.Duress(
                            keySalt = duressKeySalt,
                            pinHashSalt = duressPinHashSalt,
                            pinHash = duressPinHash,
                            mode = authMode,
                        ),
                        failCount = current.failCount,
                        cooldownExpiryEpochMs = current.cooldownExpiryEpochMs,
                    )
                }

                _saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(
                    e.message ?: "Could not save duress PIN."
                )
            }
        }
    }

    fun acknowledge() {
        if (_saveResult.value is SaveResult.Error || _saveResult.value is SaveResult.Success) {
            _saveResult.value = SaveResult.Idle
        }
    }
}
