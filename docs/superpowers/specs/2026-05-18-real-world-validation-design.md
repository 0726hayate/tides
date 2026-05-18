# Tides v1.0 — Real-World Validation Suite (Design)

**Status:** Draft for implementation
**Date:** 2026-05-18
**Goal:** Catch edge-case bugs in Tides' prediction, suppression, and stats logic that pure-unit tests miss, by running the app against ~18 realistic user personas drawn from published cycle research and clinical case reports.

## 1. Motivation

Tides' domain logic (`PhaseCalculator`, `CyclePredictor`, `CycleDetector`, `FigoAnalysis`, `CycleStats`, `SymptomStats`) is unit-tested with synthetic, well-formed inputs. Real users present messier histories: PCOS with 60-day cycles, perimenopause with skipped months, athletes with 6-month amenorrhea, fresh starts after stopping hormonal birth control. This suite layers realistic-persona scenarios on top of the existing unit tests to expose surprises before v1.0 ships.

## 2. Deliverable shape

A permanent instrumented test module in `app/src/androidTest/java/com/hayate0726/tides/validation/` that:

- Defines 18 personas (12 generated from published distributions, 6 hand-transcribed from clinical case reports).
- Runs each persona through the full app stack (DB → DAO → Repository → ViewModel → snapshot).
- Asserts 6 behavioral properties per persona, plus 3 widget-binary snapshot tests.
- Runs on emulator via `connectedDebugAndroidTest`; runtime ~6 minutes.

Personas and assertions are deterministic — seeded RNG, fixed snapshot fixtures. CI failures are reproducible from a persona id + seed alone.

## 3. Test surface

**Full app, instrumented.** Each persona's data is inserted into a real Room/SQLCipher DB, observed via `CalendarViewModel` + `StatsViewModel` + `UserPrivacyRepository`, and asserted on the resulting state. This catches DAO query bugs, Hilt wiring issues, and cross-feature inconsistency (e.g., widget vs in-app showOvulation drift) that pure-domain tests miss.

Rejected alternatives:
- *Pure-domain JVM tests* — fastest but misses integration surface.
- *Tiered (JVM + small instrumented set)* — better iteration speed but the user explicitly chose full instrumented coverage for completeness.

## 4. Persona catalog

Eighteen personas across four population groups:

### Group 1 — Typical adult (3)
| Id | Age | Cycle | Period | History | BC | Goals |
|---|---|---|---|---|---|---|
| `TYPICAL_25` | 28 | 28-30d regular | 4-6d | 18mo | none | none |
| `TYPICAL_35` | 35 | 26-32d mild variability | 4-5d | 24mo | none | TRACK_CYCLE |
| `TYPICAL_TTC` | 30 | 27-30d regular | 5d | 12mo | none | TTC |

### Group 2 — Irregular (3)
| Id | Age | Cycle | Period | History | BC | Goals |
|---|---|---|---|---|---|---|
| `PCOS_LONG` | 26 | mean 52d, sd 14d, 50% anovulatory | 4-7d | 12mo | none | TTC |
| `PERIMENOPAUSE_47` | 47 | mean 32d, sd 12d, occasional skip | 3-7d variable | 18mo | none | TRACK_CYCLE |
| `ADOLESCENT_15` | 15 | mean 35d, sd 14d, 70% anovulatory | 3-6d | 14mo | none | none |

### Group 3 — Life transitions (3)
| Id | Age | History detail | BC | Goals |
|---|---|---|---|---|
| `POSTPARTUM_RESUMED` | 32 | 9mo amenorrhea → 2 cycles (38d, 40d) | none | TRACK_CYCLE |
| `BC_STOPPED_3MO` | 27 | 12mo combined-pill clockwork → 3mo 32-45d cycles | none (stopped) | TTC |
| `BC_STARTED_6MO` | 24 | 12mo natural 28-31d → 6mo combined-pill withdrawal bleeds | COMBINED_PILL | none |

