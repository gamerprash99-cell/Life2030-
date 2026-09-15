# 08 — Security Review

This is a documentation review, not a penetration test. Findings are based
on reading the code in this repository. Absence of a finding does not mean
the app is secure in that area — it means no obvious issue was found by
manual inspection.

Classification: 🔴 CRITICAL · 🟠 HIGH · 🟡 MEDIUM · 🟢 LOW · ℹ️ INFORMATIONAL

---

### ℹ️ RESOLVED — AI API key no longer exists

**Previously** 🟠 HIGH: the Anthropic API key was stored unencrypted in
DataStore Preferences. **This finding is now moot** — the entire cloud AI
integration was removed (see `docs/11_AI_SYSTEM.md`). There is no API key
anywhere in the app anymore, so there's nothing to protect. This is the
single largest reduction in attack surface from this pass.

### 🟡 MEDIUM — App PIN hash uses DataStore Preferences, not hardware-backed storage

**Where**: `core/util/SettingsStore.kt` — the PIN's salted SHA-256 hash and
the recovery answer's salted hash are stored via DataStore Preferences.

The PIN/recovery answer are never stored in plaintext (see
`core/security/PinHasher.kt` — salted SHA-256, verified by re-hashing and
comparing digests), which is the correct baseline. However, DataStore
Preferences itself is not hardware-backed (no Android Keystore involvement),
so on a rooted device the salt+hash could theoretically be extracted for an
offline brute-force attempt against the PIN (a 4-digit PIN has only 10,000
possibilities). **Recommendation**: for a stronger PIN-storage guarantee in
a future pass, move the salt/hash into an Android Keystore-backed encrypted
store, or add PIN attempt rate-limiting (a fixed delay or lockout after
repeated failed attempts) to blunt brute-force attempts even with local
file access.

### 🟠 HIGH — Database is not encrypted at rest

**Where**: `data/db/AppDatabase.kt` — plain `Room.databaseBuilder(...)`, no
SQLCipher or Room encryption passphrase configured.

All notes, tasks, habits, expenses, and diary entries — the most sensitive
data in the app — are stored in a plain SQLite file (`lifeos.db`) with no
encryption. Combined with App Lock being **off by default**, a lost/stolen
device with USB debugging enabled, or root access, exposes all user data.
**Recommendation**: adopt SQLCipher for Room, or Android's
`EncryptedFile`/Jetpack Security equivalents. This was already flagged as a
"Phase 6 hardening item" in the code's own README before this audit.

### 🟡 MEDIUM — App Lock defaults to disabled

**Where**: `core/util/SettingsStore.kt` — `appLockEnabled` defaults to `false`.

A fresh install has **no access protection at all** until the user manually
opens Settings and turns App Lock on. Given the sensitivity of diary/personal
data, a safer default (or a prompt during onboarding) would reduce risk.

### 🟡 MEDIUM — No certificate pinning on the AI API call

**Where**: `core/ai/AiClient.kt` — plain `OkHttpClient` with default TLS
trust store, no certificate pinning.

Standard HTTPS/TLS is used (good baseline), but there's no defense against a
compromised/malicious CA or a device with a rogue trusted certificate
installed (e.g. some corporate MDM or malware scenarios) intercepting the
AI traffic. Given the AI traffic only carries note/diary text the user
chose to send, the impact is moderate, not severe.

### 🟡 MEDIUM — No input length/size limits before sending to AI

**Where**: `core/ai/AiRepository.kt` — note/diary text is sent to the AI
API with no client-side length cap.

A very large note could produce excessive token usage/cost on the user's
own API key, or hit provider-side limits ungracefully (the failure would
surface as a generic `AiResult.Error`, not a specific "too long" message).

### 🟡 MEDIUM — Debug builds are not minified/obfuscated

**Where**: `app/build.gradle.kts` — `buildTypes { debug { isMinifyEnabled = false } }`.

Standard and expected for debug builds, but flagged so it's not mistaken for
the release configuration. **Release builds do have** `isMinifyEnabled = true`
with `proguard-rules.pro` applied — this is correct.

### 🟢 LOW — No account/credential system to attack

