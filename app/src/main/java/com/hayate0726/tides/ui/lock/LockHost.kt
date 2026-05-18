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
 * The LockViewModel `submitting` flag blocks double-fire from a fast 7th
 * tap or a recompose-triggered re-collect. Host resets the flag when the
 * unlock result is observed (either AppState transition or unlockError).
 *
 * Biometric is not wired here yet — BiometricController exists but the
 * Keystore-bound unwrap flow lands as a separate feature.
 */
@Composable
fun LockHost(
    appViewModel: AppViewModel,
) {
    val vm: LockViewModel = hiltViewModel()
    val pinChars by vm.pin.collectAsStateWithLifecycle()
    val vmError by vm.error.collectAsStateWithLifecycle()
    val submitting by vm.submitting.collectAsStateWithLifecycle()
    val appError by appViewModel.unlockError.collectAsStateWithLifecycle()
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    // When the PIN reaches 6 digits and we're not already submitting,
    // hand it off to AppViewModel. consumePin() is idempotent — only
    // the first call past the threshold succeeds.
    LaunchedEffect(pinChars.size, submitting) {
        if (pinChars.size >= 6 && !submitting) {
            val pin = vm.consumePin() ?: return@LaunchedEffect
            appViewModel.onUnlockAttempt(pin)
        }
    }

    // Re-enable input once the attempt resolves: either the app state
    // transitioned (Unlocked / LockedCooldown / Onboarding from wipe)
    // or unlockError surfaced (WrongPin).
    LaunchedEffect(appState, appError) {
        if (submitting && (appState !is AppState.Locked || appError != null)) {
            vm.onAttemptResolved()
        }
    }

    val cooldownExpiryMs = (appState as? AppState.LockedCooldown)?.expiryEpochMs

    LockScreen(
        pinLength = pinChars.size,
        onDigit = { d ->
            if (appError != null) appViewModel.clearUnlockError()
            vm.pushDigit(d)
        },
        onBackspace = { vm.backspace() },
        onBiometric = null,
        error = appError ?: vmError,
        cooldownExpiryEpochMs = cooldownExpiryMs,
    )
}
