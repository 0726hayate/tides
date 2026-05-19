# Persona Validation Suite

This package exercises Tides against 21 realistic user histories drawn from published cycle research (13 personas synthesized from distribution parameters) and clinical case reports (8 hand-transcribed personas). Tests run on emulator via:

```bash
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.package=com.hayate0726.tides.validation
```

Suite size: 21 personas × (6 universal + 2 hormonal-IUD-conditional) property assertions = 168 instrumented test cases, ~37 of which are `Assume`-skipped (the IUD-conditional ones for non-IUD personas).

## Files

- `PersonaSpec.kt` — declarative spec shape (distribution params, BC, goals, trailing-amenorrhea pad).
- `Persona.kt` — the realized history (cycle + symptom rows, BC, goals).
- `PersonaGenerator.kt` — deterministic synthesis from spec + seed (log-normal cycle sampling, Box-Muller).
- `SyntheticPersonas.kt` — 13 synthetic specs across 4 population groups (typical, irregular, life transitions, special — including 4 hormonal-IUD variants).
- `CaseReportPersonas.kt` — 8 hand-built personas with citations to published case reports or ACOG bulletins.
- `AllPersonas.kt` — the union, used by the `@Parameterized` test class.
- `PersonaTestHarness.kt` — ephemeral test-DB lifecycle (open with persona data, close + cleanup).
- `PersonaScenarioTest.kt` — 6 universal + 2 hormonal-IUD-conditional property tests.

## Data sources

The persona parameters are drawn from the research doc at:

```
docs/superpowers/specs/research/2026-05-18-cycle-distributions.md
```

That doc surveys cycle length / period length / anovulation rate / common symptoms per population segment, with citations. When adding or tuning a persona, update its parameters consistently with that research; if a parameter has no clear research basis, note it in the persona's KDoc.

## Adding a new persona

For a synthetic persona:

1. Add a new `PersonaSpec(...)` entry to `SyntheticPersonas.all`.
2. Pick a `PopulationSegment` (or add a new enum value if no segment fits).
3. Set `cycleLengthDays` / `periodLengthDays` / `anovulationRate` / `flowDistribution` / `symptomPrevalence` from the research doc.
4. If the persona represents a life transition (BC started or stopped mid-history), set `bcStartedMonthsAgo`.
5. If the persona must trigger FIGO AMENORRHEA deterministically, set `trailingAmenorrheaPadDays = 100`.
6. Run `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hayate0726.tides.validation.PersonaScenarioTest` and verify all 6 universal property tests pass for the new persona. If a property fails, decide whether the persona definition is wrong (over-aggressive distribution, missing goals/BC) or you've found a real bug. Don't weaken the assertion to make a persona pass.

For a clinical case report:

1. Find a published case (or ACOG Practice Bulletin example) for the scenario.
2. Add a `Persona(...)` instance to `CaseReportPersonas.all` with the bleeding-day dates from the source.
3. Add a KDoc block above the val citing the source (DOI, PMC ID, or bulletin number).
4. Normalize the dates so the most recent bleeding event falls within ~12 months of `today = LocalDate.of(2026, 5, 1)`.

## Snapshot tests

The design spec originally planned byte-exact snapshot tests of `widget_summary.bin` for 4 representative personas, to lock the binary format. During implementation we discovered the format encodes `updatedAtEpochMs = System.currentTimeMillis()` and date-derived fields driven by `LocalDate.now()` — neither deterministic from persona inputs alone. The snapshot tests were dropped because:

- Property 5 (`widget_summary_matches_privacy_gate`) already asserts the security-sensitive `showOvulation` field round-trips correctly.
- Property 8 (`hormonal_iud_empty_history_renders_without_crash`) asserts the amenorrhea-persona widget has `predictedPeriodStartEpochDay == null` and `showOvulation == false`.
- Format-version changes live in `WidgetSummary.kt`'s schema-version constant; any change is visible in code review.

If a future refactor injects a `Clock` into `WidgetUpdater`, byte-exact snapshots become trivially achievable and can be revisited. See spec §7.2 for the design tradeoff.