Because there is no login, password, or session system (see
`docs/07_AUTHENTICATION.md`), the entire class of authentication
vulnerabilities (credential stuffing, weak password policy, session
fixation, JWT mishandling, etc.) simply does not apply to this app in its
current form.

### 🟢 LOW — SQL injection risk

**Where**: All DAO queries in `data/db/dao/*.kt` use Room's `@Query`
annotation with parameter binding (`:query`, `:id`, etc.), not raw string
concatenation. Room parameterizes these automatically. No raw/dynamic SQL
string-building was found anywhere in the codebase.

### 🟢 LOW — XSS / injection in the UI

Not applicable in the traditional web sense — this is a native Android
Compose app with no `WebView` usage found anywhere in the source tree, and
no HTML rendering of user or AI content.

### ℹ️ INFORMATIONAL — Exported activity

**Where**: `AndroidManifest.xml` — `MainActivity` has `android:exported="true"`.

This is required (Android mandates the launcher activity be exported) and
is not itself a vulnerability, but it's worth a new developer knowing this
is the only exported component. The `FileProvider` is correctly declared
with `android:exported="false"`.

### ℹ️ INFORMATIONAL — Permissions requested at point of use

**Where**: `core/util/PermissionManager.kt`, used throughout `ui/capture/*`
and the Reminders toggle in `ui/settings/SettingsScreen.kt`.

This is a **positive** finding: Camera, Record Audio, and Post Notifications
permissions are requested only at the moment the relevant feature is used,
not en masse at first launch. This matches Android best practice and
reduces the app's requested-permission footprint at install time.

### ℹ️ INFORMATIONAL — `allowBackup="false"`

**Where**: `AndroidManifest.xml`.

This is a **positive** finding: it prevents the OS's automatic cloud
backup (Auto Backup for Apps) from silently copying the local database
off-device. Combined with `res/xml/data_extraction_rules.xml` and
`res/xml/backup_rules.xml` (both explicitly exclude `database` and `file`
domains), this is a deliberate and correctly implemented privacy control.

### ℹ️ INFORMATIONAL — No secrets committed to the repository

A full-text search for common secret patterns (`api_key`, `secret`,
`password`, `token`) found no hardcoded credentials anywhere in the source
tree. The only "key" in the app is the user's own AI API key, entered at
runtime and never committed to source control.

### ℹ️ INFORMATIONAL — Dependency vulnerability scanning

⚠️ **NOT VERIFIED** — no dependency vulnerability scan (e.g. `gradle
dependencyCheckAnalyze`, GitHub Dependabot, Snyk) has been run or configured
for this repository. Versions in `app/build.gradle.kts` should be checked
against current CVE databases before a production release.

---

## Summary table

| # | Finding | Severity |
|---|---|---|
| 1 | AI API key stored unencrypted in DataStore | 🟠 HIGH |
| 2 | Room/SQLite database not encrypted at rest | 🟠 HIGH |
| 3 | App Lock disabled by default | 🟡 MEDIUM |
| 4 | No certificate pinning on AI API calls | 🟡 MEDIUM |
| 5 | No input size limits before sending text to AI | 🟡 MEDIUM |
| 6 | Debug build not minified (expected, noted for clarity) | 🟡 MEDIUM |
| 7 | No auth system = no credential attack surface | 🟢 LOW (positive) |
| 8 | No SQL injection risk found (parameterized queries) | 🟢 LOW (positive) |
| 9 | No WebView/XSS surface found | 🟢 LOW (positive) |
| 10 | Runtime, point-of-use permission requests | ℹ️ INFORMATIONAL (positive) |
| 11 | `allowBackup="false"` + backup/extraction rules correctly restrict OS backup | ℹ️ INFORMATIONAL (positive) |
| 12 | No committed secrets found | ℹ️ INFORMATIONAL (positive) |
| 13 | No dependency vulnerability scan configured | ℹ️ INFORMATIONAL (gap) |

**Priority recommendation for before any real user data is at stake**:
address findings #1 and #2 (encryption at rest for both the API key and the
database) before this app is used with real personal/diary data on a
device outside a developer's own controlled testing.
