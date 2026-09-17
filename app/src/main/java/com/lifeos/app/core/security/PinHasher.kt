package com.lifeos.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted key-derivation for the LifeOS App PIN and recovery answer.
 *
 * Neither the PIN nor the recovery answer is ever stored in plaintext —
 * only a random salt and the resulting derived hash are persisted (see
 * core/util/SettingsStore.kt). Verification re-derives the value with the
 * stored salt and compares digests in constant time; the original value is
 * never reconstructed or logged.
 *
 * Version 2 (current): PBKDF2-HMAC-SHA256 with a per-install random salt and
 * a high iteration count. This is deliberately slow, which is what makes a
 * short numeric PIN expensive to brute-force if the device storage is ever
 * copied. Stored hash format:
 *
 *     v2$<iterations>$<base64(pbkdf2 bytes)>
 *
 * Version 1 (legacy): a single unsalted-iteration SHA-256 of salt + value.
 * It is still *verified* so that PINs created before this upgrade keep
 * working, but it is never produced for new PINs.
 */
object PinHasher {

    private const val SALT_BYTES = 16
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val V2_PREFIX = "v2"

    data class SaltedHash(val saltBase64: String, val hashBase64: String)

    fun hash(rawValue: String): SaltedHash {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = pbkdf2(rawValue, salt, PBKDF2_ITERATIONS)
        return SaltedHash(
            saltBase64 = encodeBase64(salt),
            hashBase64 = "$V2_PREFIX\$$PBKDF2_ITERATIONS\$${encodeBase64(digest)}"
        )
    }

    fun verify(rawValue: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val salt = runCatching { decodeBase64(saltBase64) }.getOrNull() ?: return false
        return if (expectedHashBase64.startsWith("$V2_PREFIX\$")) {
            val parts = expectedHashBase64.split('$')
            if (parts.size != 3) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val expected = parts[2]
            val actual = encodeBase64(pbkdf2(rawValue, salt, iterations))
            constantTimeEquals(actual, expected)
        } else {
            // Legacy v1: single-pass salted SHA-256.
            val actual = encodeBase64(legacySha256(rawValue, salt))
            constantTimeEquals(actual, expectedHashBase64)
        }
    }

    private fun pbkdf2(rawValue: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(rawValue.toCharArray(), salt, iterations, PBKDF2_KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacySha256(rawValue: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(rawValue.toByteArray(Charsets.UTF_8))
    }

    private fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decodeBase64(value: String): ByteArray = Base64.getDecoder().decode(value)

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
