# Security

## 1. Threat model in one sentence

LifeOS holds intensely personal data (private diary, mood, location, spending) and
therefore treats the **device as the trust boundary**: data is stored only in
app-private storage, the app makes no network calls, and no secret is embedded in
the client.

## 2. Data at rest

- All records live in a SQLite database inside the app's private storage
  (`getDatabasePath(...)`), which is not readable by other apps on a
  non-rooted device.
- Media files (when `core/media` lands — see `PROJECT_AUDIT.md`) are written to
  app-private storage, never to shared external storage.
- Android backup rules (`res/xml/data_extraction_rules.xml`) are declared so that
  cloud-backup behaviour is explicit rather than defaulted.

## 3. No secrets in the client

- There is **no API key, token, or credential** anywhere in the source, resources,
  or Gradle files.
- There is **no remote AI provider**. `LocalRuleBasedProvider` runs on-device.
  Adding a remote provider would require a server-side key holder, not an
  embedded client secret; that is documented as future work rather than
  approximated with an insecure shortcut.

## 4. Permissions

The app requests permissions **only at the point of use**, never at startup:

| Permission | Requested when |
| --- | --- |
| `CAMERA` | the user taps capture-photo / capture-video |
| `RECORD_AUDIO` | the user taps capture-audio |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | the user taps capture-location |
| `POST_NOTIFICATIONS` | the user enables reminders |

A denied permission degrades that one feature; the rest of the app continues to
work. No permission is requested to unlock core functionality.

## 5. AI consent

AI is off by default. Enabling it is explicit and graded (0–3). Lowering the level
immediately disables every AI-derived string in the UI. Because the only provider
is local and deterministic, consent here governs *what is computed*, not *what is
transmitted* — nothing is transmitted.

## 6. Destructive operations

- Deleting a record moves it to trash (soft delete). Permanent delete is a
  separate, confirmed action.
- Room destructive migration is disabled; a schema change without a migration
  fails loudly instead of wiping data.

## 7. Supply chain

Dependency versions are pinned in `gradle/libs.versions.toml`, and an explicit
constraint pins `okio` to a version present in the build cache. No dependency is
resolved from an untrusted repository.

## 8. What is NOT claimed

- No penetration test has been performed.
- No formal cryptographic review: the app relies on OS-level app sandboxing, not
  app-level encryption. Database encryption (e.g. SQLCipher) is not implemented.
- These gaps are listed so that no reader assumes a security property that is not
  actually present.
