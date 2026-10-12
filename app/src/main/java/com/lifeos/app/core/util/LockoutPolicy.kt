package com.lifeos.app.core.util

/**
 * Pure lockout math shared by the App-Lock PIN and recovery-answer
 * verification paths (Section 59: settings security). Kept free of Android
 * dependencies so the escalating-window policy is unit-testable.
 */
object LockoutPolicy {

    private const val MAX_TIER = 8

    /**
     * Lockout window (millis) to apply after [failures] total failed attempts.
     * Returns 0 while under [maxAttempts]; at/after it, the window doubles each
     * tier (base, base*2, base*4 …) up to [maxLockoutMs].
     *
     * Matches the escalation that shipped for the PIN path:
     * `min(base shl min(tier, 8), 16 minutes)`.
     */
    fun lockoutDurationMillis(
        failures: Int,
        maxAttempts: Int,
        baseLockoutMs: Long,
        maxLockoutMs: Long
    ): Long {
        if (failures < maxAttempts) return 0L
        val tier = (failures - maxAttempts).coerceIn(0, MAX_TIER).toLong()
        return minOf(baseLockoutMs shl tier.toInt(), maxLockoutMs)
    }

    /** Attempts the user may still try before the next lockout, floored at 0. */
    fun attemptsRemaining(failures: Int, maxAttempts: Int): Int =
        (maxAttempts - failures).coerceAtLeast(0)
}
