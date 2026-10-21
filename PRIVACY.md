# Privacy

## The one-line version

LifeOS has no account, no server, and no analytics. Your data stays on your
device unless *you* explicitly export it.

## What is collected

Everything the app stores is data **you entered yourself**:

- notes, tasks, habits (and their completion history)
- expenses (amount in minor units + currency code)
- diary entries (with an optional mood)
- captures: photo/video/audio files, free-text thoughts, mood scores, and — only
  when you tap capture-location — a latitude/longitude and an optional place label
- a timeline of events derived from the above

No usage telemetry, no crash reporting SDK, no advertising identifier, no device
fingerprinting.

## Where it is stored

- Structured data: a SQLite database in the app's private storage.
- Media: app-private storage (when `core/media` lands; see `PROJECT_AUDIT.md`).
- Settings and AI consent: local `DataStore` preferences.

Private app storage is not readable by other apps on a non-rooted device.

## What leaves the device

- **Nothing, automatically.** The app makes no network requests and contains no
  remote AI provider.
- **Only what you export.** The Settings → Data section builds an export on-device
  and opens the Android share sheet. From that point the destination app (mail,
  cloud drive, chat) handles the data under *its* privacy policy, not LifeOS's. You
  choose whether to share at all.

## Permissions

Permissions are requested only when the matching feature is used, and denying one
does not break the rest of the app:

- Camera — only when you capture a photo or video
- Microphone — only when you capture audio
- Location — only when you capture a location
- Notifications — only when you enable reminders

## AI

"LifeOS AI" is a **local, deterministic, rule-based** provider. It computes
extractive summaries, keyword tags and suggestions entirely on the device from
data already stored there. Nothing is sent anywhere. AI is off by default and can
be turned down or off at any time, which immediately disables every AI-derived
string in the UI.

## Your controls

- Delete any record → it moves to Trash and can be restored.
- Delete permanently → an explicit, separate, confirmed action.
- Export → you own a portable copy (`lifeos-export`, formatVersion 1).
- Turn AI off → nothing is computed.

## Children and sensitive data

Because a private diary and mood log can contain sensitive personal information,
the app is designed to minimise exposure: local-only storage, no sharing by
default, and an explicit export action. There is no cloud sync to opt out of,
because there is no cloud.
