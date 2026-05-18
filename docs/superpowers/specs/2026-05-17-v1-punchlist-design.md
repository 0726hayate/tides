# Tides v1.0 punch-list — design

**Status:** Draft for user review — 2026-05-17
**Scope:** Close the gap between the 2026-05-15 main design spec and what's shipped, so we can tag v1.0. Five sequential commits.

This document inherits everything from `2026-05-15-tides-design.md`. It only resolves the open design questions that follow-up work surfaced.

---

## Commit 1 — Biometric unlock end-to-end

**Goal:** Wire the existing `BiometricController` + `KeystoreWrapper` into the actual unlock path.

### Wrapped-key blob format

Stored at `filesDir/biometric.bin` (separate from `auth_meta.bin` so corruption / invalidation of one doesn't trash the other). Fixed-size, app-private, MODE_PRIVATE.

```
bytes  0..3   magic "TBIO"
byte   4      version 0x01
bytes  5..    KeystoreWrapper.wrap() output (12-byte IV || ciphertext || GCM tag)
              plaintext = the 32-byte DB key
```

Keystore alias: `tides.biometric.v1`. Created with `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)` (the existing `KeystoreWrapper.wrap(alias, requireBiometric = true, ...)`).

### Setup flow (eager — option A)

`OnboardingViewModel.complete()` now does, conditional on `draft.biometricEnabled`:

1. Derive DB key from PIN (already happens).
2. Open the DB with the key (already happens).
3. Wrap the key bytes under the biometric alias via `KeystoreWrapper.wrap(...)`.
4. Write `biometric.bin` with magic + version + wrapped blob.
5. Zero the key (already happens).

If biometric hardware is unavailable on this device, silently skip steps 3-4 even if the user enabled the switch — the wrap call will fail and we don't want onboarding to fail. The Settings toggle later will re-attempt.

### Unlock flow

`LockHost` currently passes `onBiometric = null`. Change to:

1. On enter, check `BiometricController.availability(activity)` AND `biometric.bin` exists. If both yes, show the biometric prompt button and auto-trigger it on first composition.
2. On biometric success: load `biometric.bin`, call `KeystoreWrapper.unwrap(alias, blob)` → DB key → `DatabaseFactory.open` → transition to `AppState.Unlocked`. Bypasses `LockManager` (no failure counter for biometric — it has its own lockout).
3. On `KeyPermanentlyInvalidatedException` (re-enrolled fingerprints): delete `biometric.bin`, delete the alias, surface a transient `unlockError` ("Biometric unlock was disabled because your fingerprints changed; re-enable in Settings"), fall back to PIN entry.
4. On user-cancel or other error: surface error, fall back to PIN entry.

### Settings: biometric toggle

In Settings → Privacy, add "Biometric unlock" row showing current state. Tapping:
- If currently off and hardware available → prompt for primary PIN, derive key, wrap it, write `biometric.bin`. Same as onboarding setup steps 3-4.
- If currently on → delete `biometric.bin`, delete alias.

### Duress wipe + lock state

`AppViewModel.handleDuress` (WIPE mode) and `replaceDatabaseFile` already wipe the relevant files. Add `biometric.bin` deletion + `KeystoreWrapper.deleteKey("tides.biometric.v1")` to both.

### Files touched

- New: `crypto/BiometricKeyStore.kt` (writes/reads `biometric.bin`, owns the alias name)
- `OnboardingViewModel.kt` (call BiometricKeyStore.enroll after open)
- `LockHost.kt` + `LockViewModel.kt` (biometric trigger + unwrap → unlock)
- `AppViewModel.kt` (biometric unlock path + duress cleanup)
- `ui/settings/BiometricToggleScreen.kt` + nav route
- Manifest: nothing new (USE_BIOMETRIC already declared)

### Tests

Instrumented: `BiometricKeyStoreTest` — enrollment writes a valid blob; unwrap on emulator with no biometric should throw `KeyPermanentlyInvalidatedException` (or `UserNotAuthenticatedException` — accept either as "needs auth"). The actual biometric-auth round trip can't be unit-tested without a real device's biometric prompt; flag as manual-test for v1.0 release.

---

## Commit 2 — Stats polish

**Goal:** Fill in spec §5.6 features that StatsScreen has skeleton wiring for but doesn't deliver.

### Range selector

State: `StatsViewModel.range: StateFlow<Range>` where `Range = THREE_MO | SIX_MO | ONE_YR | ALL`. Default `SIX_MO` (most stable for the typical user with 3-12 cycles logged).

`setRange(range)` triggers a fresh `refresh(range)` that reads `cycle_entries` and `symptom_entries` from the corresponding date window. `ALL` = `LocalDate.MIN..now`.

UI: `SegmentedButton` row at the top of the screen, above InsightCard.

### Share PDF / CSV

The TODOs in `StatsRoute`'s `onExportPdf` / `onExportCsv` callbacks. Wire:

- `onExportPdf` → build `DoctorPdfBuilder` (FIGO) bytes from the current range's stats + `Sharer.sharePdf(ctx, bytes, "tides-cycle-summary-$ts.pdf")`
- `onExportCsv` → `CsvBuilder.build(cycles, symptoms)` → `Sharer.shareCsv(ctx, csv, "tides-export-$ts.csv")`

Both buttons live at the bottom of StatsScreen. Disabled while no data exists.

### Symptom-cycle heatmap

Existing `SymptomStats.cycleDayHeatmap` already computes it. New composable `SymptomHeatmap` renders one row per top-6 symptom; each row is a horizontal series of squares (one per cycle day, opacity = log-scaled count). Shape redundancy via outline thickness on cells with `count > 0`.

Hidden behind an expandable section ("Details") to avoid cluttering the main view.

### Period-length trend sparkline

New `PeriodLengthTrend` composable consuming `cycles.mapNotNull { it.periodLength }`. Compose `Canvas`-based: dots + a line. Below the existing CycleLengthChart, also in the "Details" expansion.

### "What does this mean?" info buttons

Small `?` IconButtons next to: `Avg cycle`, `Avg period`, `CYCLE LENGTH` chart header, regularity bucket label. Tap → `AlertDialog` with the plain-language explanation from spec §5.6 ("FIGO defines a cycle as irregular if shortest-to-longest varies by more than 7 days." etc.). Strings co-located with the screen.

### Files touched

- `ui/stats/StatsViewModel.kt` — add `range` state, parameterized `refresh(range)`
- `ui/stats/StatsScreen.kt` — range selector, info buttons, share buttons, details expansion
- New: `ui/stats/SymptomHeatmap.kt`, `ui/stats/PeriodLengthTrend.kt`
- `ui/nav/MainNavGraph.kt` — wire the share callbacks (currently TODOs)

### Tests

Unit (JVM): `StatsViewModelTest` for range-window date math. The composables themselves don't need new tests — the underlying domain stats are already unit-tested.

---

## Commit 3 — Goal suppression + BirthControl edit + Glance Normal variant

### Goal-based ovulation/fertile-window suppression

Spec §5.1: suppress ovulation/fertile-window UI when:
- goals do NOT include `AVOID_PREGNANCY` or `TRYING_TO_CONCEIVE`, OR
- active birth control method is hormonal (pill / hormonal-iud / implant / patch / ring)

**Source of truth:** new `data/UserPrivacyView.kt` — a `data class` with `showOvulation: Boolean`. Computed from `GoalDao.all()` + `BirthControlDao.activeOnce()` once at unlock time, exposed as `StateFlow<UserPrivacyView>` on a new `@Singleton UserPrivacyRepository` that's invalidated when settings change (manual `refresh()` call from BirthControl edit and from a future goals-edit screen).

Consumers:
- `CalendarViewModel` — reads `showOvulation` and only computes/emits `phase` and `ovulationWindow` when true. Calendar passes null to `CalendarMonthState.ovulationWindow` when suppressed.
- `StatsScreen` — hides fertile-window-related rows (none yet, future-proofing).
- `TidesDiscreetWidget` — unaffected (shows only cycle day).
- `TidesNormalWidget` — gates the "fertile in X days" line on `showOvulation`.

### BirthControl edit screen

`Settings → Privacy → Birth control` row. Reads `BirthControlDao.activeOnce()`. Form: radio list of `BirthControlMethod` enum values, optional start-date picker (defaults to today). Save inserts a new row with `start_date = today, end_date = null` AND sets `end_date = today.minusDays(1)` on the previously-active row (the schema is append-only history per `2026-05-15-tides-design.md` §4).

After save, calls `UserPrivacyRepository.refresh()` so the calendar/widget pick up the change.

### Glance Normal variant

Second `GlanceAppWidget` subclass `TidesNormalWidget` with its own `tides_normal_widget_info.xml`. Renders:
- Cycle day (large)
- Predicted next-period date (small, "in N days" or "M-D" format)
- Fertile window indicator — only when `UserPrivacyView.showOvulation` was true at last `WidgetUpdater.publish()` call

`WidgetSummary` format extends to v2 (the existing v1 is still readable for old summary files). New layout:

```
bytes  0..3    magic "TWGT"
byte   4       version 0x02
bytes  5..8    cycle day (int32)
bytes  9..16   updated-at epoch ms (int64)
bytes 17..24   predicted period start epoch day (int64; Long.MIN_VALUE = unknown)
byte   25      showOvulation flag (0x00 / 0x01)
bytes 26..33   ovulation date epoch day (int64; Long.MIN_VALUE = unknown)
```

Reader accepts both v1 and v2 (older blobs have no prediction).

**Variant choice:** both widgets are independent `AppWidgetProvider` entries. The user picks at widget-add time via the system widget picker — no Settings toggle needed. This is how Android handles variant widgets natively (cf. Google Calendar's multiple widget sizes); reinventing it in app Settings would just add a layer that doesn't change behavior.

### Files touched

- New: `data/UserPrivacyView.kt`, `data/UserPrivacyRepository.kt` (Hilt singleton)
- `ui/calendar/CalendarViewModel.kt` — consume the repo
- New: `ui/settings/BirthControlScreen.kt` + `BirthControlViewModel.kt` + nav route
- `widget/WidgetSummary.kt` — v2 format with backward-compat read
- `widget/WidgetUpdater.kt` — pass `UserPrivacyView` snapshot when writing
- New: `widget/TidesNormalWidget.kt` + `tides_normal_widget_info.xml`
- Manifest — second `<receiver>` for the Normal widget
- `domain/PhaseCalculator.kt` — no changes (suppression is a UI-layer concern)

### Tests

- Unit: `UserPrivacyViewTest` (compute from goals + birth_control combinations per spec §5.1)
- Unit: `WidgetSummaryTest` extended for v2 round-trip + v1-read compatibility

---

## Commit 4 — PIN lifetime hardening + test re-enable

### PIN CharArray plumbing

Reality check: Compose `TextField(String, (String)->Unit)` forces PIN chars through the JVM string pool during entry. Cannot eliminate without a custom text-input, which is out of scope. Mitigation: minimize lifetime.

Changes:
- `OnboardingViewModel.DraftState.pin: String` → `pinChars: CharArray?`. Setter `setPin(pin: String)` copies to a fresh `CharArray` and overwrites the local String reference (no further String reference held). `complete()` runs Argon2 using a `Pin(pinChars)`, then `pinChars.fill(0)` and sets `pinChars = null`.
- `LockViewModel`, `BackupViewModel`, `DuressSetupViewModel` already follow this pattern at their API boundaries — no changes.
- Document the JVM-pool limitation in a `/* design note */` comment near `OnboardingViewModel.DraftState`.

### Re-enable OnboardingFlowTest

Currently `@Ignore`d. The unlocked-DB wiring it was waiting for landed in c5d9548. Remove `@Ignore`, update its assertions to match the current `MainHost` → calendar bottom-nav rendering (assert on "Calendar" tab text or visible day grid, not the old "Cycle day" string).

### HiltAndroidRule for instrumented tests

Add `@HiltAndroidTest` + `HiltAndroidRule` to `OnboardingFlowTest` and `BackupRoundTripTest`. Lets them resolve injected singletons (`FileAuthMetaStore`, etc.) instead of constructing manually — matches the production graph and catches DI misconfigurations.

`@HiltAndroidApp` is already on `TidesApplication`. Need to add the test runner glue: `androidx.test.runner.AndroidJUnitRunner` → custom subclass that uses `HiltTestApplication`. Standard Hilt-on-Android-tests setup.

### Files touched

- `ui/onboarding/OnboardingViewModel.kt`
- `androidTest/.../OnboardingFlowTest.kt`
- `androidTest/.../BackupRoundTripTest.kt`
- New: `androidTest/.../HiltTestRunner.kt`
- `app/build.gradle.kts` — `testInstrumentationRunner` swap + `hiltAndroidTesting` test dep

---

## Commit 5 — Accessibility + TopAppBar consistency + `useDynamicColor` toggle

### Accessibility pass

Coverage:
- `contentDescription` on every `Icon`, `Image`, and decorative shape where the surrounding text alone doesn't communicate purpose. Each `DropGlyph` / `DiamondGlyph` / `DashedBar` in `Glyphs.kt` already represents semantic state — add a `contentDescription` parameter.
- 48dp minimum touch target audit. Specifically: `PinKeypad` digit buttons (currently ~44dp), `CalendarMonth` day cells (might be <48dp on small screens), backspace icon in PinKeypad.
- `Modifier.semantics { contentDescription = ... }` on `DayCell` so TalkBack reads "May 7, period day 2 of 5" instead of just "7".

### TopAppBar consistency

Every Settings sub-screen currently uses a bare `Text(headline)` for its title. Standardize: each sub-screen wraps content in `Scaffold(topBar = { TopAppBar(title = ..., navigationIcon = back-arrow) })`. Back arrow calls `nav.popBackStack()`. Affects: NotificationsScreen, BackupScreen, DuressSetupScreen, BirthControlScreen, BiometricToggleScreen, ThreatPresetRoute, FeedbackScreen.

### `useDynamicColor` toggle

Settings → Appearance → "Match system colors" switch. Default off (spec §6 preference). When on, `TidesTheme(useDynamicColor = true)`.

Settings copy below the toggle:

> Tides uses a color-blind-safe palette by default. Cycle marks use shape (drops, diamonds, dashes) so they're readable on any palette, including your system colors.

Stored in SharedPreferences (`tides_appearance` file, `use_dynamic_color` bool), read by a small `@Singleton AppearanceRepository` that exposes a `StateFlow<Boolean>`. `MainActivity` reads it and passes to `TidesTheme` at composition root.

### Files touched

- `ui/theme/Glyphs.kt` — `contentDescription` parameters
- `ui/calendar/DayCell.kt` — semantics
- `ui/lock/PinKeypad.kt` — touch-target size, contentDescription on backspace
- All Settings sub-screens — TopAppBar wrap
- New: `ui/settings/AppearanceScreen.kt` + `AppearanceRepository` + nav route
- `MainActivity.kt` — observe AppearanceRepository, pass to TidesTheme

### Tests

No new tests — accessibility correctness is verified by manual TalkBack pass before v1.0 tag. Add to release checklist (separate file).

---

## Out of scope (left alone per user direction)

- Pregnancy mode
- Stealth mode (disguised icon)
- Partner sharing
- Custom user-defined free-form symptoms
- INTERNET permission / any network capability

These remain "v2 or never" per `2026-05-15-tides-design.md` §10.

---

## Approval

Commits land in the listed order: 1 → 2 → 3 → 4 → 5. Each commit must keep `:app:testDebugUnitTest` green and the CI permission audit clean (no INTERNET, no ACCESS_NETWORK_STATE, no FOREGROUND_SERVICE, no BOOT_COMPLETED) — including after Glance dep changes in commit 3.

Author: hayate0726 (via Claude)
