# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — Plan 4: Export, Notifications, Release
- **Export (Wave 1).** `ui/export/CsvBuilder.kt`, `ui/export/PdfBuilder.kt` (US-Letter cycle summary), and `ui/export/DoctorPdfBuilder.kt` (FIGO-aligned, black-ink-only doctor PDF using the Android `PdfDocument` API — no third-party PDF dependency).
- **Sharing (Wave 2).** `ui/export/Sharer.kt` wraps `Intent.ACTION_SEND` + a `FileProvider` (`xml/file_provider_paths.xml`, authority `${applicationId}.fileprovider`) so PDFs and CSVs hand off via the system share sheet without ever copying outside app-private cache.
- **Feedback screen.** `ui/feedback/FeedbackScreen.kt` opens the GitHub Issues tracker or `mailto:0726hayate@gmail.com` via system Intents only — no in-app form, no network.
- **Duress PIN setup.** `ui/settings/DuressSetupScreen.kt` + `DuressSetupViewModel.kt`. Rejects collisions with the primary PIN via `KeyDerivation.validatePin` before persisting.
- **Encrypted backup (Wave 3).** `data/BackupExporter.kt` + `data/BackupImporter.kt` implement a real Tides container format: 4-byte `TBAK` magic + version + 16-byte key salt + 16-byte verifier salt + 32-byte Argon2id verifier + SQLCipher payload rekeyed under the backup password via `PRAGMA rekey`. Wrong passwords are rejected at the verifier (constant-time `MessageDigest.isEqual`) before any destination file is written. `ui/settings/BackupScreen.kt` exposes the export/import UI; round-trip tests cover correct password, wrong password, and non-Tides bytes.
- **Notifications (Wave 4).** `notifications/` package: three opt-in reminder types per spec §5.12 (PERIOD_PREDICTED at -3 days, PERIOD_START on the predicted day, LATE_PERIOD at +3 days). All toggles default off. `ReminderScheduleCalculator` is pure Kotlin and unit-tested; `ReminderScheduler` arms `AlarmManager.setAndAllowWhileIdle` per type. Notification title and lock-screen visibility are baked into the `PendingIntent` extras at schedule time from the active `ThreatPreset` so the receiver works while the app is locked. `ui/settings/NotificationsScreen.kt` exposes three toggles and a system-permission rationale row.
- **Release pipeline (Wave 5).** `app/build.gradle.kts` reads signing config exclusively from environment variables — no keystore is ever checked in, and F-Droid maintainer builds run unsigned for downstream signing. `.github/workflows/release.yml` triggers on `v*` tags, decodes a base64 keystore from a GitHub secret, runs the same `aapt2 dump permissions` audit as CI on the release APK, and posts the signed artifact to GitHub Releases.
- **F-Droid metadata.** `fastlane/metadata/android/en-US/` directory with `title.txt`, `short_description.txt`, `full_description.txt`, and per-version changelogs.

### Added — v1.1 follow-ups
- **Bottom navigation + Settings nav graph.** `MainHost` becomes a Scaffold with three tabs (Calendar / Stats / Settings); five Settings sub-routes (Reminders, Backup, Duress, privacy preset, Feedback) wire up the per-feature screens shipped in Plan 4. Day-tap on the calendar opens `LogBottomSheet` via `ModalBottomSheet`; save refreshes the calendar and republishes the widget snapshot.
- **Backup restore.** Full spec §5.8 flow: SAF picker → primary PIN + backup password → confirm dialog → `BackupViewModel.restore()` validates both passwords, extracts the rekeyed payload, `PRAGMA rekey`s it back to the live primary key, and hands the file to `AppViewModel.replaceDatabaseFile()` for a mutex-guarded atomic swap (NIO `ATOMIC_MOVE` with copy+delete fallback). AuthMeta and primary PIN are unchanged — the user re-unlocks with their existing PIN. Instrumented test covers the rekey path end-to-end.
- **Discreet Glance widget (spec §5.13).** `widget/TidesDiscreetWidget` renders the current cycle-day number on a neutral background; tap opens the app. Reads from `widget_summary.bin` (17 bytes: magic + version + cycle day + timestamp) under `filesDir`, written by `WidgetUpdater` from inside `CalendarViewModel.refresh()`. The "Normal" variant (cycle day + predicted date) remains deferred — adding it expands the summary file's leak surface and needs a Settings variant picker. Glance transitively merges `INTERNET`-adjacent permissions (`ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `USE_FINGERPRINT`); all five are stripped at manifest-merge time via `tools:node="remove"` so the release APK still declares only `USE_BIOMETRIC` / `VIBRATE` / `POST_NOTIFICATIONS`. Duress-wipe now deletes `widget_summary.bin`.

