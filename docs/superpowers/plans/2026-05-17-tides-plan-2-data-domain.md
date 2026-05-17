# Tides Plan 2: Data & Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Plan 1 placeholder schema with the real Room schema, and build the pure-Kotlin `domain/` module containing all cycle prediction, phase calculation, statistics, and FIGO analysis logic. End state: a headless library with ≥90% test coverage where every cycle/symptom math operation has a test.

**Architecture:** `data/` owns Room entities, DAOs, and the SQLCipher-backed database. `domain/` is pure Kotlin (no Android, no Room, no Compose imports) so the math is JVM-testable in milliseconds. The two packages communicate via plain Kotlin data classes; a `CycleRepository` in `data/` is the bridge that hands `domain/` the data it needs.

**Tech Stack:**
- Kotlin 2.0.x with `java.time` (`LocalDate`, `Period`)
- Room 2.6+ with KSP
- SQLCipher (already integrated in Plan 1)
- JUnit5 (already in Plan 1)
- `kotlinx.coroutines` Flow for reactive queries

**Out of scope:**
- UI for any of this (Plan 3)
- Export/PDF generation (Plan 4 — but the FIGO analysis functions are built here)
- Threat-preset/duress-mode logic (already in Plan 1's `LockManager`)
- Insights card content (Plan 3 UI, uses Plan 2 stats)

---

## File structure

**`domain/` (pure Kotlin):**
- `app/src/main/java/com/hayate0726/tides/domain/model/CycleDay.kt` — value type
- `app/src/main/java/com/hayate0726/tides/domain/model/FlowIntensity.kt` — enum
- `app/src/main/java/com/hayate0726/tides/domain/model/Symptom.kt` — enum + category
- `app/src/main/java/com/hayate0726/tides/domain/model/BirthControlMethod.kt` — enum
- `app/src/main/java/com/hayate0726/tides/domain/model/Goal.kt` — enum
- `app/src/main/java/com/hayate0726/tides/domain/model/CalendarView.kt` — enum
- `app/src/main/java/com/hayate0726/tides/domain/model/ThreatPreset.kt` — enum
- `app/src/main/java/com/hayate0726/tides/domain/model/Cycle.kt` — derived data class
- `app/src/main/java/com/hayate0726/tides/domain/model/Phase.kt` — enum + boundary calculation
- `app/src/main/java/com/hayate0726/tides/domain/model/PredictionRange.kt` — value type
- `app/src/main/java/com/hayate0726/tides/domain/CycleDetector.kt` — period-day list → list of `Cycle`
- `app/src/main/java/com/hayate0726/tides/domain/CyclePredictor.kt` — predict next-period range
- `app/src/main/java/com/hayate0726/tides/domain/PhaseCalculator.kt` — current phase + boundaries
- `app/src/main/java/com/hayate0726/tides/domain/CycleStats.kt` — averages, variance, regularity
- `app/src/main/java/com/hayate0726/tides/domain/SymptomStats.kt` — frequency, cycle-day heatmap
- `app/src/main/java/com/hayate0726/tides/domain/FigoAnalysis.kt` — FIGO threshold detection

**`data/` (Room + SQLCipher; replaces Plan 1 placeholder):**
- `app/src/main/java/com/hayate0726/tides/data/entity/CycleEntryEntity.kt`
- `app/src/main/java/com/hayate0726/tides/data/entity/SymptomEntryEntity.kt`
- `app/src/main/java/com/hayate0726/tides/data/entity/BirthControlEntity.kt`
- `app/src/main/java/com/hayate0726/tides/data/entity/SettingsEntity.kt`
- `app/src/main/java/com/hayate0726/tides/data/entity/GoalEntity.kt`
- `app/src/main/java/com/hayate0726/tides/data/Converters.kt` — `LocalDate`, enum converters
- `app/src/main/java/com/hayate0726/tides/data/dao/CycleEntryDao.kt`
- `app/src/main/java/com/hayate0726/tides/data/dao/SymptomEntryDao.kt`
- `app/src/main/java/com/hayate0726/tides/data/dao/BirthControlDao.kt`
- `app/src/main/java/com/hayate0726/tides/data/dao/SettingsDao.kt`
- `app/src/main/java/com/hayate0726/tides/data/dao/GoalDao.kt`
- Modify: `app/src/main/java/com/hayate0726/tides/data/TidesDatabase.kt` — replace placeholder schema
- Delete: `app/src/main/java/com/hayate0726/tides/data/Placeholder.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/CycleRepository.kt` — bridge to `domain/`
- Create: `app/src/main/schemas/com.hayate0726.tides.data.TidesDatabase/1.json` (Room schema export, generated)

**Tests:**
- `app/src/test/java/com/hayate0726/tides/domain/CycleDetectorTest.kt`
- `app/src/test/java/com/hayate0726/tides/domain/CyclePredictorTest.kt`
- `app/src/test/java/com/hayate0726/tides/domain/PhaseCalculatorTest.kt`
- `app/src/test/java/com/hayate0726/tides/domain/CycleStatsTest.kt`
- `app/src/test/java/com/hayate0726/tides/domain/SymptomStatsTest.kt`
- `app/src/test/java/com/hayate0726/tides/domain/FigoAnalysisTest.kt`
- `app/src/androidTest/java/com/hayate0726/tides/data/RoomSchemaTest.kt`
- `app/src/androidTest/java/com/hayate0726/tides/data/CycleEntryDaoTest.kt`
- `app/src/androidTest/java/com/hayate0726/tides/data/CycleRepositoryTest.kt`

---

## Task 1: Domain enums and value types

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/FlowIntensity.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/Symptom.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/BirthControlMethod.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/Goal.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/CalendarView.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/ThreatPreset.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/CycleDay.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/PredictionRange.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/Cycle.kt`
- Create: `app/src/main/java/com/hayate0726/tides/domain/model/Phase.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/model/SymptomTest.kt`

These are leaf types — no logic, just data shapes. We do one test on the curated `Symptom` taxonomy to verify it's exhaustive per the spec.

- [ ] **Step 1: Write the failing taxonomy test**

Create `app/src/test/java/com/hayate0726/tides/domain/model/SymptomTest.kt`:

```kotlin
package com.hayate0726.tides.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymptomTest {

    @Test
    fun `curated taxonomy contains every spec symptom`() {
        val expected = setOf(
            // Pain
            Symptom.CRAMPS, Symptom.HEADACHE, Symptom.MIGRAINE, Symptom.BACK_PAIN,
            Symptom.BREAST_TENDERNESS, Symptom.JOINT_PAIN, Symptom.ABDOMINAL_PAIN,
            // Mood
            Symptom.ANXIOUS, Symptom.IRRITABLE, Symptom.SAD, Symptom.HAPPY,
            Symptom.SENSITIVE, Symptom.CALM,
            // Energy
            Symptom.TIRED, Symptom.ENERGETIC, Symptom.RESTLESS, Symptom.FOGGY,
            // Body
            Symptom.BLOATING, Symptom.ACNE, Symptom.HOT_FLASHES, Symptom.CHILLS,
            Symptom.DIZZINESS, Symptom.NAUSEA, Symptom.VOMITING,
            // Digestive
            Symptom.CONSTIPATION, Symptom.DIARRHEA, Symptom.GAS,
            Symptom.INCREASED_APPETITE, Symptom.DECREASED_APPETITE, Symptom.CRAVINGS,
            // Sleep
            Symptom.INSOMNIA, Symptom.OVERSLEEPING, Symptom.VIVID_DREAMS, Symptom.NIGHT_SWEATS,
            // Discharge
            Symptom.DRY, Symptom.STICKY, Symptom.CREAMY, Symptom.WATERY,
            Symptom.EGG_WHITE, Symptom.BROWN, Symptom.SPOTTING,
            // Sex
            Symptom.HIGH_LIBIDO, Symptom.LOW_LIBIDO, Symptom.PAINFUL_SEX,
            // Other (note)
            Symptom.OTHER,
        )
        assertEquals(expected, Symptom.values().toSet())
    }

    @Test
    fun `every symptom belongs to exactly one category`() {
        val all = Symptom.values()
        for (s in all) {
            assertTrue(s.category != null, "Symptom $s has no category")
        }
    }

    @Test
    fun `OTHER is the only symptom that is freetext`() {
        val freetext = Symptom.values().filter { it.isFreeText }
        assertEquals(listOf(Symptom.OTHER), freetext)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.model.SymptomTest"
```

Expected: FAIL with "unresolved reference: Symptom"

- [ ] **Step 3: Create `FlowIntensity`**

Create `app/src/main/java/com/hayate0726/tides/domain/model/FlowIntensity.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class FlowIntensity(val intCode: Int) {
    NONE(0),
    SPOTTING(1),
    LIGHT(2),
    MEDIUM(3),
    HEAVY(4);

    companion object {
        fun fromInt(value: Int): FlowIntensity =
            values().first { it.intCode == value }

        /** Per FIGO, "heavy" includes our HEAVY only (spec §5.7). */
        fun isHeavy(intensity: FlowIntensity) = intensity == HEAVY

        /** Any actual bleeding, excluding NONE. Used for cycle detection. */
        fun isBleeding(intensity: FlowIntensity) = intensity != NONE
    }
}
```

- [ ] **Step 4: Create `Symptom`**

Create `app/src/main/java/com/hayate0726/tides/domain/model/Symptom.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class SymptomCategory {
    PAIN, MOOD, ENERGY, BODY, DIGESTIVE, SLEEP, DISCHARGE, SEX, OTHER,
}

enum class Symptom(
    val category: SymptomCategory,
    val isFreeText: Boolean = false,
) {
    // Pain
    CRAMPS(SymptomCategory.PAIN),
    HEADACHE(SymptomCategory.PAIN),
    MIGRAINE(SymptomCategory.PAIN),
    BACK_PAIN(SymptomCategory.PAIN),
    BREAST_TENDERNESS(SymptomCategory.PAIN),
    JOINT_PAIN(SymptomCategory.PAIN),
    ABDOMINAL_PAIN(SymptomCategory.PAIN),

    // Mood
    ANXIOUS(SymptomCategory.MOOD),
    IRRITABLE(SymptomCategory.MOOD),
    SAD(SymptomCategory.MOOD),
    HAPPY(SymptomCategory.MOOD),
    SENSITIVE(SymptomCategory.MOOD),
    CALM(SymptomCategory.MOOD),

    // Energy
    TIRED(SymptomCategory.ENERGY),
    ENERGETIC(SymptomCategory.ENERGY),
    RESTLESS(SymptomCategory.ENERGY),
    FOGGY(SymptomCategory.ENERGY),

    // Body
    BLOATING(SymptomCategory.BODY),
    ACNE(SymptomCategory.BODY),
    HOT_FLASHES(SymptomCategory.BODY),
    CHILLS(SymptomCategory.BODY),
    DIZZINESS(SymptomCategory.BODY),
    NAUSEA(SymptomCategory.BODY),
    VOMITING(SymptomCategory.BODY),

    // Digestive
    CONSTIPATION(SymptomCategory.DIGESTIVE),
    DIARRHEA(SymptomCategory.DIGESTIVE),
    GAS(SymptomCategory.DIGESTIVE),
    INCREASED_APPETITE(SymptomCategory.DIGESTIVE),
    DECREASED_APPETITE(SymptomCategory.DIGESTIVE),
    CRAVINGS(SymptomCategory.DIGESTIVE),

    // Sleep
    INSOMNIA(SymptomCategory.SLEEP),
    OVERSLEEPING(SymptomCategory.SLEEP),
    VIVID_DREAMS(SymptomCategory.SLEEP),
    NIGHT_SWEATS(SymptomCategory.SLEEP),

    // Discharge
    DRY(SymptomCategory.DISCHARGE),
    STICKY(SymptomCategory.DISCHARGE),
    CREAMY(SymptomCategory.DISCHARGE),
    WATERY(SymptomCategory.DISCHARGE),
    EGG_WHITE(SymptomCategory.DISCHARGE),
    BROWN(SymptomCategory.DISCHARGE),
    SPOTTING(SymptomCategory.DISCHARGE),

    // Sex
    HIGH_LIBIDO(SymptomCategory.SEX),
    LOW_LIBIDO(SymptomCategory.SEX),
    PAINFUL_SEX(SymptomCategory.SEX),

    // Other (note) — free-text escape hatch
    OTHER(SymptomCategory.OTHER, isFreeText = true);
}
```

- [ ] **Step 5: Create the remaining enums**

Create `app/src/main/java/com/hayate0726/tides/domain/model/BirthControlMethod.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class BirthControlMethod(val isHormonal: Boolean) {
    NONE(isHormonal = false),
    PILL(isHormonal = true),
    HORMONAL_IUD(isHormonal = true),
    COPPER_IUD(isHormonal = false),
    IMPLANT(isHormonal = true),
    PATCH(isHormonal = true),
    RING(isHormonal = true),
    OTHER(isHormonal = false);
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/Goal.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class Goal {
    TRACK_PERIOD,
    TRACK_SYMPTOMS,
    MANAGE_CONDITION,
    AVOID_PREGNANCY,
    TRYING_TO_CONCEIVE,
    JUST_CURIOUS;

    companion object {
        /**
         * Per spec §5.1, ovulation/fertile-window UI is suppressed unless
         * the user has at least one of these goals selected.
         */
        val OVULATION_RELEVANT: Set<Goal> = setOf(AVOID_PREGNANCY, TRYING_TO_CONCEIVE)
    }
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/CalendarView.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class CalendarView {
    ALL, PERIOD_ONLY, PHASES, SYMPTOMS;
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/ThreatPreset.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class ThreatPreset(
    val backgroundLockTimeoutMs: Long?,
    val lockScreenPreviewVisible: Boolean,
    val defaultNotificationTitle: String,
    val duressAvailable: Boolean,
) {
    JUST_FOR_ME(
        backgroundLockTimeoutMs = null, // never auto-lock
        lockScreenPreviewVisible = true,
        defaultNotificationTitle = "Tides",
        duressAvailable = false,
    ),
    LOCKED_WHEN_AWAY(
        backgroundLockTimeoutMs = 5 * 60 * 1000L,
        lockScreenPreviewVisible = false,
        defaultNotificationTitle = "Reminder",
        duressAvailable = false,
    ),
    ALWAYS_LOCKED(
        backgroundLockTimeoutMs = 30 * 1000L,
        lockScreenPreviewVisible = false,
        defaultNotificationTitle = "Reminder",
        duressAvailable = true,
    );

    companion object {
        val DEFAULT = LOCKED_WHEN_AWAY
    }
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/CycleDay.kt`:

```kotlin
package com.hayate0726.tides.domain.model

import java.time.LocalDate

@JvmInline
value class CycleDay(val value: Int) {
    init {
        require(value >= 1) { "cycle day is 1-indexed, got $value" }
    }

    companion object {
        fun of(startDate: LocalDate, date: LocalDate): CycleDay =
            CycleDay(java.time.temporal.ChronoUnit.DAYS.between(startDate, date).toInt() + 1)
    }
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/PredictionRange.kt`:

```kotlin
package com.hayate0726.tides.domain.model

import java.time.LocalDate

/**
 * A predicted range of dates (inclusive on both ends).
 *
 * `confidence` is a coarse bucket so the UI can render width.
 */
data class PredictionRange(
    val start: LocalDate,
    val end: LocalDate,
    val confidence: Confidence,
) {
    init {
        require(!end.isBefore(start)) { "end must be >= start" }
    }

    enum class Confidence { HIGH, MEDIUM, LOW }
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/Cycle.kt`:

```kotlin
package com.hayate0726.tides.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One completed (or in-progress) menstrual cycle.
 *
 * - `start`: first day of the period that began this cycle
 * - `periodEnd`: last day with non-zero flow in this cycle (null if still bleeding)
 * - `nextStart`: first day of the *next* period (null if current cycle still active)
 *
 * `length` = days from `start` to `nextStart` (exclusive). Null if active.
 * `periodLength` = days from `start` to `periodEnd` inclusive. Null if still bleeding.
 */
data class Cycle(
    val start: LocalDate,
    val periodEnd: LocalDate?,
    val nextStart: LocalDate?,
) {
    init {
        if (periodEnd != null) require(!periodEnd.isBefore(start)) {
            "periodEnd must be >= start"
        }
        if (nextStart != null) require(nextStart.isAfter(start)) {
            "nextStart must be > start"
        }
        if (nextStart != null && periodEnd != null) require(!periodEnd.isAfter(nextStart)) {
            "periodEnd must be <= nextStart"
        }
    }

    val length: Int?
        get() = nextStart?.let { ChronoUnit.DAYS.between(start, it).toInt() }

    val periodLength: Int?
        get() = periodEnd?.let { ChronoUnit.DAYS.between(start, it).toInt() + 1 }

    val isActive: Boolean get() = nextStart == null
}
```

Create `app/src/main/java/com/hayate0726/tides/domain/model/Phase.kt`:

```kotlin
package com.hayate0726.tides.domain.model

enum class Phase {
    MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL;
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.model.SymptomTest"
```

Expected: 3 tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/model app/src/test/java/com/hayate0726/tides/domain/model
git commit -m "feat(domain): add enums and value types (Symptom, FlowIntensity, Phase, Cycle, etc.)"
```

---

## Task 2: CycleDetector — turn period-day entries into Cycle objects

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/CycleDetector.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/CycleDetectorTest.kt`

A "cycle" begins on the first day of a period after a gap of 2+ days without bleeding. This detector turns a sparse list of `(date, FlowIntensity)` entries into a list of `Cycle` objects.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/domain/CycleDetectorTest.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CycleDetectorTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun entry(date: String, flow: FlowIntensity) =
        CycleDetector.Entry(d(date), flow)

    @Test
    fun `empty input produces empty list`() {
        val result = CycleDetector.detect(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single bleeding day produces one active cycle with periodEnd=that day`() {
        val entries = listOf(entry("2026-05-01", FlowIntensity.MEDIUM))
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        val c = result.single()
        assertEquals(d("2026-05-01"), c.start)
        assertEquals(d("2026-05-01"), c.periodEnd)
        assertNull(c.nextStart)
        assertTrue(c.isActive)
    }

    @Test
    fun `contiguous bleeding days form one period`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-02", FlowIntensity.MEDIUM),
            entry("2026-05-03", FlowIntensity.LIGHT),
            entry("2026-05-04", FlowIntensity.SPOTTING),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        assertEquals(d("2026-05-04"), result.single().periodEnd)
        assertEquals(4, result.single().periodLength)
    }

    @Test
    fun `gap of one day still counts as same period`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-02", FlowIntensity.LIGHT),
            // no entry on 5-3
            entry("2026-05-04", FlowIntensity.SPOTTING),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        assertEquals(d("2026-05-04"), result.single().periodEnd)
    }

    @Test
    fun `gap of two or more days starts a new cycle`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-02", FlowIntensity.LIGHT),
            // gap of 2 days (5-3 and 5-4 both empty)
            entry("2026-05-29", FlowIntensity.MEDIUM),
            entry("2026-05-30", FlowIntensity.LIGHT),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(2, result.size)
        assertEquals(d("2026-05-01"), result[0].start)
        assertEquals(d("2026-05-29"), result[0].nextStart)
        assertEquals(28, result[0].length)
        assertEquals(d("2026-05-29"), result[1].start)
        assertTrue(result[1].isActive)
    }

    @Test
    fun `NONE entries do not start or extend a period`() {
        val entries = listOf(
            entry("2026-05-01", FlowIntensity.NONE),
            entry("2026-05-02", FlowIntensity.MEDIUM),
            entry("2026-05-03", FlowIntensity.NONE),
            entry("2026-05-04", FlowIntensity.LIGHT),
        )
        val result = CycleDetector.detect(entries)
        assertEquals(1, result.size)
        assertEquals(d("2026-05-02"), result.single().start)
        assertEquals(d("2026-05-04"), result.single().periodEnd)
    }

    @Test
    fun `entries are sorted internally so order in does not matter`() {
        val a = CycleDetector.detect(listOf(
            entry("2026-05-04", FlowIntensity.LIGHT),
            entry("2026-05-01", FlowIntensity.MEDIUM),
        ))
        val b = CycleDetector.detect(listOf(
            entry("2026-05-01", FlowIntensity.MEDIUM),
            entry("2026-05-04", FlowIntensity.LIGHT),
        ))
        assertEquals(a.size, b.size)
        assertEquals(a[0].start, b[0].start)
        assertEquals(a[0].periodEnd, b[0].periodEnd)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.CycleDetectorTest"
```

Expected: FAIL with "unresolved reference: CycleDetector"

- [ ] **Step 3: Implement `CycleDetector`**

Create `app/src/main/java/com/hayate0726/tides/domain/CycleDetector.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Detects [Cycle] boundaries from a sparse list of daily flow entries.
 *
 * Rules (spec §5.2 + domain conventions):
 *  - Only entries with non-NONE flow count as "bleeding."
 *  - A cycle starts on the first bleeding day after a gap of ≥2 days without bleeding.
 *  - A period ends on the last consecutive bleeding day (allowing a one-day gap).
 *  - The most recent cycle is "active" (nextStart = null) until a new cycle starts.
 */
object CycleDetector {

    data class Entry(val date: LocalDate, val flow: FlowIntensity)

    /** Max allowed gap (days) within a single period — gaps longer split the period. */
    private const val MAX_INTRA_PERIOD_GAP_DAYS = 1L

    fun detect(entries: List<Entry>): List<Cycle> {
        val bleedingDays = entries
            .asSequence()
            .filter { FlowIntensity.isBleeding(it.flow) }
            .map { it.date }
            .distinct()
            .sorted()
            .toList()

        if (bleedingDays.isEmpty()) return emptyList()

        // Group bleeding days into "period runs" allowing 1-day gaps.
        val periodRuns = mutableListOf<MutableList<LocalDate>>()
        var current = mutableListOf<LocalDate>().apply { add(bleedingDays[0]) }
        for (i in 1 until bleedingDays.size) {
            val gap = ChronoUnit.DAYS.between(bleedingDays[i - 1], bleedingDays[i])
            if (gap <= MAX_INTRA_PERIOD_GAP_DAYS + 1) {
                current.add(bleedingDays[i])
            } else {
                periodRuns.add(current)
                current = mutableListOf(bleedingDays[i])
            }
        }
        periodRuns.add(current)

        // Convert period runs into Cycle objects.
        val cycles = mutableListOf<Cycle>()
        for ((i, run) in periodRuns.withIndex()) {
            val start = run.first()
            val periodEnd = run.last()
            val nextStart = periodRuns.getOrNull(i + 1)?.first()
            cycles.add(Cycle(start = start, periodEnd = periodEnd, nextStart = nextStart))
        }
        return cycles
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.CycleDetectorTest"
```

Expected: 7 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/CycleDetector.kt \
        app/src/test/java/com/hayate0726/tides/domain/CycleDetectorTest.kt
git commit -m "feat(domain): add CycleDetector (flow entries → Cycle list)"
```

---

## Task 3: CycleStats — median, variance, regularity, period trend

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/CycleStats.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/CycleStatsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/domain/CycleStatsTest.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CycleStatsTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            LocalDate.parse(start),
            LocalDate.parse(periodEnd),
            nextStart?.let { LocalDate.parse(it) }
        )

    @Test
    fun `empty cycles list returns empty stats`() {
        val s = CycleStats.compute(emptyList())
        assertNull(s.medianCycleLength)
        assertNull(s.medianPeriodLength)
    }

    @Test
    fun `single completed cycle reports its length`() {
        val s = CycleStats.compute(listOf(
            cycle("2026-01-01", "2026-01-04", "2026-01-29")
        ))
        assertEquals(28, s.medianCycleLength)
        assertEquals(4, s.medianPeriodLength)
    }

    @Test
    fun `median of an even number takes the lower middle`() {
        // lengths: 27, 28, 29, 30 → median = 28 (low-median convention)
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-04", "2026-01-28"),  // 27
            cycle("2026-01-28", "2026-01-31", "2026-02-25"),  // 28
            cycle("2026-02-25", "2026-02-28", "2026-03-26"),  // 29
            cycle("2026-03-26", "2026-03-29", "2026-04-25"),  // 30 (length-wise)
        )
        val s = CycleStats.compute(cycles)
        assertEquals(28, s.medianCycleLength)
    }

    @Test
    fun `regularity score is HIGH for variance under 2 days`() {
        // 28, 28, 29 — variance = 1
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 28
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 28
            cycle("2026-02-26", "2026-03-02", "2026-03-27"),  // 29
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Regularity.VERY_REGULAR, s.regularity)
    }

    @Test
    fun `regularity score is MODERATELY_VARIABLE for variance 4 to 7`() {
        // 26, 28, 30, 32 — max-min = 6
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-27"),  // 26
            cycle("2026-01-27", "2026-01-31", "2026-02-24"),  // 28
            cycle("2026-02-24", "2026-02-28", "2026-03-26"),  // 30
            cycle("2026-03-26", "2026-03-30", "2026-04-27"),  // 32
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Regularity.MODERATELY_VARIABLE, s.regularity)
    }

    @Test
    fun `regularity score is HIGHLY_VARIABLE for variance over 7`() {
        // 22, 28, 35 — max-min = 13
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-23"),  // 22
            cycle("2026-01-23", "2026-01-27", "2026-02-20"),  // 28
            cycle("2026-02-20", "2026-02-24", "2026-03-27"),  // 35
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Regularity.HIGHLY_VARIABLE, s.regularity)
    }

    @Test
    fun `period length trend detects shortening`() {
        // Recent period lengths: 5, 5, 4, 3, 3 — clearly shortening
        val cycles = listOf(
            // longer first half
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 5
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 5
            cycle("2026-02-26", "2026-03-01", "2026-03-26"),  // 4
            cycle("2026-03-26", "2026-03-28", "2026-04-23"),  // 3
            cycle("2026-04-23", "2026-04-25", "2026-05-21"),  // 3
        )
        val s = CycleStats.compute(cycles)
        assertEquals(CycleStats.Trend.DECREASING, s.periodLengthTrend)
    }

    @Test
    fun `active cycle is excluded from length stats`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-04", "2026-01-29"),
            cycle("2026-01-29", "2026-02-02", null),  // active, no length yet
        )
        val s = CycleStats.compute(cycles)
        assertEquals(28, s.medianCycleLength)
        assertEquals(1, s.completedCycleCount)
        assertTrue(s.hasActiveCycle)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.CycleStatsTest"
```

Expected: FAIL with "unresolved reference: CycleStats"

- [ ] **Step 3: Implement `CycleStats`**

Create `app/src/main/java/com/hayate0726/tides/domain/CycleStats.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle

/**
 * Aggregate statistics over a series of cycles.
 *
 * "Median" uses the lower-of-two-middles convention for even-length lists.
 * Regularity buckets are FIGO-aligned per spec §5.6.
 */
data class CycleStats(
    val medianCycleLength: Int?,
    val cycleLengthMin: Int?,
    val cycleLengthMax: Int?,
    val cycleLengthVariance: Int?, // max - min
    val medianPeriodLength: Int?,
    val regularity: Regularity?,
    val periodLengthTrend: Trend,
    val completedCycleCount: Int,
    val hasActiveCycle: Boolean,
) {
    enum class Regularity { VERY_REGULAR, MODERATELY_VARIABLE, HIGHLY_VARIABLE }
    enum class Trend { INCREASING, DECREASING, STABLE, UNKNOWN }

    companion object {

        fun compute(cycles: List<Cycle>): CycleStats {
            val completed = cycles.filter { !it.isActive }
            val active = cycles.firstOrNull { it.isActive }

            val cycleLens = completed.mapNotNull { it.length }
            val periodLens = completed.mapNotNull { it.periodLength }

            val medianCycle = cycleLens.medianOrNull()
            val minCycle = cycleLens.minOrNull()
            val maxCycle = cycleLens.maxOrNull()
            val variance = if (minCycle != null && maxCycle != null) maxCycle - minCycle else null

            val regularity = variance?.let {
                when {
                    it <= 2 -> Regularity.VERY_REGULAR
                    it <= 7 -> Regularity.MODERATELY_VARIABLE
                    else -> Regularity.HIGHLY_VARIABLE
                }
            }

            return CycleStats(
                medianCycleLength = medianCycle,
                cycleLengthMin = minCycle,
                cycleLengthMax = maxCycle,
                cycleLengthVariance = variance,
                medianPeriodLength = periodLens.medianOrNull(),
                regularity = regularity,
                periodLengthTrend = trendOf(periodLens),
                completedCycleCount = completed.size,
                hasActiveCycle = active != null,
            )
        }

        /**
         * Simple two-half comparison. If the back half average is >1 day lower than
         * the front half, that's DECREASING; >1 day higher is INCREASING; else STABLE.
         * Requires >=4 data points.
         */
        private fun trendOf(values: List<Int>): Trend {
            if (values.size < 4) return Trend.UNKNOWN
            val mid = values.size / 2
            val front = values.subList(0, mid).average()
            val back = values.subList(values.size - mid, values.size).average()
            val delta = back - front
            return when {
                delta < -0.5 -> Trend.DECREASING
                delta > 0.5 -> Trend.INCREASING
                else -> Trend.STABLE
            }
        }

        private fun List<Int>.medianOrNull(): Int? {
            if (isEmpty()) return null
            val sorted = sorted()
            return sorted[(sorted.size - 1) / 2]
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.CycleStatsTest"
```

Expected: 8 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/CycleStats.kt \
        app/src/test/java/com/hayate0726/tides/domain/CycleStatsTest.kt
git commit -m "feat(domain): add CycleStats with median, variance, regularity, trend"
```

---

## Task 4: CyclePredictor — next-period range with confidence

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/CyclePredictor.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/CyclePredictorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/domain/CyclePredictorTest.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.PredictionRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CyclePredictorTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            LocalDate.parse(start),
            LocalDate.parse(periodEnd),
            nextStart?.let { LocalDate.parse(it) }
        )

    @Test
    fun `returns null when fewer than 2 completed cycles`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        assertNull(CyclePredictor.predictNextPeriod(listOf(active)))
    }

    @Test
    fun `regular cycles produce a narrow HIGH-confidence range`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 28
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 28
            cycle("2026-02-26", "2026-03-02", "2026-03-26"),  // 28
            cycle("2026-03-26", "2026-03-30", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)
        assertNotNull(range)
        assertEquals(PredictionRange.Confidence.HIGH, range!!.confidence)
        // Expected median start: active start + 28 = 2026-04-23
        assertTrue(range.start <= LocalDate.parse("2026-04-23"))
        assertTrue(range.end >= LocalDate.parse("2026-04-23"))
        // Narrow: <= 5 days wide
        val width = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end).toInt()
        assertTrue(width <= 4, "expected narrow range, got width=$width")
    }

    @Test
    fun `moderately variable cycles produce MEDIUM confidence and wider range`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-28"),  // 27
            cycle("2026-01-28", "2026-02-01", "2026-02-25"),  // 28
            cycle("2026-02-25", "2026-03-01", "2026-03-29"),  // 32 (variance up to 5)
            cycle("2026-03-29", "2026-04-02", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)!!
        assertEquals(PredictionRange.Confidence.MEDIUM, range.confidence)
        val width = java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end).toInt()
        assertTrue(width >= 4, "expected wider range, got width=$width")
    }

    @Test
    fun `highly variable cycles produce LOW confidence and a wide range`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-22"),  // 21
            cycle("2026-01-22", "2026-01-26", "2026-02-26"),  // 35
            cycle("2026-02-26", "2026-03-02", "2026-04-10"),  // 43
            cycle("2026-04-10", "2026-04-14", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)!!
        assertEquals(PredictionRange.Confidence.LOW, range.confidence)
    }

    @Test
    fun `predicted range is centered on median next-start date`() {
        // All 28-day cycles → predicted next start = active.start + 28
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),
            cycle("2026-02-26", "2026-03-02", null),
        )
        val range = CyclePredictor.predictNextPeriod(cycles)!!
        val expected = LocalDate.parse("2026-03-26")
        val midpoint = range.start.plusDays(
            java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end) / 2
        )
        // The expected date must be within the range; allow off-by-one for rounding.
        assertTrue(
            !expected.isBefore(range.start.minusDays(1)) &&
            !expected.isAfter(range.end.plusDays(1)),
            "expected $expected to be near range center, got [$range]"
        )
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.CyclePredictorTest"
```

Expected: FAIL.

- [ ] **Step 3: Implement `CyclePredictor`**

Create `app/src/main/java/com/hayate0726/tides/domain/CyclePredictor.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.PredictionRange
import java.time.LocalDate

