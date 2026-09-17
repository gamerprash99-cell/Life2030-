package com.lifeos.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class PinHasherTest {

    @Test
    fun `hash and verify roundtrip`() {
        val salted = PinHasher.hash("1234")
        assertTrue(PinHasher.verify("1234", salted.saltBase64, salted.hashBase64))
    }

    @Test
    fun `wrong pin is rejected`() {
        val salted = PinHasher.hash("1234")
        assertFalse(PinHasher.verify("4321", salted.saltBase64, salted.hashBase64))
        assertFalse(PinHasher.verify("", salted.saltBase64, salted.hashBase64))
    }

    @Test
    fun `same pin produces unique salts`() {
        val a = PinHasher.hash("1234")
        val b = PinHasher.hash("1234")
        assertNotEquals(a.saltBase64, b.saltBase64)
        assertNotEquals(a.hashBase64, b.hashBase64)
    }

    @Test
    fun `new hashes use the pbkdf2 version prefix`() {
        val salted = PinHasher.hash("1234")
        assertTrue(salted.hashBase64.startsWith("v2$"))
    }

    @Test
    fun `legacy sha256 hash still verifies`() {
        val salt = ByteArray(16) { it.toByte() }
        val digest = MessageDigest.getInstance("SHA-256").let { md ->
            md.update(salt)
            md.digest("1234".toByteArray(Charsets.UTF_8))
        }
        val legacyHash = Base64.getEncoder().encodeToString(digest)
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        assertTrue(PinHasher.verify("1234", saltB64, legacyHash))
    }

    @Test
    fun `legacy hash rejects wrong pin`() {
        val salt = ByteArray(16) { it.toByte() }
        val digest = MessageDigest.getInstance("SHA-256").let { md ->
            md.update(salt)
            md.digest("1234".toByteArray(Charsets.UTF_8))
        }
        val legacyHash = Base64.getEncoder().encodeToString(digest)
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        assertFalse(PinHasher.verify("0000", saltB64, legacyHash))
    }

    @Test
    fun `malformed stored hash is rejected`() {
        assertFalse(PinHasher.verify("1234", "!!not-base64!!", "v2\$120000\$abc"))
        assertFalse(PinHasher.verify("1234", "AAAA", "v2\$x\$y\$z"))
        assertFalse(PinHasher.verify("1234", "AAAA", ""))
    }

    @Test
    fun `recovery answers are hashed lowercase-trimmed by caller - here arbitrary text works`() {
        val salted = PinHasher.hash("First Pet")
        assertTrue(PinHasher.verify("First Pet", salted.saltBase64, salted.hashBase64))
    }

    @Test
    fun `long pins hash and verify`() {
        val salted = PinHasher.hash("000000")
        assertTrue(PinHasher.verify("000000", salted.saltBase64, salted.hashBase64))
        assertFalse(PinHasher.verify("000001", salted.saltBase64, salted.hashBase64))
    }
}