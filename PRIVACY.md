# Tides Privacy

**Tides collects no data. None. From anyone.**

## What this means in practice

- **No internet.** Tides has no `INTERNET` permission in its Android manifest. The app cannot make network requests. You can verify this yourself by installing the APK and checking its permissions, or by reading `app/src/main/AndroidManifest.xml` in the source.
- **No server.** There is no Tides server. There is no Tides cloud. There is no Tides account.
- **No analytics.** No crash reporting, no usage telemetry, no "anonymous statistics," no third-party SDKs that phone home.
- **No backups to anyone but you.** If you create an encrypted backup via Settings → Backup, the encrypted file stays on your device until you choose to share it (with yourself, via your own files app). Tides does not upload it anywhere.

## What lives on your device

Tides stores your logged data — cycle entries, symptoms, settings — in an AES-256-encrypted SQLite database (SQLCipher), locked behind a PIN you set during onboarding. The encryption key is derived from your PIN using Argon2id and (optionally) wrapped with a biometric-bound key in the Android Keystore.

If you uninstall Tides, the database is deleted along with the app. The Keystore-held key goes with the uninstall too. There is no copy of your data anywhere else.

## Permissions Tides requests

- `USE_BIOMETRIC` — to support fingerprint/face unlock, if you turn it on.
- `VIBRATE` — for notification feedback.
- `POST_NOTIFICATIONS` — to deliver the period reminders you opt into (also entirely on-device, no remote scheduling).

That is the complete list. There is no permission for internet, external storage, location, contacts, calendar, or anything else.

## What Tides does not have

- No accounts, no logins, no email, no phone number — Tides doesn't know who you are.
- No partner-sharing feature that uploads data anywhere.
- No "anonymized research data" opt-in that secretly is data collection.
- No way for me, the developer, to access your data. Even if I wanted to. The encryption key never leaves your device.

## Verifying these claims

Tides is open-source under GPL-3.0. You can read the full source, audit `AndroidManifest.xml` for the permission list, and check the network behavior with any standard Android inspection tool. If something in the code doesn't match what's written here, please open an issue.

## Changes

This privacy policy will change only if the app's data-handling behavior changes. The git history of this file is the public log of any such change. As of today, no data handling exists to describe — there is nothing to log.
