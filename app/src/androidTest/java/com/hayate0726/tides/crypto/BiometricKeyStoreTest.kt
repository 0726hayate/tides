package com.hayate0726.tides.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiometricKeyStoreTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val store = BiometricKeyStore(ctx)

    @After fun cleanup() { store.clear() }

    @Test
    fun isEnrolled_is_false_before_enroll() {
        assertFalse(store.isEnrolled())
    }

    @Test
    fun clear_after_no_enrollment_does_not_throw() {
        store.clear()
        assertFalse(store.isEnrolled())
    }

    // We cannot complete the full enroll+unwrap round trip in an instrumented
    // test because requireBiometric=true keys require user authentication,
    // which only fires during a real BiometricPrompt session. Enroll alone
    // would succeed (key generation doesn't require auth), but reading back
    // would throw UserNotAuthenticatedException — that's the expected guard.
    // Manual test on a device with enrolled fingerprints is on the v1.0
    // release checklist.

    @Test
    fun isEnrolled_reflects_blob_existence_after_dummy_write() {
        // Skip the full enroll() (it'd need real biometric hardware for the
        // wrap call to fully succeed on every emulator); test the file probe.
        ctx.filesDir.resolve("biometric.bin").writeBytes(ByteArray(20))
        assertTrue(store.isEnrolled())
    }
}