/**
 * Predicts the next period as a range, with a coarse confidence bucket
 * derived from cycle-length variance.
 *
 * Rules (spec §5.5, §10):
 *  - Requires ≥2 completed cycles plus an active cycle (or just ≥2 completed
 *    cycles if the most recent has finished).
 *  - Predicted median next-start = active cycle start + median completed length.
 *  - Range width = max(2, ceil(variance / 2)) days on each side.
 *  - Confidence: variance ≤2 → HIGH, ≤7 → MEDIUM, >7 → LOW.
 */
object CyclePredictor {

    fun predictNextPeriod(cycles: List<Cycle>): PredictionRange? {
        val completed = cycles.filter { !it.isActive }.mapNotNull { c -> c.length?.let { c to it } }
        if (completed.size < 2) return null

        val lengths = completed.map { it.second }.sorted()
        val median = lengths[(lengths.size - 1) / 2]
        val variance = lengths.max() - lengths.min()

        val active = cycles.firstOrNull { it.isActive } ?: completed.last().first

        val center = active.start.plusDays(median.toLong())
        val halfWidth = maxOf(2, (variance + 1) / 2)
        val start = center.minusDays(halfWidth.toLong())
        val end = center.plusDays(halfWidth.toLong())

        val confidence = when {
            variance <= 2 -> PredictionRange.Confidence.HIGH
            variance <= 7 -> PredictionRange.Confidence.MEDIUM
            else -> PredictionRange.Confidence.LOW
        }
        return PredictionRange(start, end, confidence)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.CyclePredictorTest"
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/CyclePredictor.kt \
        app/src/test/java/com/hayate0726/tides/domain/CyclePredictorTest.kt
git commit -m "feat(domain): add CyclePredictor (next-period range with confidence)"
```

---

## Task 5: PhaseCalculator — current phase, boundaries, ovulation window

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/PhaseCalculator.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/PhaseCalculatorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/domain/PhaseCalculatorTest.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Phase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PhaseCalculatorTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            LocalDate.parse(start),
            LocalDate.parse(periodEnd),
            nextStart?.let { LocalDate.parse(it) }
        )

    private val nonHormonal = BirthControlMethod.NONE
    private val hormonal = BirthControlMethod.PILL
    private val ovulationGoals = setOf(Goal.AVOID_PREGNANCY)
    private val noOvulationGoals = setOf(Goal.TRACK_PERIOD)

    @Test
    fun `phase is null when bc method is hormonal regardless of goals`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(active),
            today = LocalDate.parse("2026-05-14"),
            birthControl = hormonal,
            goals = ovulationGoals,
        )
        assertNull(result)
    }

    @Test
    fun `phase is null when goals do not include ovulation-relevant`() {
        val active = cycle("2026-05-01", "2026-05-04", null)
        val result = PhaseCalculator.compute(
            cycles = listOf(active),
            today = LocalDate.parse("2026-05-14"),
            birthControl = nonHormonal,
            goals = noOvulationGoals,
        )
        assertNull(result)
    }

    @Test
    fun `during period day 1-4 phase is MENSTRUAL`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.MENSTRUAL, result.currentPhase)
    }

    @Test
    fun `around day 14 (median 28-day cycle) phase is OVULATION`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-12"), // active cycle day 14
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.OVULATION, result.currentPhase)
    }

    @Test
    fun `between menstrual and ovulation phase is FOLLICULAR`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-06"), // active cycle day 8
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.FOLLICULAR, result.currentPhase)
    }

    @Test
    fun `after ovulation phase is LUTEAL`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-20"), // active cycle day 22
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        assertEquals(Phase.LUTEAL, result.currentPhase)
    }

    @Test
    fun `ovulation window is a contiguous 3-day span`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
            cycle("2026-04-29", "2026-05-02", null),
        )
        val result = PhaseCalculator.compute(
            cycles = cycles,
            today = LocalDate.parse("2026-05-12"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )!!
        val window = result.ovulationWindow
        assertEquals(3, window.toList().size)
    }

    @Test
    fun `single completed cycle with no active uses heuristic 28 days`() {
        // Only one completed cycle, no active → no inference possible
        val result = PhaseCalculator.compute(
            cycles = listOf(cycle("2026-04-01", "2026-04-04", "2026-04-29")),
            today = LocalDate.parse("2026-05-01"),
            birthControl = nonHormonal,
            goals = ovulationGoals,
        )
        assertNull(result)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.PhaseCalculatorTest"
```

Expected: FAIL.

- [ ] **Step 3: Implement `PhaseCalculator`**

Create `app/src/main/java/com/hayate0726/tides/domain/PhaseCalculator.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Phase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Computes the current cycle phase if it's meaningful to display.
 *
 * Returns null (phase suppressed) when:
 *  - The user is on hormonal contraception (PILL, HORMONAL_IUD, IMPLANT, PATCH, RING).
 *  - None of the user's goals include "Avoid pregnancy" or "Trying to conceive."
 *  - There's no active cycle.
 *  - There's no completed cycle to estimate a median length from.
 *
 * Phase boundaries (default 28-day cycle; scales proportionally for other lengths):
 *  - MENSTRUAL: cycle days 1 .. periodEnd-day-of-cycle (typically 1–5)
 *  - FOLLICULAR: periodEnd+1 .. ovulationStart-1
 *  - OVULATION: ovulationStart .. ovulationStart+2  (3-day window centered on day 14 of 28)
 *  - LUTEAL: ovulationEnd+1 .. cycle end
 */
object PhaseCalculator {

    private const val DEFAULT_CYCLE_LENGTH = 28
    private const val OVULATION_DAY_OF_DEFAULT = 14
    private const val OVULATION_WINDOW_HALF_WIDTH = 1

    data class Result(
        val currentPhase: Phase,
        val ovulationWindow: ClosedRange<LocalDate>,
    )

    fun compute(
        cycles: List<Cycle>,
        today: LocalDate,
        birthControl: BirthControlMethod,
        goals: Set<Goal>,
    ): Result? {
        if (birthControl.isHormonal) return null
        if (goals.intersect(Goal.OVULATION_RELEVANT).isEmpty()) return null

        val active = cycles.firstOrNull { it.isActive } ?: return null
        val completed = cycles.filter { !it.isActive }.mapNotNull { it.length }
        if (completed.isEmpty()) return null

        val medianLength = completed.sorted().let { it[(it.size - 1) / 2] }
        val ovulationDayOfCycle = (OVULATION_DAY_OF_DEFAULT.toDouble() * medianLength / DEFAULT_CYCLE_LENGTH).toInt()
            .coerceIn(1, medianLength)

        val cycleDay = ChronoUnit.DAYS.between(active.start, today).toInt() + 1
        if (cycleDay < 1) return null

        val periodLastDayOfCycle = active.periodLength ?: 5  // default if still bleeding

        val phase = when {
            cycleDay <= periodLastDayOfCycle -> Phase.MENSTRUAL
            cycleDay < ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH -> Phase.FOLLICULAR
            cycleDay <= ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH -> Phase.OVULATION
            else -> Phase.LUTEAL
        }

        val ovStart = active.start.plusDays((ovulationDayOfCycle - OVULATION_WINDOW_HALF_WIDTH - 1).toLong())
        val ovEnd = active.start.plusDays((ovulationDayOfCycle + OVULATION_WINDOW_HALF_WIDTH - 1).toLong())
        return Result(currentPhase = phase, ovulationWindow = ovStart..ovEnd)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.PhaseCalculatorTest"
```

Expected: 8 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/PhaseCalculator.kt \
        app/src/test/java/com/hayate0726/tides/domain/PhaseCalculatorTest.kt
git commit -m "feat(domain): add PhaseCalculator with suppression for hormonal BC and non-fertility goals"
```

---

## Task 6: SymptomStats — frequency, top symptoms, cycle-day heatmap

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/SymptomStats.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/SymptomStatsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/domain/SymptomStatsTest.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Symptom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SymptomStatsTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun entry(date: String, sym: Symptom) =
        SymptomStats.Entry(d(date), sym)

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(
            d(start),
            d(periodEnd),
            nextStart?.let { d(it) }
        )

    @Test
    fun `frequency counts each symptom`() {
        val entries = listOf(
            entry("2026-05-01", Symptom.CRAMPS),
            entry("2026-05-02", Symptom.CRAMPS),
            entry("2026-05-02", Symptom.HEADACHE),
        )
        val stats = SymptomStats.compute(entries, emptyList())
        assertEquals(2, stats.frequency[Symptom.CRAMPS])
        assertEquals(1, stats.frequency[Symptom.HEADACHE])
    }

    @Test
    fun `topSymptoms returns N most frequent`() {
        val entries = listOf(
            entry("2026-05-01", Symptom.CRAMPS),
            entry("2026-05-02", Symptom.CRAMPS),
            entry("2026-05-03", Symptom.CRAMPS),
            entry("2026-05-01", Symptom.HEADACHE),
            entry("2026-05-02", Symptom.HEADACHE),
            entry("2026-05-01", Symptom.BLOATING),
        )
        val stats = SymptomStats.compute(entries, emptyList())
        val top2 = stats.topSymptoms(2)
        assertEquals(listOf(Symptom.CRAMPS, Symptom.HEADACHE), top2)
    }

    @Test
    fun `OTHER symptoms are excluded from frequency stats`() {
        val entries = listOf(
            entry("2026-05-01", Symptom.OTHER),
            entry("2026-05-01", Symptom.CRAMPS),
        )
        val stats = SymptomStats.compute(entries, emptyList())
        assertTrue(stats.frequency[Symptom.OTHER] == null || stats.frequency[Symptom.OTHER] == 0)
        assertEquals(1, stats.frequency[Symptom.CRAMPS])
    }

    @Test
    fun `cycleDayHeatmap maps symptoms to cycle days`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),  // 28 days
            cycle("2026-04-29", "2026-05-02", "2026-05-27"),  // 28 days
        )
        val entries = listOf(
            // Cramps on day 1 of each cycle (4-1, 4-29)
            entry("2026-04-01", Symptom.CRAMPS),
            entry("2026-04-29", Symptom.CRAMPS),
            // Headache on day 14 of cycle 2 (5-12)
            entry("2026-05-12", Symptom.HEADACHE),
        )
        val stats = SymptomStats.compute(entries, cycles)
        val heat = stats.cycleDayHeatmap
        assertEquals(2, heat[Symptom.CRAMPS]?.get(1))
        assertEquals(1, heat[Symptom.HEADACHE]?.get(14))
    }

    @Test
    fun `entries outside any cycle are not counted in heatmap`() {
        val cycles = listOf(
            cycle("2026-04-01", "2026-04-04", "2026-04-29"),
        )
        val entries = listOf(
            entry("2025-12-01", Symptom.CRAMPS),  // before any cycle
            entry("2026-04-01", Symptom.CRAMPS),  // inside cycle
        )
        val stats = SymptomStats.compute(entries, cycles)
        // Both still count in frequency
        assertEquals(2, stats.frequency[Symptom.CRAMPS])
        // But only the one inside a cycle is in heatmap
        assertEquals(1, stats.cycleDayHeatmap[Symptom.CRAMPS]?.values?.sum())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.SymptomStatsTest"
```

Expected: FAIL.

- [ ] **Step 3: Implement `SymptomStats`**

Create `app/src/main/java/com/hayate0726/tides/domain/SymptomStats.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Aggregates symptom data into frequency counts and cycle-day heatmaps.
 *
 * Spec §4: OTHER symptoms are excluded from `frequency` and `cycleDayHeatmap`
 * because their `other_text` content is free-form. They surface only in
 * the doctor PDF appendix (handled in Plan 4).
 */
data class SymptomStats(
    /** Symptom → number of log entries. Excludes OTHER. */
    val frequency: Map<Symptom, Int>,
    /** Symptom → (cycle day 1-based → count of entries on that cycle day). */
    val cycleDayHeatmap: Map<Symptom, Map<Int, Int>>,
) {
    fun topSymptoms(n: Int): List<Symptom> =
        frequency.entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key }

    companion object {

        data class Entry(val date: LocalDate, val symptom: Symptom)

        fun compute(entries: List<Entry>, cycles: List<Cycle>): SymptomStats {
            val freq = mutableMapOf<Symptom, Int>()
            for (e in entries) {
                if (e.symptom.isFreeText) continue
                freq[e.symptom] = (freq[e.symptom] ?: 0) + 1
            }

            val heat = mutableMapOf<Symptom, MutableMap<Int, Int>>()
            for (e in entries) {
                if (e.symptom.isFreeText) continue
                val containing = cycles.firstOrNull { c ->
                    val end = c.nextStart?.minusDays(1) ?: e.date
                    !e.date.isBefore(c.start) && !e.date.isAfter(end)
                } ?: continue
                val cycleDay = ChronoUnit.DAYS.between(containing.start, e.date).toInt() + 1
                val inner = heat.getOrPut(e.symptom) { mutableMapOf() }
                inner[cycleDay] = (inner[cycleDay] ?: 0) + 1
            }

            return SymptomStats(frequency = freq, cycleDayHeatmap = heat)
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.SymptomStatsTest"
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/SymptomStats.kt \
        app/src/test/java/com/hayate0726/tides/domain/SymptomStatsTest.kt
git commit -m "feat(domain): add SymptomStats (frequency, top-N, cycle-day heatmap)"
```

---

## Task 7: FigoAnalysis — detect clinical patterns of note

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/domain/FigoAnalysis.kt`
- Create: `app/src/test/java/com/hayate0726/tides/domain/FigoAnalysisTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/domain/FigoAnalysisTest.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FigoAnalysisTest {

    private fun cycle(start: String, periodEnd: String, nextStart: String?) =
        Cycle(LocalDate.parse(start), LocalDate.parse(periodEnd), nextStart?.let { LocalDate.parse(it) })

    @Test
    fun `regular cycles flag no FIGO patterns`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),  // 28, 5
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),  // 28, 5
            cycle("2026-02-26", "2026-03-02", "2026-03-26"),  // 28, 5
        )
        val patterns = FigoAnalysis.analyze(
            cycles = cycles,
            cycleFlowEntries = emptyList(),
            painEntries = emptyList(),
            intermenstrualBleedingDates = emptyList(),
        )
        assertTrue(patterns.isEmpty(), "expected no patterns, got $patterns")
    }

    @Test
    fun `frequent cycles (less than 24 days) flag CYCLE_FREQUENT`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-22"),  // 21
            cycle("2026-01-22", "2026-01-26", "2026-02-12"),  // 21
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList())
        assertTrue(patterns.contains(FigoAnalysis.Pattern.CYCLE_FREQUENT))
    }

    @Test
    fun `infrequent cycles (over 38 days) flag CYCLE_INFREQUENT`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-02-15"),  // 45
            cycle("2026-02-15", "2026-02-19", "2026-04-01"),  // 45
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList())
        assertTrue(patterns.contains(FigoAnalysis.Pattern.CYCLE_INFREQUENT))
    }

    @Test
    fun `variation greater than 7 days flags CYCLE_IRREGULAR`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-23"),  // 22
            cycle("2026-01-23", "2026-01-27", "2026-02-23"),  // 31
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList())
        assertTrue(patterns.contains(FigoAnalysis.Pattern.CYCLE_IRREGULAR))
    }

    @Test
    fun `period longer than 8 days flags PERIOD_PROLONGED`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-10", "2026-01-29"),  // 9-day period
        )
        val patterns = FigoAnalysis.analyze(cycles, emptyList(), emptyList(), emptyList())
        assertTrue(patterns.contains(FigoAnalysis.Pattern.PERIOD_PROLONGED))
    }

    @Test
    fun `intermenstrual bleeding flags INTERMENSTRUAL_BLEEDING`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            emptyList(),
            listOf(LocalDate.parse("2026-01-15")),
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.INTERMENSTRUAL_BLEEDING))
    }

    @Test
    fun `severe dysmenorrhea (pain greater than equal to 7) flags SEVERE_DYSMENORRHEA`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            painEntries = listOf(
                FigoAnalysis.PainEntry(LocalDate.parse("2026-01-01"), 8),
            ),
            intermenstrualBleedingDates = emptyList(),
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.SEVERE_DYSMENORRHEA))
    }

    @Test
    fun `heavy flow logged repeatedly flags HEAVY_FLOW`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", "2026-01-29"),
            cycle("2026-01-29", "2026-02-02", "2026-02-26"),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            cycleFlowEntries = listOf(
                FigoAnalysis.FlowEntry(LocalDate.parse("2026-01-01"), FlowIntensity.HEAVY),
                FigoAnalysis.FlowEntry(LocalDate.parse("2026-01-29"), FlowIntensity.HEAVY),
            ),
            painEntries = emptyList(),
            intermenstrualBleedingDates = emptyList(),
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.HEAVY_FLOW))
    }

    @Test
    fun `amenorrhea 90+ days flags AMENORRHEA`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", null),  // active, never had next period
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            emptyList(),
            emptyList(),
            today = LocalDate.parse("2026-05-01"),  // 120 days later
        )
        assertTrue(patterns.contains(FigoAnalysis.Pattern.AMENORRHEA))
    }

    @Test
    fun `amenorrhea under 90 days does NOT flag`() {
        val cycles = listOf(
            cycle("2026-01-01", "2026-01-05", null),
        )
        val patterns = FigoAnalysis.analyze(
            cycles,
            emptyList(),
            emptyList(),
            emptyList(),
            today = LocalDate.parse("2026-03-01"),  // 59 days later
        )
        assertFalse(patterns.contains(FigoAnalysis.Pattern.AMENORRHEA))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.FigoAnalysisTest"
```

Expected: FAIL.

- [ ] **Step 3: Implement `FigoAnalysis`**

Create `app/src/main/java/com/hayate0726/tides/domain/FigoAnalysis.kt`:

```kotlin
package com.hayate0726.tides.domain

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Detects FIGO-aligned clinical patterns to surface in the doctor PDF.
 *
 * Per spec §5.7 and ACOG #651 / Munro 2018:
 *  - CYCLE_FREQUENT: median cycle <24 days
 *  - CYCLE_INFREQUENT: median cycle >38 days
 *  - CYCLE_IRREGULAR: max-min variation >7 days across the analyzed range
 *  - PERIOD_PROLONGED: period duration >8 days in any cycle
 *  - HEAVY_FLOW: HEAVY flow logged in ≥2 cycles
 *  - INTERMENSTRUAL_BLEEDING: any bleeding logged between defined period ranges
 *  - SEVERE_DYSMENORRHEA: any pain entry ≥7/10 (NRS)
 *  - AMENORRHEA: ≥90 days since last period start with no new period
 *
 * All findings are descriptive — never diagnostic. The PDF wraps these in
 * neutral language per the spec.
 */
object FigoAnalysis {

    data class FlowEntry(val date: LocalDate, val flow: FlowIntensity)
    data class PainEntry(val date: LocalDate, val severityNrs: Int) {
        init { require(severityNrs in 0..10) }
    }

    enum class Pattern {
        CYCLE_FREQUENT,
        CYCLE_INFREQUENT,
        CYCLE_IRREGULAR,
        PERIOD_PROLONGED,
        HEAVY_FLOW,
        INTERMENSTRUAL_BLEEDING,
        SEVERE_DYSMENORRHEA,
        AMENORRHEA,
    }

    fun analyze(
        cycles: List<Cycle>,
        cycleFlowEntries: List<FlowEntry>,
        painEntries: List<PainEntry>,
        intermenstrualBleedingDates: List<LocalDate>,
        today: LocalDate = LocalDate.now(),
    ): Set<Pattern> {
        val found = mutableSetOf<Pattern>()
        val completed = cycles.filter { !it.isActive }
        val lengths = completed.mapNotNull { it.length }

        if (lengths.isNotEmpty()) {
            val median = lengths.sorted()[(lengths.size - 1) / 2]
            if (median < 24) found += Pattern.CYCLE_FREQUENT
            if (median > 38) found += Pattern.CYCLE_INFREQUENT
            val variance = lengths.max() - lengths.min()
            if (variance > 7) found += Pattern.CYCLE_IRREGULAR
        }

        if (completed.any { (it.periodLength ?: 0) > 8 }) {
            found += Pattern.PERIOD_PROLONGED
        }

        val heavyCycles = cycles.count { c ->
            cycleFlowEntries.any { e ->
                !e.date.isBefore(c.start) &&
                    (c.nextStart == null || e.date.isBefore(c.nextStart)) &&
                    FlowIntensity.isHeavy(e.flow)
            }
        }
        if (heavyCycles >= 2) found += Pattern.HEAVY_FLOW

        if (intermenstrualBleedingDates.isNotEmpty()) {
            found += Pattern.INTERMENSTRUAL_BLEEDING
        }

        if (painEntries.any { it.severityNrs >= 7 }) {
            found += Pattern.SEVERE_DYSMENORRHEA
        }

        val mostRecentStart = cycles.maxOfOrNull { it.start }
        if (mostRecentStart != null) {
            val daysSince = ChronoUnit.DAYS.between(mostRecentStart, today)
            val activeAndNoNew = cycles.any { it.start == mostRecentStart && it.isActive }
            if (activeAndNoNew && daysSince >= 90) {
                found += Pattern.AMENORRHEA
            }
        }

        return found
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.domain.FigoAnalysisTest"
```

Expected: 9 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/domain/FigoAnalysis.kt \
        app/src/test/java/com/hayate0726/tides/domain/FigoAnalysisTest.kt
git commit -m "feat(domain): add FigoAnalysis for FIGO-aligned pattern detection"
```

---

## Task 8: Room entities

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/data/Converters.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/entity/CycleEntryEntity.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/entity/SymptomEntryEntity.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/entity/BirthControlEntity.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/entity/SettingsEntity.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/entity/GoalEntity.kt`
- Modify: `app/src/main/java/com/hayate0726/tides/data/TidesDatabase.kt`
- Delete: `app/src/main/java/com/hayate0726/tides/data/Placeholder.kt`

- [ ] **Step 1: Create `Converters.kt`**

Create `app/src/main/java/com/hayate0726/tides/data/Converters.kt`:

```kotlin
package com.hayate0726.tides.data

import androidx.room.TypeConverter
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun localDateToEpochDay(d: LocalDate?): Long? = d?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epoch: Long?): LocalDate? = epoch?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun flowToInt(f: FlowIntensity): Int = f.intCode

    @TypeConverter
    fun flowFromInt(i: Int): FlowIntensity = FlowIntensity.fromInt(i)

    @TypeConverter
    fun symptomToString(s: Symptom): String = s.name

    @TypeConverter
    fun symptomFromString(s: String): Symptom = Symptom.valueOf(s)

    @TypeConverter
    fun bcMethodToString(m: BirthControlMethod): String = m.name

    @TypeConverter
    fun bcMethodFromString(s: String): BirthControlMethod = BirthControlMethod.valueOf(s)
}
```

- [ ] **Step 2: Create entity files**

Create `app/src/main/java/com/hayate0726/tides/data/entity/CycleEntryEntity.kt`:

```kotlin
package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate

/**
 * One day-level entry for flow intensity, optional pain (NRS 0-10),
 * and optional free-text notes. The primary key is the date — one entry
 * per day, last-write-wins.
 */
@Entity(tableName = "cycle_entries")
data class CycleEntryEntity(
    @PrimaryKey val date: LocalDate,
    val flowIntensity: FlowIntensity,
    val painSeverity: Int?,  // 0-10 NRS, nullable
    val notes: String?,
)
```

Create `app/src/main/java/com/hayate0726/tides/data/entity/SymptomEntryEntity.kt`:

```kotlin
package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

/**
 * One logged symptom for a date. A user can log many symptoms per day,
 * so primary key is autoIncrement int. The (date, symptom) pair is indexed
 * for efficient frequency queries.
 *
 * `severity` is 0..2 (mild/moderate/severe).
 * `otherText` is null unless `symptom == OTHER`.
 */
@Entity(
    tableName = "symptom_entries",
    indices = [Index(value = ["date", "symptom"])],
)
data class SymptomEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val symptom: Symptom,
    val severity: Int,
    val otherText: String?,
)
```

Create `app/src/main/java/com/hayate0726/tides/data/entity/BirthControlEntity.kt`:

```kotlin
package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.BirthControlMethod
import java.time.LocalDate

