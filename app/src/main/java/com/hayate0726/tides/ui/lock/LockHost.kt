package com.hayate0726.tides.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel

/**
 * Hosts LockScreen and wires it to AppViewModel + LockViewModel.
 *
 * Auto-submits the PIN as soon as it reaches 6 digits — there's no
 * dedicated "Submit" button, matching the spec's iOS-style numeric PIN UX.
 *
 * Biometric is not wired here yet — BiometricController landing in a
 * follow-up commit and the AppViewModel.onBiometricUnlock entry point.
 */
@Composable
fun LockHost(
    appViewModel: AppViewModel,
) {
    val vm: LockViewModel = hiltViewModel()
    val pinChars by vm.pin.collectAsStateWithLifecycle()
    val vmError by vm.error.collectAsStateWithLifecycle()
    val appError by appViewModel.unlockError.collectAsStateWithLifecycle()
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    // When the PIN reaches 6 digits, hand it off to AppViewModel.
    LaunchedEffect(pinChars.size) {
        if (pinChars.size >= 6) {
            appViewModel.onUnlockAttempt(vm.consumePin())
        }
    }

    val cooldownExpiryMs = (appState as? AppState.LockedCooldown)?.expiryEpochMs

    LockScreen(
        pinLength = pinChars.size,
        onDigit = { d ->
            // Starting a new attempt clears any previous error.
            if (appError != null) appViewModel.clearUnlockError()
            vm.pushDigit(d)
        },
        onBackspace = { vm.backspace() },
        onBiometric = null,
        error = appError ?: vmError,
        cooldownExpiryEpochMs = cooldownExpiryMs,
    )
}
