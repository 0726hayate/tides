package com.hayate0726.tides.ui.lock

import androidx.lifecycle.ViewModel
import com.hayate0726.tides.crypto.Pin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor() : ViewModel() {
    // Note: plan listed `onAttempt: (Pin) -> Unit = {}` here, but Hilt cannot
    // inject function types. The plan body never uses it, so it has been removed.
    // Attempts are dispatched from the host composable observing this state.

    private val _pin = MutableStateFlow(CharArray(0))
    val pin = _pin.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun pushDigit(d: Int) {
        if (_pin.value.size >= 12) return
        _pin.value = _pin.value + d.digitToChar()
        _error.value = null
        if (_pin.value.size >= 6) submitIfComplete()
    }

    fun backspace() {
        if (_pin.value.isNotEmpty()) {
            _pin.value = _pin.value.dropLast(1).toCharArray()
            _error.value = null
        }
    }

    fun reset() {
        java.util.Arrays.fill(_pin.value, 0.toChar())
        _pin.value = CharArray(0)
    }

    private fun submitIfComplete() {
        // Note: in real wiring, the AppViewModel's onUnlockAttempt is called by the host
        // composable observing this state. We don't keep a callback here to keep the
        // ViewModel pure for testing.
    }

    fun consumePin(): Pin {
        val p = Pin(_pin.value.copyOf())
        java.util.Arrays.fill(_pin.value, 0.toChar())
        _pin.value = CharArray(0)
        return p
    }

    fun showError(msg: String) { _error.value = msg }
}