### Deferred to v1.2
- Glance "Normal" variant (cycle day + predicted date). Needs a Settings variant picker and expands the unencrypted summary file's content.

### Added — Plan 3: Core UI
- Material 3 theme system under `ui/theme/`: CVD-safe color palette (`TidesColors`), 12 typography styles, Shapes, and the root `TidesTheme` composable. Light palette = cream + warm tones; dark = black + warm reds.
- `ui/theme/Glyphs.kt`: shape primitives — `DropGlyph` (period), `DiamondGlyph` (ovulation), `DashedBar` (predicted period) — used by the calendar for shape-redundant marks readable under deuteranopia / protanopia / tritanopia.
- App-level state machine: `AppState` (Onboarding / Locked / LockedCooldown / Unlocked / UnlockedDecoy) and `AppViewModel` (Hilt) that inspects `auth_meta.bin` at startup, calls `LockManager.attemptUnlock`, and routes unlock results into state transitions including duress decoy and wipe paths.
- `ui/nav/Routes.kt` + `ui/nav/TidesNavHost.kt`: Compose Navigation root that observes `AppState` and routes between Onboarding, Lock, and Main destinations.
- Lock screen (`ui/lock/`): `PinKeypad`, `LockScreen`, `LockViewModel`. Biometric callback hook is in place; full `BiometricPrompt` integration is deferred to Plan 4.
- Onboarding flow (`ui/onboarding/`, 6 screens + a completion screen): Welcome → Goals → PinSetup → Biometric → ThreatPreset → LastPeriod → OnboardingComplete. `OnboardingViewModel` is shared across screens via a Hilt-scoped back-stack entry; `complete()` does Argon2 derive + SQLCipher DB open on `Dispatchers.IO`.
- Calendar (`ui/calendar/`): `CalendarScreen` + `CalendarMonth` with weekday header, month chevrons, and per-day cells. `DayCell` uses the Glyphs primitives for shape-redundant period / predicted-period / ovulation / symptom marks. `CalendarViewToggle` switches between ALL / PERIOD_ONLY / PHASES / SYMPTOMS modes. `PhaseCard` shows current `Phase` + fertile-window dates when `PhaseCalculator` returns a non-null result.
- Log sheet (`ui/log/`): `LogBottomSheet` content composable with `FlowPicker` (5-button SegmentedButton) and `SymptomPicker` (chips grouped by `SymptomCategory`). `LogViewModel.save()` upserts a `CycleEntryEntity` and inserts the selected `SymptomEntryEntity` rows.
- Stats (`ui/stats/`): `StatsScreen` with `InsightCard` (synthesized one-line summary), `CycleLengthChart` (Compose-Box bars), and `SymptomFrequencyList`. `InsightGenerator` consumes the renamed `cycleLengthRange` field from `CycleStats`.
- Settings (`ui/settings/`): minimal `SettingsScreen` placeholders (privacy preset, lock now, check for updates, send feedback, Ko-fi link). Full backup / export / theme override come in Plan 4.

### Changed — Plan 3
- `MainActivity` now hosts `TidesNavHost` under `TidesTheme` instead of the Plan 1 placeholder.
- `OnboardingFlowTest` instrumented test is `@Ignore`d for now; it asserts the post-onboarding Main route renders the Calendar with "Cycle day" text, which requires the unlocked-DB wiring deferred to Plan 4. All other instrumented tests pass (22/22).

