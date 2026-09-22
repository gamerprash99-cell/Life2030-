package com.lifeos.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockoutPolicyTest {

    private val base = 30_000L
    private val max = 16 * 60 * 1000L

    @Test
    fun `no lockout while under max attempts`() {
        assertEquals(0L, LockoutPolicy.lockoutDurationMillis(failures = 4, maxAttempts = 5, baseLockoutMs = base, maxLockoutMs = max))
        assertEquals(0L, LockoutPolicy.lockoutDurationMillis(failures = 1, maxAttempts = 3, baseLockoutMs = base, maxLockoutMs = max))
        assertEquals(0L, LockoutPolicy.lockoutDurationMillis(failures = 0, maxAttempts = 5, baseLockoutMs = base, maxLockoutMs = max))
    }

    @Test
    fun `first lockout is the base duration`() {
        assertEquals(base, LockoutPolicy.lockoutDurationMillis(5, 5, base, max))
        assertEquals(base, LockoutPolicy.lockoutDurationMillis(3, 3, base, max))
    }

    @Test
    fun `repeated failures double the lockout each tier`() {
        assertEquals(base * 2, LockoutPolicy.lockoutDurationMillis(6, 5, base, max))
        assertEquals(base * 4, LockoutPolicy.lockoutDurationMillis(7, 5, base, max))
        assertEquals(base * 8, LockoutPolicy.lockoutDurationMillis(8, 5, base, max))
    }

    @Test
    fun `lockout is capped at sixteen minutes`() {
        assertEquals(max, LockoutPolicy.lockoutDurationMillis(20, 5, base, max))
        assertEquals(max, LockoutPolicy.lockoutDurationMillis(1000, 5, base, max))
    }

    @Test
    fun `attempts remaining counts down to zero`() {
        assertEquals(5, LockoutPolicy.attemptsRemaining(failures = 0, maxAttempts = 5))
        assertEquals(1, LockoutPolicy.attemptsRemaining(failures = 4, maxAttempts = 5))
        assertEquals(0, LockoutPolicy.attemptsRemaining(failures = 5, maxAttempts = 5))
        assertEquals(0, LockoutPolicy.attemptsRemaining(failures = 9, maxAttempts = 5))
    }

    @Test
    fun `tier exponent is capped at eight`() {
        // failures = maxAttempts + tier; tier coerced to at most 8.
        val atCap = LockoutPolicy.lockoutDurationMillis(5 + 8, 5, base, max)
        val beyondCap = LockoutPolicy.lockoutDurationMillis(5 + 9, 5, base, max)
        assertTrue(atCap <= max)
        assertEquals(atCap, beyondCap)
    }
}