/**
 * The user's birth control method over time. Each entry is a span:
 * `startDate` (inclusive) .. `endDate` (exclusive). The active row has
 * `endDate == null`.
 */
@Entity(tableName = "birth_control")
data class BirthControlEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: BirthControlMethod,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)
```

Create `app/src/main/java/com/hayate0726/tides/data/entity/SettingsEntity.kt`:

```kotlin
package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simple key/value table for app settings. Keys are strings to keep migrations
 * trivial (adding a new setting is a write, not a schema change).
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
)
```

Create `app/src/main/java/com/hayate0726/tides/data/entity/GoalEntity.kt`:

```kotlin
package com.hayate0726.tides.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hayate0726.tides.domain.model.Goal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val goal: Goal,
)
```

Note: `GoalEntity` uses the `Goal` enum directly as primary key. Room needs a converter for this — add to Converters.kt now.

Edit `app/src/main/java/com/hayate0726/tides/data/Converters.kt` to add Goal converters:

```kotlin
    @TypeConverter
    fun goalToString(g: com.hayate0726.tides.domain.model.Goal): String = g.name

    @TypeConverter
    fun goalFromString(s: String): com.hayate0726.tides.domain.model.Goal =
        com.hayate0726.tides.domain.model.Goal.valueOf(s)