### Added — Plan 2: Data & Domain
- `domain/model/` leaf types: `FlowIntensity`, `Symptom` (curated 8-category taxonomy + `OTHER` freetext), `BirthControlMethod`, `Goal`, `CalendarView`, `ThreatPreset`, `CycleDay`, `PredictionRange`, `Cycle`, `Phase`
- `domain/` pure-Kotlin logic (no Android imports):
  - `CycleDetector` — period-day entries → `List<Cycle>`
  - `CycleStats` — median cycle/period length, range, regularity bucket, period-length trend
  - `CyclePredictor` — next-period `PredictionRange` with `Confidence`
  - `PhaseCalculator` — current `Phase` + fertile window (clinical fixed-luteal formula `ovulationDay = medianLength - 14`, ±2-day window, suppressed for cycles outside 21..35d)
  - `SymptomStats` — frequency map + cycle-day heatmap (OTHER excluded from aggregates)
  - `FigoAnalysis` — FIGO System 1 pattern detection (frequency, regularity, duration, volume, dysmenorrhea, amenorrhea, intermenstrual bleeding) per ACOG #651 / Munro 2018
- `data/entity/`: real Room schema replacing the Plan 1 placeholder — `CycleEntryEntity`, `SymptomEntryEntity`, `BirthControlEntity`, `SettingsEntity`, `GoalEntity`
- `data/dao/`: one-shot suspend + reactive `Flow` reads, `@Upsert/@Insert/@Update/@Delete` writes for every entity
- `data/Converters.kt`: `LocalDate` ↔ epoch day, plus enum ↔ string converters
- `data/CycleRepository.kt`: the single seam between DAOs and domain types
- Room schema exported to `app/schemas/com.hayate0726.tides.data.TidesDatabase/1.json` (committed; future migrations diff against it)

### Changed — Plan 2
- Plan 1 instrumented tests updated to write/read real `CycleEntryEntity` rows instead of the deleted `Placeholder` entity. All 11 instrumented tests still pass.

### Added — Plan 1: Foundation & Crypto
- Project scaffold: Gradle 8.9, Kotlin 2.0, AGP 8.5, Hilt, Compose, Room + SQLCipher (`sqlcipher-android` 4.5.4), argon2kt 1.6.0
- `crypto/` package:
  - `DbKey` and `Pin` value types with explicit `zero()` lifecycle
  - `Argon2` wrapper (64 MiB / 3 iter / 1 thread, 32-byte output, Argon2id)
  - `KeyDerivation` (`deriveKey`, `derivePinHash`, `validatePin`)
  - `AuthMeta` fixed-size binary file format (147 bytes, magic + version + salts + hashes + duress slot + fail counter + cooldown)
  - `KeystoreWrapper` (AES-256-GCM wrap/unwrap, biometric-bound or app-bound)
  - `FileAuthMetaStore` persistence
- `lock/` package: `LockManager` with rate-limited PIN attempts (30s → 60s → 120s → … capped at 1 h) and duress detection; `LockState` sealed hierarchy
- `data/` package: SQLCipher-backed Room placeholder schema (real entities arrive in Plan 2)
- Hilt modules wiring crypto and data
- Security tests:
  - No `INTERNET` or `ACCESS_NETWORK_STATE` declared in the APK
  - Derived DB key never appears in any app-private file
  - Logcat never leaks PIN or row content
  - 200 random wrong PINs never unlock
  - Rate-limit blocks the correct PIN during cooldown
- GitHub Actions CI: builds, runs unit tests, audits APK permissions on every PR; runs instrumented tests on `main` pushes

### Privacy / security guarantees locked
- AndroidManifest has no `INTERNET` permission. Static aapt2 check in CI enforces this.
- Database key is derived from the user PIN via Argon2id and is never written to disk in clear.
- No telemetry, no analytics, no accounts, no cloud sync.