### Group 4 — Special (3)
| Id | Age | History detail | BC | Goals |
|---|---|---|---|---|
| `HORMONAL_IUD_MIRENA` | 33 | 18mo on Mirena, 0-2 bleed events per 6mo, breakthrough spotting | HORMONAL_IUD | TRACK_CYCLE |
| `COPPER_IUD` | 29 | 18mo on copper IUD, regular 28d cycles, heavier 6-7d periods | COPPER_IUD | TTC |
| `ATHLETE_AMENORRHEA` | 24 | 3-4 cycles over 18mo, gaps 90-160d | none | TRACK_CYCLE |

### Group 5 — Clinical case reports (6, hand-transcribed)

Sourced from public medical case reports during research phase; each transcription carries a citation comment block referencing the journal. Target distribution:

- `CASE_PCOS_LATE_DX` — PCOS case report (long cycles, late diagnosis)
- `CASE_PERIMENOPAUSE_TRANSITION` — perimenopause transition cycle pattern
- `CASE_POSTPARTUM_LACTATIONAL` — lactational amenorrhea then resumption
- `CASE_PILL_DISCONTINUATION` — post-pill cycle return pattern
- `CASE_ANOVULATORY` — chronic anovulation case
- `CASE_IUD_BREAKTHROUGH` — hormonal-IUD breakthrough bleeding pattern

Exact case selection happens during the research phase (commit 1); fallbacks if a fitting case isn't found: pull from textbook clinical examples (ACOG Practice Bulletins) which are also citable.

## 5. Data sources

**Primary: synthetic from published distributions.** A single research subagent run produces `docs/superpowers/specs/research/2026-05-18-cycle-distributions.md` covering:

- Treloar et al. 1967 — landmark longitudinal cycle-length dataset
- Bull et al. 2019 "Real-world menstrual cycle characteristics" (Clue, n≈600k cycles)
- Liu et al. 2024 or similar recent publications
- ACOG Practice Bulletin on menstrual disorders
- WHO definitions for amenorrhea / oligomenorrhea
- Apple Women's Health Study aggregate findings
- Standard reference on post-partum cycle resumption
- Hormonal vs copper IUD bleeding patterns (clinical literature)

Per population segment, the doc records: cycle length mean + sd + distribution shape, period length mean + sd, anovulation rate %, symptom prevalence per type, common bleeding-pattern abnormalities, citations.

`PersonaCatalog` parameters are picked by hand from this doc; reviewers can trace any parameter back to a citation.