```

(Add these inside the `Converters` class.)

- [ ] **Step 3: Delete the placeholder entity**

```bash
rm app/src/main/java/com/hayate0726/tides/data/Placeholder.kt
```

- [ ] **Step 4: Replace `TidesDatabase.kt` with the real schema**

Overwrite `app/src/main/java/com/hayate0726/tides/data/TidesDatabase.kt`:

```kotlin
package com.hayate0726.tides.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hayate0726.tides.data.dao.BirthControlDao
import com.hayate0726.tides.data.dao.CycleEntryDao
import com.hayate0726.tides.data.dao.GoalDao
import com.hayate0726.tides.data.dao.SettingsDao
import com.hayate0726.tides.data.dao.SymptomEntryDao
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.GoalEntity
import com.hayate0726.tides.data.entity.SettingsEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity

@Database(
    entities = [
        CycleEntryEntity::class,
        SymptomEntryEntity::class,
        BirthControlEntity::class,
        SettingsEntity::class,
        GoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TidesDatabase : RoomDatabase() {
    abstract fun cycleEntryDao(): CycleEntryDao
    abstract fun symptomEntryDao(): SymptomEntryDao
    abstract fun birthControlDao(): BirthControlDao
    abstract fun settingsDao(): SettingsDao
    abstract fun goalDao(): GoalDao
}
```

This will fail to compile until the DAOs exist (next task). Don't run the build yet.

- [ ] **Step 5: Configure schema export directory**

Add to `app/build.gradle.kts` inside the `android { defaultConfig { } }` block (the file from Plan 1):

```kotlin
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
```

This requires importing the KSP plugin block at the top level — already done in Plan 1's `build.gradle.kts`. If your build complains, the equivalent for the KSP DSL is to add at the bottom of `app/build.gradle.kts`:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

- [ ] **Step 6: Commit (intermediate — DAOs are next)**

```bash
git add app/src/main/java/com/hayate0726/tides/data
git rm app/src/main/java/com/hayate0726/tides/data/Placeholder.kt 2>/dev/null || true
git commit -m "feat(data): add real Room entities and Converters (DAOs in next commit)"
```

---

## Task 9: Room DAOs

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/data/dao/CycleEntryDao.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/dao/SymptomEntryDao.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/dao/BirthControlDao.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/dao/SettingsDao.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/dao/GoalDao.kt`

- [ ] **Step 1: Create `CycleEntryDao`**

Create `app/src/main/java/com/hayate0726/tides/data/dao/CycleEntryDao.kt`:

```kotlin
package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hayate0726.tides.data.entity.CycleEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CycleEntryDao {

    @Upsert
    suspend fun upsert(entry: CycleEntryEntity)

    @Query("DELETE FROM cycle_entries WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM cycle_entries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): CycleEntryEntity?

    @Query("SELECT * FROM cycle_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun rangeOnce(from: LocalDate, to: LocalDate): List<CycleEntryEntity>

    @Query("SELECT * FROM cycle_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun rangeFlow(from: LocalDate, to: LocalDate): Flow<List<CycleEntryEntity>>

    @Query("SELECT * FROM cycle_entries ORDER BY date ASC")
    suspend fun all(): List<CycleEntryEntity>
}
```

- [ ] **Step 2: Create `SymptomEntryDao`**

Create `app/src/main/java/com/hayate0726/tides/data/dao/SymptomEntryDao.kt`:

```kotlin
package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SymptomEntryDao {

    @Insert
    suspend fun insert(entry: SymptomEntryEntity): Long

    @Query("DELETE FROM symptom_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM symptom_entries WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM symptom_entries WHERE date = :date ORDER BY symptom ASC")
    suspend fun getByDate(date: LocalDate): List<SymptomEntryEntity>

    @Query("SELECT * FROM symptom_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun rangeOnce(from: LocalDate, to: LocalDate): List<SymptomEntryEntity>

    @Query("SELECT * FROM symptom_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun rangeFlow(from: LocalDate, to: LocalDate): Flow<List<SymptomEntryEntity>>

    @Query("SELECT * FROM symptom_entries ORDER BY date ASC")
    suspend fun all(): List<SymptomEntryEntity>
}
```

- [ ] **Step 3: Create `BirthControlDao`**

Create `app/src/main/java/com/hayate0726/tides/data/dao/BirthControlDao.kt`:

```kotlin
package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hayate0726.tides.data.entity.BirthControlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthControlDao {

    @Insert
    suspend fun insert(row: BirthControlEntity): Long

    @Update
    suspend fun update(row: BirthControlEntity)

    @Query("SELECT * FROM birth_control WHERE endDate IS NULL LIMIT 1")
    suspend fun activeOnce(): BirthControlEntity?

    @Query("SELECT * FROM birth_control WHERE endDate IS NULL LIMIT 1")
    fun activeFlow(): Flow<BirthControlEntity?>

    @Query("SELECT * FROM birth_control ORDER BY startDate ASC")
    suspend fun all(): List<BirthControlEntity>
}
```

- [ ] **Step 4: Create `SettingsDao`**

Create `app/src/main/java/com/hayate0726/tides/data/dao/SettingsDao.kt`:

```kotlin
package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hayate0726.tides.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Upsert
    suspend fun upsert(row: SettingsEntity)

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)
}
```

- [ ] **Step 5: Create `GoalDao`**

Create `app/src/main/java/com/hayate0726/tides/data/dao/GoalDao.kt`:

```kotlin
package com.hayate0726.tides.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hayate0726.tides.data.entity.GoalEntity
import com.hayate0726.tides.domain.model.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE goal = :goal")
    suspend fun delete(goal: Goal)

    @Query("DELETE FROM goals")
    suspend fun clearAll()

    @Query("SELECT goal FROM goals")
    suspend fun all(): List<Goal>

    @Query("SELECT goal FROM goals")
    fun observeAll(): Flow<List<Goal>>
}
```

- [ ] **Step 6: Build to verify compilation**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. The Room schema JSON should be generated at `app/schemas/com.hayate0726.tides.data.TidesDatabase/1.json`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/data/dao app/schemas
git commit -m "feat(data): add DAOs for all entities (Cycle, Symptom, BirthControl, Settings, Goal)"
```

---

## Task 10: Update DatabaseFactory and existing tests for the new schema

**Files:**
- Modify: `app/src/main/java/com/hayate0726/tides/data/DatabaseFactory.kt` (no signature change; only an internal note)
- Modify: `app/src/androidTest/java/com/hayate0726/tides/data/DatabaseFactoryTest.kt`
- Modify: `app/src/androidTest/java/com/hayate0726/tides/security/KeyNotOnDiskTest.kt`
- Modify: `app/src/androidTest/java/com/hayate0726/tides/security/NoSensitiveLogsTest.kt`

The placeholder usage in these Plan 1 tests must be replaced. The DatabaseFactory signature is unchanged.

- [ ] **Step 1: Update `DatabaseFactoryTest` to use real entities**

Overwrite `app/src/androidTest/java/com/hayate0726/tides/data/DatabaseFactoryTest.kt`:

```kotlin
package com.hayate0726.tides.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseFactoryTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "test_roundtrip.db")
        dbFile.delete()
    }

    @After
    fun tearDown() { dbFile.delete() }

    @Test
    fun create_and_reopen_with_same_key_succeeds() = runBlocking {
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.cycleEntryDao().upsert(
            CycleEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                flowIntensity = FlowIntensity.MEDIUM,
                painSeverity = 3,
                notes = "hello",
            )
        )
        db.close()
        key.zero()

        val key2 = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val db2 = DatabaseFactory.open(ctx, dbFile, key2)
        val rows = db2.cycleEntryDao().all()
        assertEquals(1, rows.size)
        assertEquals("hello", rows[0].notes)
        assertEquals(FlowIntensity.MEDIUM, rows[0].flowIntensity)
        db2.close()
        key2.zero()
    }

    @Test
    fun reopen_with_wrong_key_fails() = runBlocking {
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        DatabaseFactory.open(ctx, dbFile, key).also {
            it.cycleEntryDao().upsert(
                CycleEntryEntity(
                    date = LocalDate.parse("2026-05-01"),
                    flowIntensity = FlowIntensity.LIGHT,
                    painSeverity = null,
                    notes = "data",
                )
            )
            it.close()
        }
        key.zero()

        val wrongKey = KeyDerivation.deriveKey(Pin("999999".toCharArray()), ByteArray(16) { 1 })
        try {
            val db2 = DatabaseFactory.open(ctx, dbFile, wrongKey)
            db2.cycleEntryDao().all()
            db2.close()
            fail("expected SQLCipher to reject wrong key")
        } catch (e: Exception) {
            assertNotNull(e)
        } finally {
            wrongKey.zero()
        }
    }
}
```

- [ ] **Step 2: Update `KeyNotOnDiskTest`**

Overwrite `app/src/androidTest/java/com/hayate0726/tides/security/KeyNotOnDiskTest.kt`:

```kotlin
package com.hayate0726.tides.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class KeyNotOnDiskTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @After
    fun cleanup() {
        if (::dbFile.isInitialized) dbFile.delete()
    }

    @Test
    fun no_app_private_file_contains_the_derived_key_bytes() = runBlocking {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "key_leak_check.db")
        dbFile.delete()

        val key: DbKey = KeyDerivation.deriveKey(
            Pin("123456".toCharArray()),
            ByteArray(16) { 11 }
        )
        val keyBytes = key.bytes.copyOf()

        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.cycleEntryDao().upsert(
            CycleEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                flowIntensity = FlowIntensity.LIGHT,
                painSeverity = null,
                notes = "x",
            )
        )
        db.close()
        key.zero()

        val roots = listOf(ctx.filesDir, ctx.cacheDir, ctx.dataDir)
        for (root in roots) {
            scanFiles(root) { f ->
                val data = f.readBytes()
                assertFalse(
                    "Key bytes found in app-private file: ${f.absolutePath}",
                    contains(data, keyBytes),
                )
            }
        }
    }

    private fun scanFiles(root: File, body: (File) -> Unit) {
        if (!root.exists()) return
        root.walkTopDown().forEach { f -> if (f.isFile && f.length() > 0L) body(f) }
    }

    private fun contains(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }
}
```

- [ ] **Step 3: Update `NoSensitiveLogsTest`**

Overwrite `app/src/androidTest/java/com/hayate0726/tides/security/NoSensitiveLogsTest.kt`:

```kotlin
package com.hayate0726.tides.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NoSensitiveLogsTest {

    private val secretMarker = "PIN_MARKER_4f8a9e2b"
    private val flowMarker = "FLOW_MARKER_dead1234"

    @Test
    fun logcat_does_not_leak_pin_or_flow_data() = runBlocking {
        Runtime.getRuntime().exec(arrayOf("logcat", "-c"))

        val ctx: Context = ApplicationProvider.getApplicationContext()
        val dbFile = java.io.File(ctx.filesDir, "log_leak_check.db")
        dbFile.delete()

        val pin = Pin(secretMarker.toCharArray())
        val key = KeyDerivation.deriveKey(pin, ByteArray(16) { 5 })
        pin.zero()

        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.cycleEntryDao().upsert(
            CycleEntryEntity(
                date = LocalDate.parse("2026-05-01"),
                flowIntensity = FlowIntensity.LIGHT,
                painSeverity = null,
                notes = flowMarker,
            )
        )
        db.close()
        key.zero()

        val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "raw"))
        val all = BufferedReader(InputStreamReader(proc.inputStream)).readText()
        proc.destroy()

        assertFalse("PIN content appeared in logcat", all.contains(secretMarker))
        assertFalse("Flow content appeared in logcat", all.contains(flowMarker))

        dbFile.delete()
    }
}
```

- [ ] **Step 4: Run all instrumented tests to verify nothing regressed**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: all instrumented tests pass (3 in DatabaseFactoryTest, 1 each in KeyNotOnDiskTest and NoSensitiveLogsTest, plus the Plan 1 tests that don't use placeholder).

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest
git commit -m "test(data): update Plan 1 instrumented tests for real Room schema"
```

