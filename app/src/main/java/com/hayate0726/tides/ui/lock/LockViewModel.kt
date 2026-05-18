package com.hayate0726.tides.ui.lock

import androidx.lifecycle.ViewModel
import com.hayate0726.tides.crypto.Pin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor() : ViewModel() {

    private val _pin = MutableStateFlow(CharArray(0))
    val pin = _pin.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * True while an attempt is in flight (PIN consumed, awaiting result).
     * Blocks pushDigit and consumePin so a fast 7th tap or a re-collect
     * can't double-fire the attempt.
     */
    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    fun pushDigit(d: Int) {
        if (_submitting.value) return
        if (_pin.value.size >= 12) return
        _pin.value = _pin.value + d.digitToChar()
        _error.value = null
    }

    fun backspace() {
        if (_submitting.value) return
        if (_pin.value.isNotEmpty()) {
            _pin.value = _pin.value.dropLast(1).toCharArray()
            _error.value = null
        }
    }

    fun reset() {
        java.util.Arrays.fill(_pin.value, 0.toChar())
        _pin.value = CharArray(0)
        _submitting.value = false
    }

    /**
     * Returns the current PIN as a [Pin], clears local state, and marks the
     * ViewModel as submitting. Returns null if already submitting or the PIN
     * isn't long enough — host must call [onAttemptResolved] when the
     * attempt completes (success or failure) to re-enable input.
     */
    fun consumePin(minLength: Int = 6): Pin? {
        if (_submitting.value) return null
        if (_pin.value.size < minLength) return null
        _submitting.value = true
        val p = Pin(_pin.value.copyOf())
        java.util.Arrays.fill(_pin.value, 0.toChar())
        _pin.value = CharArray(0)
        return p
    }

    /** Re-enable input after an attempt resolves (whatever the outcome). */
    fun onAttemptResolved() {
        _submitting.value = false
    }

    fun showError(msg: String) { _error.value = msg }
}
