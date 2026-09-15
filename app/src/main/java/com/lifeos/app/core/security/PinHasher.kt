package com.lifeos.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 hashing for the LifeOS App PIN and recovery answer.
 *
 * Neither the PIN nor the recovery answer is ever stored in plaintext —
 * only a random salt and the resulting hash are persisted (see
 * core/util/SettingsStore.kt). Verification re-hashes the entered value
 * with the stored salt and compares digests; the original value is never
 * reconstructed or logged.
 */
object PinHasher {

    private const val SALT_BYTES = 16

    data class SaltedHash(val saltBase64: String, val hashBase64: String)

    fun hash(rawValue: String): SaltedHash {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = hashWithSalt(rawValue, salt)
        return SaltedHash(
            saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP),
            hashBase64 = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
        )
    }

    fun verify(rawValue: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP)
        val digest = hashWithSalt(rawValue, salt)
        val actualHashBase64 = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
        return constantTimeEquals(actualHashBase64, expectedHashBase64)
    }

    private fun hashWithSalt(rawValue: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(rawValue.toByteArray(Charsets.UTF_8))
    }

    /** Avoids timing-attack-friendly short-circuit string comparison. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
