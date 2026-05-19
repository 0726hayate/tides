# Tides

An offline-first period tracker for Android. Free, open source, no accounts, no telemetry, no network.

**Status:** v1.0.0 released — May 2026. [Download here](https://github.com/0726hayate/tides/releases/latest).

## About

Tides started as something I built for my girlfriend, who wanted to track her cycle without sending her data anywhere. After looking at what was on the market, we couldn't find an option that actually respected privacy — most apps either sold the data, kept it in someone else's cloud, or both. So I built one that doesn't.

I'm sharing it because anyone should be able to track their cycle without becoming a product. Tides is free, open-source, runs entirely on your device, and will never collect anything from you.

## Install

Tides is distributed as a signed APK directly through GitHub Releases — no app store account needed.

### Option 1: Direct download (one-time install)

1. On your Android phone, open <https://github.com/0726hayate/tides/releases/latest>
2. Tap the `tides-X.Y.Z.apk` asset to download it
3. When Android asks, allow installation from this source (this is a one-time per-source permission on Android 8+)
4. Tap the downloaded file to install

You can verify the download independently: each release is signed with the same key, and the signing certificate's SHA-256 fingerprint is published in the release notes. Mismatched signatures will prevent install — that's intended.

### Option 2: Obtainium (recommended for auto-updates)

[Obtainium](https://github.com/ImranR98/Obtainium) is a free Android app that tracks GitHub Releases and prompts you when new versions are available. It's the closest thing to an "app store" experience for apps distributed outside Play Store.

1. Install Obtainium (also from a GitHub Release)
2. In Obtainium, tap "Add App"
3. Paste this URL: `https://github.com/0726hayate/tides`
4. Obtainium will detect new Tides releases automatically and let you install updates with one tap

### Option 3: F-Droid

(Coming — pending inclusion in F-Droid's catalog. Status: TBD.)

## What Tides is

- **Offline.** Tides has no internet permission. There is no server, no cloud, no telemetry, no analytics. Your data never leaves your device.
- **Encrypted.** Your cycle data is stored in an AES-256-encrypted SQLite database (SQLCipher), locked behind a PIN you set during onboarding. Optional biometric unlock uses Android's standard Keystore — your fingerprint or face never touches the app's data directly.
- **Free.** GPL-3.0 licensed. No ads, no subscriptions, no in-app purchases. If you'd like to support the project, donations are welcome at <https://ko-fi.com/hayate0726> — they're a gift, not a payment.

## What Tides is *not*

- **Tides is not a medical device.** Predictions and patterns shown by Tides are estimates from your own logged data. They are not diagnoses and not medical advice. For questions about your health, please talk to a qualified healthcare professional.
- **Tides is not a contraceptive.** Cycle predictions are not reliable for preventing pregnancy. If you are trying to avoid pregnancy, use a method that is actually designed for that purpose.
- **Tides is not warranted.** Per the GPL-3.0 license, this software is provided "AS IS," without warranty of any kind, express or implied. You use it at your own discretion. The author makes no guarantees about accuracy, completeness, or fitness for any particular purpose.

## Privacy

Short version: nothing leaves your device. See [PRIVACY.md](PRIVACY.md) for the full details.

## Design

See [docs/superpowers/specs/2026-05-15-tides-design.md](docs/superpowers/specs/2026-05-15-tides-design.md) for the full design spec.

Plans live in [docs/superpowers/plans/](docs/superpowers/plans/).

## Building

```bash
./gradlew :app:assembleDebug
```

Requires JDK 17 and Android SDK platform 34 + build-tools 34.

## Testing

```bash
./gradlew :app:testDebugUnitTest                    # JVM unit tests
./gradlew :app:connectedDebugAndroidTest            # Instrumented tests (needs emulator/device)
```

The instrumented suite includes a 23-persona real-world validation suite under `app/src/androidTest/.../validation/` — see [the validation README](app/src/androidTest/java/com/hayate0726/tides/validation/README.md) for what it covers.

## License

GPL-3.0. See [LICENSE](LICENSE).