---

## Task 11: CycleRepository — bridge between data and domain

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/data/CycleRepository.kt`
- Create: `app/src/androidTest/java/com/hayate0726/tides/data/CycleRepositoryTest.kt`

`CycleRepository` is the only class that reads from DAOs and produces `domain/` types. UI code talks to `CycleRepository`, not to DAOs directly.

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/hayate0726/tides/data/CycleRepositoryTest.kt`:

```kotlin
package com.hayate0726.tides.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CycleRepositoryTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File
    private lateinit var db: TidesDatabase
    private lateinit var repo: CycleRepository

    @Before
    fun setUp() = runBlocking {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "repo_test.db")
        dbFile.delete()
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16))
        db = DatabaseFactory.open(ctx, dbFile, key)
        repo = CycleRepository(
            db.cycleEntryDao(),
            db.symptomEntryDao(),
            db.birthControlDao(),
            db.goalDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun cycles_built_from_real_entries() = runBlocking {
        db.cycleEntryDao().upsert(CycleEntryEntity(LocalDate.parse("2026-05-01"), FlowIntensity.MEDIUM, null, null))
        db.cycleEntryDao().upsert(CycleEntryEntity(LocalDate.parse("2026-05-02"), FlowIntensity.LIGHT, null, null))
        db.cycleEntryDao().upsert(CycleEntryEntity(LocalDate.parse("2026-05-29"), FlowIntensity.MEDIUM, null, null))

        val cycles = repo.detectCycles(
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2027-01-01"),
        )
        assertEquals(2, cycles.size)
        assertEquals(LocalDate.parse("2026-05-01"), cycles[0].start)
        assertEquals(LocalDate.parse("2026-05-29"), cycles[1].start)
    }

    @Test
    fun symptom_entries_mapped_correctly() = runBlocking {
        db.symptomEntryDao().insert(SymptomEntryEntity(
            date = LocalDate.parse("2026-05-01"),
            symptom = Symptom.CRAMPS,
            severity = 2,
            otherText = null,
        ))
        val entries = repo.symptomEntriesInRange(
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-31"),
        )
        assertEquals(1, entries.size)
        assertEquals(Symptom.CRAMPS, entries[0].symptom)
    }

    @Test
    fun other_text_preserved_only_for_OTHER_symptom() = runBlocking {
        db.symptomEntryDao().insert(SymptomEntryEntity(
            date = LocalDate.parse("2026-05-01"),
            symptom = Symptom.OTHER,
            severity = 1,
            otherText = "weird tingling",
        ))
        val entries = repo.symptomEntriesInRange(
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-31"),
        )
        assertEquals("weird tingling", entries[0].otherText)
    }

    @Test
    fun active_bc_method_returned() = runBlocking {
        db.birthControlDao().insert(
            com.hayate0726.tides.data.entity.BirthControlEntity(
                method = com.hayate0726.tides.domain.model.BirthControlMethod.NONE,
                startDate = LocalDate.parse("2026-01-01"),
                endDate = null,
            )
        )
        val active = repo.activeBirthControl()
        assertTrue(active != null)
        assertEquals(
            com.hayate0726.tides.domain.model.BirthControlMethod.NONE,
            active!!.method,
        )
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.data.CycleRepositoryTest"
```

