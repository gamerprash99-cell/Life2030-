# 07 — Authentication

## Technical explanation

**There is no user authentication system in this application.** Confirmed
by exhaustive repository search: no login screen, no signup/registration
flow, no password field, no OAuth SDK, no session/token management, no
Firebase Auth, no `User` entity in the database, and no "logged in / logged
out" state anywhere in the code.

The app is designed as a **single-user, single-device, local-only** tool —
whoever has the phone unlocked is "the user." There is no concept of
multiple accounts on one device or the same account across multiple devices.

### What *does* exist: App Lock (not authentication)

The one access-control feature is **App Lock** — a screen-level gate that
uses the phone's own biometric/PIN system, not a LifeOS account:

- **File**: `app/src/main/java/com/lifeos/app/core/security/AppLockManager.kt`
- **Mechanism**: `androidx.biometric.BiometricPrompt`, requesting
  `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` — this means it will accept a
  fingerprint/face unlock **or** the device's own PIN/pattern/password.
  LifeOS itself never sees or stores that credential; Android's biometric
  framework handles the actual verification.
- **Gating logic**: `MainActivity.kt`'s `AppLockGate` composable — checks
  `SettingsStore.appLockEnabled`; if true, blocks the `LifeOSNavHost` behind
  a biometric prompt on launch.
- **Toggle**: `ui/settings/SettingsScreen.kt`, a simple on/off `Switch`.
- **Persistence of the toggle**: `core/util/SettingsStore.kt`, DataStore
  Preferences key `app_lock_enabled`.

This is a **local unlock gate**, comparable to how a notes app or banking
app might require Face ID before opening — it is **not** a username/password
or account system, and it does not protect data if someone has direct
filesystem/ADB access to the device (see `docs/08_SECURITY.md`).

### Session management, tokens, password reset, OAuth

**None of these exist.** There is nothing to document because there is no
session, no token, and no password to reset.

### Authorization / protected routes

There is only one user, so there is no role-based access control and no
"protected route" concept beyond the App Lock gate applying to the entire
app at once (all-or-nothing — there's no way to lock only some screens).

---

## Simple explanation (for the founder)

Think of LifeOS today the same way you'd think of your phone's built-in
Notes app: **there's no sign-in screen, no username, no password, and no
account.** Whoever picks up the unlocked phone can open the app and see
everything in it.

The one thing you *can* turn on is **App Lock** — this makes the app itself
ask for your fingerprint or your phone's PIN before it opens, similar to
how some banking apps work. But this isn't a "LifeOS account" — it's
borrowing your phone's own lock screen security. If you ever want real user
accounts (so multiple people could each have their own private LifeOS, or
so your data could sync to a new phone by logging in), that would be new
work — a login system doesn't exist yet at all.

## Security considerations

See `docs/08_SECURITY.md` for the full classified list. Headline items
relevant to authentication:
- 🟡 **MEDIUM** — App Lock is all-or-nothing and defaults to *off*; a new
  install has zero protection until the user manually enables it in Settings.
- 🟢 **LOW** — Because there's no account system, there's no
  password-database, credential-stuffing, or account-takeover risk to
  manage at all — the attack surface that would normally exist here simply
  doesn't.
