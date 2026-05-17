package com.hayate0726.tides.lock

import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

/**
 * Depends on KeyDerivation (Argon2 JNI), which doesn't load on JVM/Robolectric.
 * Covered by instrumented tests in Tasks 13 and 14.
 */
@Disabled("argon2kt native lib not available on JVM; covered by instrumented tests in Tasks 13/14")
class LockManagerTest {

    private val nowMs = AtomicLong(1_000_000L)
    private val clock = LockManager.Clock { nowMs.get() }

    private val keySalt = ByteArray(16) { 1 }
    private val pinHashSalt = ByteArray(16) { 2 }
    private val correctPin = "654321"
    private val correctHash by lazy {
        KeyDerivation.derivePinHash(Pin(correctPin.toCharArray()), pinHashSalt)
    }

    private fun baseMeta(): AuthMeta = AuthMeta(
        keySalt = keySalt,
        pinHashSalt = pinHashSalt,
        pinHash = correctHash,
        duress = null,
        failCount = 0,
        cooldownExpiryEpochMs = 0L,
    )

    @Test
    fun `correct PIN unlocks and resets fail count`() {
        val store = InMemoryAuthMetaStore(baseMeta())
        val mgr = LockManager(store, clock)
        val result = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(result is LockManager.UnlockResult.Success)
        assertEquals(0, store.current().failCount)
    }

    @Test
    fun `wrong PIN increments fail count`() {
        val store = InMemoryAuthMetaStore(baseMeta())
        val mgr = LockManager(store, clock)
        repeat(3) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(3, store.current().failCount)
    }

    @Test
    fun `5th wrong PIN triggers 30s cooldown`() {
        val store = InMemoryAuthMetaStore(baseMeta())
        val mgr = LockManager(store, clock)
        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        val meta = store.current()
        assertEquals(5, meta.failCount)
        assertEquals(nowMs.get() + 30_000L, meta.cooldownExpiryEpochMs)
    }

    @Test
    fun `unlock during cooldown returns RateLimited even with correct PIN`() {
        val seeded = baseMeta().copyWith(
            failCount = 5,
            cooldownExpiryEpochMs = nowMs.get() + 10_000L,
        )
        val store = InMemoryAuthMetaStore(seeded)
        val mgr = LockManager(store, clock)
        val r = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(r is LockManager.UnlockResult.RateLimited)
    }

    @Test
    fun `cooldown doubles each subsequent batch`() {
        val store = InMemoryAuthMetaStore(baseMeta())
        val mgr = LockManager(store, clock)

        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(30_000L, store.current().cooldownExpiryEpochMs - nowMs.get())
        nowMs.set(nowMs.get() + 31_000L)

        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(60_000L, store.current().cooldownExpiryEpochMs - nowMs.get())
        nowMs.set(nowMs.get() + 61_000L)

        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(120_000L, store.current().cooldownExpiryEpochMs - nowMs.get())
    }

    @Test
    fun `cooldown caps at 1 hour`() {
        val seeded = baseMeta().copyWith(failCount = 100, cooldownExpiryEpochMs = 0L)
        val store = InMemoryAuthMetaStore(seeded)
        val mgr = LockManager(store, clock)
        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        val gap = store.current().cooldownExpiryEpochMs - nowMs.get()
        assertEquals(3_600_000L, gap)
    }

    private class InMemoryAuthMetaStore(initial: AuthMeta) : LockManager.AuthMetaStore {
        private var state = initial
        fun current() = state
        override fun load(): AuthMeta = state
        override fun update(updater: (AuthMeta) -> AuthMeta) { state = updater(state) }
    }
}
