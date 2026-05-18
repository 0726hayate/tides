package com.hayate0726.tides.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.BiometricKeyStore
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Two-phase enrollment because the biometric-bound Keystore key requires a
 * preceding BiometricPrompt session for the cipher.init(ENCRYPT_MODE) op:
 *
 *   prepare()         -> derive DB key from primary PIN, hold it,
 *                        emit Status.AwaitingBiometric
 *   completeEnroll()  -> screen fires BiometricPrompt, on success calls this,
 *                        we wrap + persist within the 30s validity window
 *   cancelPending()   -> screen calls this if the user cancels the prompt;
 *                        zeroes the held key
 */
class BiometricToggleViewModel(
    private val authMetaStore: FileAuthMetaStore,
    private val biometricKeyStore: BiometricKeyStore,
) : ViewModel() {

    sealed interface Status {
        data object Idle : Status
        data object Working : Status
        /** Key is held; screen should fire BiometricPrompt and then call [completeEnroll]. */
        data object AwaitingBiometric : Status
        data object EnrolledOk : Status
        data object DisabledOk : Status
        data class Error(val message: String) : Status
    }

    private val _enrolled = MutableStateFlow(biometricKeyStore.isEnrolled())
    val enrolled: StateFlow<Boolean> = _enrolled.asStateFlow()

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    /** Held between prepare() and completeEnroll()/cancelPending(). Zeroed on every transition. */
    private var pendingKey: DbKey? = null

    fun prepare(primaryPin: String) {
        if (primaryPin.length < 6) return
        if (_status.value == Status.Working || _status.value == Status.AwaitingBiometric) return
        _status.value = Status.Working
        val pinChars = primaryPin.toCharArray()
        viewModelScope.launch {
            try {
                val key = withContext(Dispatchers.IO) {
                    val meta = authMetaStore.load()
                    val pin = Pin(pinChars.copyOf())
                    val ok = KeyDerivation.validatePin(pin, meta.pinHashSalt, meta.pinHash)
                    if (!ok) {
                        pin.zero()
                        throw IllegalArgumentException("Wrong primary PIN.")
                    }
                    val derived = KeyDerivation.deriveKey(pin, meta.keySalt)
                    pin.zero()
                    derived
                }
                pendingKey = key
                _status.value = Status.AwaitingBiometric
            } catch (e: Exception) {
                _status.value = Status.Error(e.message ?: "Could not start biometric setup.")
            } finally {
                java.util.Arrays.fill(pinChars, 0.toChar())
            }
        }
    }

    fun completeEnroll() {
        val key = pendingKey ?: return
        if (_status.value != Status.AwaitingBiometric) return
        _status.value = Status.Working
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    biometricKeyStore.enroll(key)
                }
                _enrolled.value = true
                _status.value = Status.EnrolledOk
            } catch (e: Exception) {
                _status.value = Status.Error(e.message ?: "Could not enable biometric unlock.")
            } finally {
                key.zero()
                pendingKey = null
            }
        }
    }

    fun cancelPending(message: String? = null) {
        pendingKey?.zero()
        pendingKey = null
        _status.value = if (message != null) Status.Error(message) else Status.Idle
    }

    fun disable() {
        viewModelScope.launch(Dispatchers.IO) {
            biometricKeyStore.clear()
            _enrolled.value = false
            _status.value = Status.DisabledOk
        }
    }

    fun clearStatus() { _status.value = Status.Idle }
}
