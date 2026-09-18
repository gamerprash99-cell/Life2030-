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
 * Thrown when LifeOS cannot recover the SQLCipher passphrase for an existing
 * database. This is deliberately a hard, non-destructive failure: the raw
 * database file is left untouched so the user's data is never silently
 * destroyed by "recovering" with a brand new passphrase.
 */
class DatabaseKeyUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/**
 * Generates and protects the SQLCipher database passphrase.
 *
 * A random 32-byte passphrase is created once per install and then wrapped
 * ("encrypted") by an AES-GCM key that lives in the Android Keystore. Only
 * the wrapped passphrase is persisted; the raw passphrase is only ever held
 * in memory long enough to open the Room database.
 *
 * DATA-SAFETY CONTRACT (P0):
 * - Fresh install (no wrapped passphrase, no database file) -> create a new
 *   random passphrase and wrap it.
 * - Existing install with valid wrapped passphrase -> reuse it.
 * - Existing install whose wrapped passphrase cannot be unwrapped, or which
 *   has a database file but no wrapped passphrase at all -> FAIL with
 *   [DatabaseKeyUnavailableException]. The passphrase is NEVER rotated and
 *   the database is NEVER deleted or recreated, because either action would
 *   make the user's existing data permanently unreadable.
 *
 * This never stores the passphrase in plaintext and never derives it from
 * the user's PIN (which changes on recovery).
 */
object DatabasePassphraseProvider {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "lifeos_db_passphrase_key"
    private const val PREFS_NAME = "lifeos_secure_store"
    private const val PREFS_ENC_PASSPHRASE = "enc_db_passphrase"
    private const val PREFS_IV = "db_passphrase_iv"
    private const val GCM_TAG_BITS = 128
    private const val PASSPHRASE_BYTES = 32

    internal const val DATABASE_NAME = "lifeos.db"

    /** The action to take when deciding whether a passphrase can be created. */
    internal enum class PassphraseAction { REUSE, CREATE_FRESH, FAIL_KEY_UNAVAILABLE }

    /**
     * Pure decision table (unit-tested) that encodes the data-safety contract
     * above without touching Android Keystore or the filesystem.
     */
    internal fun resolveAction(
        hasWrappedPassphrase: Boolean,
        unwrapSucceeded: Boolean,
        databaseFileExists: Boolean
    ): PassphraseAction = when {
        hasWrappedPassphrase && unwrapSucceeded -> PassphraseAction.REUSE
        // Wrapped material exists but cannot be read: corrupted/inaccessible
        // key material on an existing install. Never rotate.
        hasWrappedPassphrase && !unwrapSucceeded -> PassphraseAction.FAIL_KEY_UNAVAILABLE
        // No wrapped material at all, but a database file exists: this is an
        // existing install whose key is gone. Creating a new passphrase would
        // permanently lock the user out of their own data.
        databaseFileExists -> PassphraseAction.FAIL_KEY_UNAVAILABLE
        else -> PassphraseAction.CREATE_FRESH
    }

    /**
     * Returns the 32-byte database passphrase, creating and persisting a
     * wrapped copy the first time it is called on a fresh install.
     *
     * @throws DatabaseKeyUnavailableException when an existing database's key
     *   cannot be recovered. Callers must NOT catch this to create a new key.
     */
    fun getOrCreate(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedEnc = prefs.getString(PREFS_ENC_PASSPHRASE, null)
        val storedIv = prefs.getString(PREFS_IV, null)

        val keyStore = loadKeyStore()
        val hasWrappedPassphrase = storedEnc != null && storedIv != null
        val databaseFileExists = context.getDatabasePath(DATABASE_NAME).exists()

        if (hasWrappedPassphrase) {
            val keyExists = keyStore.containsAlias(KEY_ALIAS)
            val unwrapped = if (keyExists) {
                runCatching { unwrap(keyStore, storedEnc!!, storedIv!!) }.getOrNull()
            } else {
                null
            }
            if (unwrapped != null && unwrapped.size == PASSPHRASE_BYTES) {
                return unwrapped
            }
            // Existing install with unreadable key material: fail safely.
            throw DatabaseKeyUnavailableException(
                "LifeOS could not unlock the encrypted database because its key material " +
                    "is missing or inaccessible. Your data has been preserved and was not " +
                    "modified. Recover it from a LifeOS backup or restore the device's " +
                    "Android Keystore, then reopen LifeOS."
            )
        }

        return when (resolveAction(hasWrappedPassphrase = false, unwrapSucceeded = false, databaseFileExists = databaseFileExists)) {
            PassphraseAction.FAIL_KEY_UNAVAILABLE -> throw DatabaseKeyUnavailableException(
                "LifeOS found an existing encrypted database but no longer has the key to " +
                    "open it. Your data has been preserved and was not modified. Recover it " +
                    "from a LifeOS backup, then reopen LifeOS."
            )
            PassphraseAction.REUSE,
            PassphraseAction.CREATE_FRESH -> createAndPersistPassphrase(context, prefs, keyStore)
        }
    }

    private fun createAndPersistPassphrase(
        context: Context,
        prefs: android.content.SharedPreferences,
        keyStore: KeyStore
    ): ByteArray {
        ensureKeyLivesInKeystore(keyStore)
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
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
