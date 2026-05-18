package com.hayate0726.tides.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.BiometricKeyStore
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BiometricToggleViewModel(
    private val authMetaStore: FileAuthMetaStore,
    private val biometricKeyStore: BiometricKeyStore,
) : ViewModel() {

    sealed interface Status {
        data object Idle : Status
        data object Working : Status
        data object EnrolledOk : Status
        data object DisabledOk : Status
        data class Error(val message: String) : Status
    }

    private val _enrolled = MutableStateFlow(biometricKeyStore.isEnrolled())
    val enrolled: StateFlow<Boolean> = _enrolled.asStateFlow()

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    fun enable(primaryPin: String) {
        if (primaryPin.length < 6) return
        if (_status.value == Status.Working) return
        _status.value = Status.Working
        val pinChars = primaryPin.toCharArray()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val meta = authMetaStore.load()
                    val pin = Pin(pinChars.copyOf())
                    val ok = KeyDerivation.validatePin(pin, meta.pinHashSalt, meta.pinHash)
                    if (!ok) {
                        pin.zero()
                        throw IllegalArgumentException("Wrong primary PIN.")
                    }
                    val key = KeyDerivation.deriveKey(pin, meta.keySalt)
                    pin.zero()
                    try {
                        biometricKeyStore.enroll(key)
                    } finally {
                        key.zero()
                    }
                }
                _enrolled.value = true
                _status.value = Status.EnrolledOk
            } catch (e: Exception) {
                _status.value = Status.Error(e.message ?: "Could not enable biometric unlock.")
            } finally {
                java.util.Arrays.fill(pinChars, 0.toChar())
            }
        }
    }

    fun disable() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            biometricKeyStore.clear()
            _enrolled.value = false
            _status.value = Status.DisabledOk
        }
    }

    fun clearStatus() { _status.value = Status.Idle }
}
