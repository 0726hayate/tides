package com.hayate0726.tides.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel

@Composable
fun LockHost(appViewModel: AppViewModel) {
    val vm: LockViewModel = hiltViewModel()
    val pinChars by vm.pin.collectAsStateWithLifecycle()
    val vmError by vm.error.collectAsStateWithLifecycle()
    val submitting by vm.submitting.collectAsStateWithLifecycle()
    val appError by appViewModel.unlockError.collectAsStateWithLifecycle()
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    val appearanceRepository = remember(ctx) {
        dagger.hilt.android.EntryPointAccessors
            .fromApplication(
                ctx.applicationContext,
                com.hayate0726.tides.ui.nav.MainGraphEntryPoint::class.java,
            )
            .appearanceRepository()
    }
    val useShuffled by appearanceRepository.shufflePinKeypad.collectAsStateWithLifecycle()
    val activity = ctx as? FragmentActivity

    val biometricAvailable = remember(activity) {
        activity != null
            && BiometricController.availability(activity) == BiometricController.Availability.AVAILABLE
            && appViewModel.isBiometricEnrolled()
    }
    var autoTriggered by remember { mutableStateOf(false) }

    fun triggerBiometric() {
        val a = activity ?: return
        BiometricController.authenticate(
            activity = a,
            onSuccess = { appViewModel.onBiometricSuccess() },
            onError = { /* user cancelled or system error; PIN remains available */ },
        )
    }

    LaunchedEffect(biometricAvailable, autoTriggered) {
        if (biometricAvailable && !autoTriggered) {
            autoTriggered = true
            triggerBiometric()
        }
    }

    LaunchedEffect(pinChars.size, submitting) {
        if (pinChars.size >= 6 && !submitting) {
            val pin = vm.consumePin() ?: return@LaunchedEffect
            appViewModel.onUnlockAttempt(pin)
        }
    }

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
        onBiometric = if (biometricAvailable) ({ triggerBiometric() }) else null,
        error = appError ?: vmError,
        cooldownExpiryEpochMs = cooldownExpiryMs,
        useShuffledKeypad = useShuffled,
    )
}
