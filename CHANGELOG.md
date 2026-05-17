# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
