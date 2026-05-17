package com.hayate0726.tides.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper around androidx.biometric.BiometricPrompt for use from
 * Compose. The Activity reference must be a [FragmentActivity] —
 * `LocalContext.current as? FragmentActivity` in the composable.
 *
 * This controller is intentionally minimal. The actual unwrap of the
 * Keystore-wrapped DB key on biometric success happens in a follow-up
 * commit that introduces a biometric-bound key alias and the unwrap
 * flow. For now BiometricController just verifies that the user
 * authenticated and fires an [onSuccess] callback the host can use to
 * proceed (e.g., trigger an already-stored unlock path).
 */
object BiometricController {

    enum class Availability {
        AVAILABLE,
        /** No biometric hardware on the device. */
        NO_HARDWARE,
        /** Hardware present but no enrolled prints/face. */
        NONE_ENROLLED,
        /** Some other system-level reason (locked out, unsupported). */
        UNAVAILABLE,
    }

    fun availability(activity: FragmentActivity): Availability {
        val mgr = BiometricManager.from(activity)
        return when (mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else -> Availability.UNAVAILABLE
        }
    }

    /**
     * Show the system BiometricPrompt.
     *
     * @param onSuccess invoked on the main thread when the user authenticates.
     * @param onError invoked on the main thread with the system error message
     *   on any non-success terminal outcome (cancel, lockout, etc.).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Tides",
        subtitle: String = "Use your biometric to unlock the app.",
        negativeButton: String = "Use PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }
}
