# Tides

An offline-first period tracker for Android. Free, open source, no accounts, no telemetry, no network.

**Status:** in development. v0.1.0-dev. Not yet ready for users.

## Design

See [docs/superpowers/specs/2026-05-15-tides-design.md](docs/superpowers/specs/2026-05-15-tides-design.md) for the full design spec.

Plans live in [docs/superpowers/plans/](docs/superpowers/plans/).

## Privacy

- The app's manifest does not declare `INTERNET`. The app cannot make a network call.
- All cycle data is stored in a single SQLCipher-encrypted database on the device.
- The encryption key is derived from your PIN via Argon2id and (optionally) wrapped with a biometric-bound key in the Android Keystore.
- There is no account, no telemetry, no analytics, no cloud sync.

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

## Donations

If you find Tides useful and want to support development: <https://ko-fi.com/hayate0726>

The app itself contains no IAP and will never ask for money.

## License

GPL-3.0. See [LICENSE](LICENSE).