Expected: FAIL with "unresolved reference: CycleRepository"

- [ ] **Step 3: Implement `CycleRepository`**

Create `app/src/main/java/com/hayate0726/tides/data/CycleRepository.kt`:

```kotlin
package com.hayate0726.tides.data

import com.hayate0726.tides.data.dao.BirthControlDao
import com.hayate0726.tides.data.dao.CycleEntryDao
import com.hayate0726.tides.data.dao.GoalDao
import com.hayate0726.tides.data.dao.SymptomEntryDao
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import java.time.LocalDate

/**
 * Bridges Room DAOs to domain types. UI code uses this; it does not
 * touch DAOs directly.
 */
class CycleRepository(
    private val cycleEntryDao: CycleEntryDao,
    private val symptomEntryDao: SymptomEntryDao,
    private val birthControlDao: BirthControlDao,
    private val goalDao: GoalDao,
) {

    suspend fun detectCycles(from: LocalDate, to: LocalDate): List<Cycle> {
        val entries = cycleEntryDao.rangeOnce(from, to)
        return CycleDetector.detect(
            entries.map { CycleDetector.Entry(it.date, it.flowIntensity) }
        )
    }

    suspend fun symptomEntriesInRange(from: LocalDate, to: LocalDate): List<SymptomEntryEntity> =
        symptomEntryDao.rangeOnce(from, to)

    suspend fun activeBirthControl(): BirthControlEntity? = birthControlDao.activeOnce()

    suspend fun goals(): Set<Goal> = goalDao.all().toSet()
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.data.CycleRepositoryTest"
```

