package com.lifeos.app.core.security

import com.lifeos.app.core.security.DatabasePassphraseProvider.PassphraseAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks down the P0 data-safety contract: a fresh install may create a key,
 * a healthy install reuses its key, and anything ambiguous must FAIL rather
 * than rotate or recreate the database.
 */
class DatabasePassphraseProviderTest {

    @Test
    fun `healthy existing install reuses the wrapped passphrase`() {
        assertEquals(
            PassphraseAction.REUSE,
            DatabasePassphraseProvider.resolveAction(
                hasWrappedPassphrase = true,
                unwrapSucceeded = true,
                databaseFileExists = true
            )
        )
    }

    @Test
    fun `wrapped passphrase that cannot be unwrapped fails and never rotates`() {
        assertEquals(
            PassphraseAction.FAIL_KEY_UNAVAILABLE,
            DatabasePassphraseProvider.resolveAction(
                hasWrappedPassphrase = true,
                unwrapSucceeded = false,
                databaseFileExists = true
            )
        )
    }

    @Test
    fun `existing database without any wrapped passphrase fails`() {
        assertEquals(
            PassphraseAction.FAIL_KEY_UNAVAILABLE,
            DatabasePassphraseProvider.resolveAction(
                hasWrappedPassphrase = false,
                unwrapSucceeded = false,
                databaseFileExists = true
            )
        )
    }

    @Test
    fun `fresh install creates a new passphrase`() {
        assertEquals(
            PassphraseAction.CREATE_FRESH,
            DatabasePassphraseProvider.resolveAction(
                hasWrappedPassphrase = false,
                unwrapSucceeded = false,
                databaseFileExists = false
            )
        )
    }

    @Test
    fun `no database and unreadable wrapped key is treated as corrupted, not fresh`() {
        // Wrapped material is present but unreadable: better to fail loudly than
        // overwrite key material the user may still need.
        assertEquals(
            PassphraseAction.FAIL_KEY_UNAVAILABLE,
            DatabasePassphraseProvider.resolveAction(
                hasWrappedPassphrase = true,
                unwrapSucceeded = false,
                databaseFileExists = false
            )
        )
    }

    @Test
    fun `database name is stable because existing files depend on it`() {
        assertEquals("lifeos.db", DatabasePassphraseProvider.DATABASE_NAME)
    }
}
