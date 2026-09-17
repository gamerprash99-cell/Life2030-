package com.lifeos.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and protects the SQLCipher database passphrase.
 *
 * A random 32-byte passphrase is created once per install and then wrapped
 * ("encrypted") by an AES-GCM key that lives in the Android Keystore. Only
 * the wrapped passphrase is persisted; the raw passphrase is only ever held
 * in memory long enough to open the Room database.
 *
 * Because the key material is hardware/OS-backed and non-exportable, a
 * passphrase cannot simply be read back out of the app's data dir — the
 * same class of protection Android gives to its own credential storage.
 *
 * Note: this deliberately does NOT store the passphrase in plaintext and
 * does NOT derive it from the user's PIN (which changes on recovery).
 */
object DatabasePassphraseProvider {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "lifeos_db_passphrase_key"
    private const val PREFS_NAME = "lifeos_secure_store"
    private const val PREFS_ENC_PASSPHRASE = "enc_db_passphrase"
    private const val PREFS_IV = "db_passphrase_iv"
    private const val GCM_TAG_BITS = 128

    /**
     * Returns the 32-byte database passphrase, creating and persisting a
     * wrapped copy the first time it is called.
     */
    fun getOrCreate(context: Context): ByteArray {
        val keyStore = loadKeyStore()
        ensureKeyLivesInKeystore(keyStore)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedEnc = prefs.getString(PREFS_ENC_PASSPHRASE, null)
        val storedIv = prefs.getString(PREFS_IV, null)
        if (storedEnc != null && storedIv != null) {
            val unwrapped = runCatching { unwrap(keyStore, storedEnc, storedIv) }.getOrNull()
            if (unwrapped != null && unwrapped.size == 32) return unwrapped
        }

        // No valid wrapped passphrase present (fresh install or the keystore
        // key was invalidated). Create a brand new random passphrase.
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val (cipherText, iv) = wrap(keyStore, passphrase)
        prefs.edit()
            .putString(PREFS_ENC_PASSPHRASE, cipherText)
            .putString(PREFS_IV, iv)
            .apply()
        return passphrase
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        return keyStore
    }

    private fun ensureKeyLivesInKeystore(keyStore: KeyStore) {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        generator.generateKey()
    }

    private fun wrap(keyStore: KeyStore, passphrase: ByteArray): Pair<String, String> {
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val cipherText = cipher.doFinal(passphrase)
        return Base64.encodeToString(cipherText, Base64.NO_WRAP) to
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun unwrap(keyStore: KeyStore, cipherTextB64: String, ivB64: String): ByteArray? {
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivB64, Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(cipherTextB64, Base64.NO_WRAP))
    }
}