Expected: 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/data/CycleRepository.kt \
        app/src/androidTest/java/com/hayate0726/tides/data/CycleRepositoryTest.kt
git commit -m "feat(data): add CycleRepository to bridge DAOs and domain types"
```

---

## Task 12: Update DataModule and add SymptomEntry mapper

**Files:**
- Modify: `app/src/main/java/com/hayate0726/tides/di/DataModule.kt`

Plan 1's `DataModule` only provides the db file. We don't provide the database singleton itself — the DB lifecycle is tied to unlock state (Plan 3). The repository is constructed inside the Unlock ViewModel from a live DB. For now, we leave `DataModule` mostly as-is; the integration happens in Plan 3.

- [ ] **Step 1: Verify the Plan 1 `DataModule` still compiles after the schema change**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. No code change required.

- [ ] **Step 2: Commit (nothing to commit — but verify state)**

```bash
git status
```

Expected: working tree clean.

---

## Plan 2 acceptance criteria

Before marking Plan 2 complete and moving to Plan 3:

- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] `./gradlew :app:testDebugUnitTest` — all unit tests pass (Plan 1 tests still pass + new domain tests: SymptomTest, CycleDetectorTest, CycleStatsTest, CyclePredictorTest, PhaseCalculatorTest, SymptomStatsTest, FigoAnalysisTest)
- [ ] `./gradlew :app:connectedDebugAndroidTest` — all instrumented tests pass (Plan 1 tests pass with updated entities + new CycleRepositoryTest)
- [ ] `app/schemas/com.hayate0726.tides.data.TidesDatabase/1.json` exists (Room schema export)
- [ ] `app/src/main/java/com/hayate0726/tides/data/Placeholder.kt` is deleted
- [ ] CHANGELOG.md updated under "Plan 2: Data & Domain"

What Plan 2 does **not** produce:
- Any UI (Plan 3)
- Export, widget, notifications (Plan 4)

The repository can answer questions like "give me the cycles in May 2026" and the domain can do all the math, but nothing is wired to a screen yet.
