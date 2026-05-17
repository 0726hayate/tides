package com.hayate0726.tides.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.lock.LockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RateLimitTest {

    @Test
    fun five_wrong_attempts_then_cooldown_blocks_correct_pin() {
        val correctPin = "112233"
        val pinHashSalt = ByteArray(16) { 7 }
        val pinHash = KeyDerivation.derivePinHash(Pin(correctPin.toCharArray()), pinHashSalt)
        val meta = AuthMeta(
            keySalt = ByteArray(16),
            pinHashSalt = pinHashSalt,
            pinHash = pinHash,
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )

        var currentMeta = meta
        val store = object : LockManager.AuthMetaStore {
            override fun load() = currentMeta
            override fun update(updater: (AuthMeta) -> AuthMeta) { currentMeta = updater(currentMeta) }
        }
        val nowHolder = longArrayOf(1_000_000L)
        val mgr = LockManager(store, LockManager.Clock { nowHolder[0] })

        repeat(5) {
            val r = mgr.attemptUnlock(Pin("000000".toCharArray()))
            assertTrue(r is LockManager.UnlockResult.WrongPin)
        }
        assertEquals(5, currentMeta.failCount)
        assertEquals(nowHolder[0] + 30_000L, currentMeta.cooldownExpiryEpochMs)

        val r = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(
            "Correct PIN unlocked during cooldown — rate-limit broken",
            r is LockManager.UnlockResult.RateLimited,
        )

        nowHolder[0] += 31_000L
        val r2 = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(r2 is LockManager.UnlockResult.Success)
    }
}