**Secondary: clinical case reports.** Research subagent also surfaces ~6 published case reports (or ACOG bulletin examples if equivalents aren't available), one per Group-5 persona. Each report's cycle history is hand-transcribed into a `CaseReportPersonas.kt` fixture with a citation comment block.

No real anonymized user-level datasets are imported. Off-brand for a privacy-first app and avoids redistribution / re-identification surface.

## 6. Architecture

Four new files in `app/src/androidTest/java/com/hayate0726/tides/validation/`:

### 6.1 `PersonaCatalog.kt` — declarative specs

```kotlin
data class PersonaSpec(
    val id: String,
    val populationSegment: PopulationSegment,
    val historyMonths: Int,
    val cycleLengthDays: DistParams,
    val periodLengthDays: DistParams,
    val anovulationRate: Double,
    val symptomPrevalence: Map<Symptom, Double>,
    val flowDistribution: Map<FlowIntensity, Double>,
    val goals: Set<Goal>,
    val birthControl: BirthControlMethod?,
    val bcStartedMonthsAgo: Int? = null,
    val skipLogProbability: Double = 0.0,
)

data class DistParams(
    val mean: Double,
    val sd: Double,
    val shape: Shape = Shape.LOG_NORMAL,
    val minClamp: Int? = null,
    val maxClamp: Int? = null,
)

enum class Shape { NORMAL, LOG_NORMAL }
enum class PopulationSegment {
    TYPICAL, IRREGULAR_PCOS, IRREGULAR_PERIMENOPAUSE, IRREGULAR_ADOLESCENT,
    TRANSITION_POSTPARTUM, TRANSITION_BC_STOPPED, TRANSITION_BC_STARTED,
    SPECIAL_HORMONAL_IUD, SPECIAL_COPPER_IUD, SPECIAL_ATHLETE,
    CASE_REPORT,
}

object SyntheticPersonas {
    val all: List<PersonaSpec> = listOf( /* 12 specs */ )
}
```

### 6.2 `PersonaGenerator.kt` — deterministic synthesis

```kotlin
object PersonaGenerator {
    fun generate(spec: PersonaSpec, seed: Long, today: LocalDate = LocalDate.now()): Persona
}

data class Persona(
    val spec: PersonaSpec,
    val cycleEntries: List<CycleEntryEntity>,
    val symptomEntries: List<SymptomEntryEntity>,
    val goals: Set<Goal>,
    val birthControl: BirthControlEntity?,
)
```

- `java.util.Random(seed)` for reproducibility — sufficient for synthetic test data.
- Log-normal default for cycle length (right-skewed in real populations); clamps applied post-sample.
- For BC transitions: `bcStartedMonthsAgo` splits history; earlier portion uses pre-BC distribution, newer uses post-BC pattern.
- `skipLogProbability` randomly drops daily entries to simulate users who skip logging.
- Anovulatory cycles share cycle-length distribution but get no ovulation marker (no-op for current schema; API-ready for future).

### 6.3 `CaseReportPersonas.kt` — hand-built fixtures

```kotlin
object CaseReportPersonas {
    val all: List<Persona> = listOf(/* 6 hand-built Persona instances */)
}
```

Each `Persona` constructed inline with explicit cycle dates + symptoms; citation in KDoc comment block above the val.

### 6.4 `AllPersonas.kt` — union for the test runner

```kotlin
object AllPersonas {
    val all: List<Persona> by lazy {
        SyntheticPersonas.all.map { PersonaGenerator.generate(it, seedFor(it.id)) } +
            CaseReportPersonas.all
    }
    private fun seedFor(id: String): Long = id.hashCode().toLong()
}
```

### 6.5 `PersonaTestHarness.kt` — utilities

Opens ephemeral in-memory test DB with a fixed key, wires `UserPrivacyRepository`, runs a ViewModel through `refresh()` and `firstOrNull { it.cycles.isNotEmpty() }`-style waits. Hides Hilt + coroutine boilerplate from individual tests.

## 7. Tests

### 7.1 `PersonaScenarioTest.kt` — 6 property assertions × 18 personas

```kotlin
@RunWith(Parameterized::class)
@HiltAndroidTest
class PersonaScenarioTest(private val persona: Persona) {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

    @Test fun ovulation_gate_respects_bc_and_goals()
    @Test fun cycle_detection_does_not_merge_distant_cycles()
    @Test fun prediction_confidence_drops_with_variability()
    @Test fun stats_handles_history_without_crash()
    @Test fun widget_summary_matches_privacy_gate()
    @Test fun figo_flags_pattern_for_known_disorders()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun personas(): List<Array<Persona>> = AllPersonas.all.map { arrayOf(it) }
    }
}
```

**The six properties:**

1. **Ovulation gate:** `UserPrivacyView.compute(persona.goals, persona.birthControl?.method).showOvulation` equals the expected value for the persona's `populationSegment`. Hormonal BC ⇒ always false; goals without OVULATION_RELEVANT ⇒ always false; otherwise true.

2. **Cycle merging:** for every adjacent pair returned by `CycleRepository.detectCycles(from, to)`, either the gap is `≤ 35` days or they are returned as separate `Cycle` objects. No synthesis of a 90-day "cycle" from an amenorrheic gap.

3. **Confidence decay:** `CyclePredictor.predictNextPeriod(cycles)` returns `null` when fewer than 2 completed cycles exist. Otherwise, define `range = max(lengths) - min(lengths)`. Asserted: `confidence == HIGH` iff `range ≤ 2`; `confidence == MEDIUM` iff `range ≤ 7`; `confidence == LOW` iff `range > 7`. Matches the live predictor at `CyclePredictor.kt:38-42`. For irregular personas (PCOS, perimenopause, athlete), the assertion is `confidence == LOW`.

4. **Stats robustness:** for each `StatsViewModel.Range`, `refresh()` never throws and emits a populated `Stats` state within 5 seconds.

5. **Widget consistency:** after `CalendarViewModel.refresh()`, the persisted `WidgetSummary.read(ctx)` snapshot's `showOvulation` field equals `UserPrivacyRepository.view.value.showOvulation`.

6. **FIGO patterns (segment-conditional):** PCOS personas MUST flag `OLIGOMENORRHEA`. Perimenopause personas MAY flag `OLIGOMENORRHEA` or `HEAVY_MENSTRUAL_BLEEDING`. Athlete-amenorrhea persona MUST flag `AMENORRHEA`. Typical-adult personas MUST NOT flag any abnormality.

Failure messages always include persona id + seed for repro.

### 7.2 `PersonaSnapshotTest.kt` — 3 byte-exact widget snapshots

Captures the persisted `widget_summary.bin` file bytes for `TYPICAL_25`, `PCOS_LONG`, `HORMONAL_IUD_MIRENA`. Asserts byte-exact match against fixtures in `app/src/androidTest/assets/persona-snapshots/<id>.bin`. Locks the widget binary format so any change is intentional. Regenerating a snapshot is a documented one-line operation (see README).

## 8. Build / CI integration

- New source package only; no new Gradle dependencies. Uses existing `androidx.test`, JUnit 4, Hilt testing, Room/SQLCipher.
- Run target: `./gradlew :app:connectedDebugAndroidTest`.
- Total runtime estimate: 18 personas × 6 property tests × ~3s warm-emulator overhead ≈ 6 minutes. Acceptable for nightly CI; tight for every-PR if PR builds become emulator-backed.
- Documentation:
  - `docs/superpowers/specs/research/2026-05-18-cycle-distributions.md` — research output, referenced by `PersonaCatalog` parameter choices.
  - `app/src/androidTest/java/com/hayate0726/tides/validation/README.md` — how to add a new persona, how to regenerate a snapshot fixture.

## 9. Commit plan

1. **`docs(validation): cycle distribution research + clinical case sources`** — research doc + case-report sources surfaced. No code yet.
2. **`feat(validation): PersonaGenerator + persona catalog`** — `PersonaSpec`, `PersonaGenerator`, `SyntheticPersonas` (12), `CaseReportPersonas` (6), `AllPersonas`. Compiles, no tests.
3. **`test(validation): persona scenario suite (6 property assertions × 18 personas)`** — `PersonaTestHarness`, `PersonaScenarioTest` with all six property assertions.
4. **`test(validation): widget snapshot fixtures + PersonaSnapshotTest`** — 3 fixture bytes + `PersonaSnapshotTest`; README.

## 10. Non-goals

- UI Compose tests. The validation surface stops at ViewModel state + repository snapshots.
- Real anonymized user datasets. Out of scope per Section 5.
- Performance benchmarking. This suite asserts correctness, not speed.
- Coverage of features explicitly deferred from v1.0 (pregnancy mode, stealth mode, partner sharing, custom symptoms).
- Auto-regenerating snapshot fixtures in CI. Snapshots are intentionally manual to force human review of any format change.

## 11. Risks

- **Emulator flake.** Instrumented suites can be flaky on cold emulators. Mitigation: run the suite once on a warm emulator before declaring the design accepted; document any required warmup in the README.
- **Distribution data gaps.** Some segments (post-partum lactational amenorrhea, adolescent first-2-years) have thin public literature. Mitigation: research subagent reports gaps explicitly; we either choose conservative defaults or de-scope the affected persona.
- **Case-report unavailability.** Public case reports for niche presentations may not be findable. Mitigation: substitute ACOG/WHO bulletin examples (also citable, less narrative).
- **Test runtime growth.** 6 minutes is acceptable now; if it doubles after adding personas, gate the suite behind `@Category(PersonaSuite::class)` and run nightly rather than per-PR.
