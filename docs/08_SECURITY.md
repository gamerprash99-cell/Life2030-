# 08 — Security Review

**Review date:** 2026-09-16

## Current security architecture

### App Lock

LifeOS supports:

- `NONE`
- `PIN`

The previous `BIOMETRIC` mode was removed. Current App Lock is PIN-only.

PINs and recovery answers are hashed with `PinHasher` (PBKDF2-HMAC-SHA256,
120,000 iterations, per-secret random 16-byte salt — format `v2$<iters>$<hash>`).
Legacy v1 SHA-256 hashes created before the hardening pass still verify so users
are not locked out mid-upgrade. Plaintext PINs and plaintext recovery answers
must never be persisted.

Brute-force protection is enforced in `SettingsStore`: five failed unlock
attempts escalate an account lockout (starting at 30 seconds, capped at 16
minutes). A successful PIN verifies with a constant-time comparison.

### Permissions

Camera, microphone and notification permissions are requested only when the related feature requires them. JSON backup restore uses Android's document picker and does not require broad storage permission.

### Network boundary

`AndroidManifest.xml` does not declare `android.permission.INTERNET`. There is no cloud AI client in the current source.

### Local data

Room and DataStore remain local persistence layers. The Room database is now
encrypted at rest with SQLCipher (`net.zetetic:sqlcipher-android`). The
database passphrase is a random 32-byte secret generated on first run and
wrapped with an Android Keystore AES-GCM key
(`DatabasePassphraseProvider`); the raw passphrase is never stored in plaintext.
See `data/db/AppDatabase.kt`.

## Known risks

| Priority | Finding | Current state |
|---|---|---|
| High | Room database is not encrypted at rest | Resolved — SQLCipher + Keystore-wrapped passphrase |
| Medium | PIN/recovery implementation requires continued security review | Mitigated — PBKDF2 (120k) + lockout; verify with device testing |
| Medium | Biometric result is device-level by Android design | No longer applicable — biometric mode removed |
| Medium | No automated security regression suite | Resolved — unit tests cover hashing + backup serialization |
| Low | Keystore-wrapped passphrase has no biometric/user-auth gate | Open / acceptable for current threat model |

## Biometric UX requirement

When enabling biometric App Lock:

1. explain Android device-level biometric behavior;
2. check availability;
3. guide enrollment through supported Android settings when required;
4. re-check after return;
5. authenticate successfully before saving the preference.

Do not implement a custom fingerprint database or fake biometric prompt.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The capture redesign does not add storage or network permissions. Camera and microphone access are still requested only when the relevant capture mode is opened.


## Current state — 2026-09-16

The biometric App Lock path has been removed. The remaining App Lock choices are `NONE` and `PIN`, with secure salted-hash verification and recovery-question verification.

## 2026-09-18 security update
Profile photo metadata is stored locally as a content URI in DataStore; the image is not uploaded by LifeOS. App Lock continues to use the existing hashed PIN/recovery flow. Audio and other captures remain in app-private storage.
