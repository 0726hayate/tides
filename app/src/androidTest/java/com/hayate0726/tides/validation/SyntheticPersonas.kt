package com.hayate0726.tides.validation

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Symptom

object SyntheticPersonas {

    // Typical (non-HMB) flow: HEAVY is rare. The FIGO HEAVY_FLOW pattern fires
    // when HEAVY is logged in ≥2 cycles; a 5-day period with 20% HEAVY/day
    // yields ~67% per-cycle HEAVY rate, which over 12-24 months guarantees
    // misclassification. Drop HEAVY entirely from this distribution — the
    // heavyFlow map covers HMB personas (COPPER_IUD).
    private val typicalFlow = mapOf(
        FlowIntensity.LIGHT to 0.4,
        FlowIntensity.MEDIUM to 0.6,
    )
    private val lightFlow = mapOf(
        FlowIntensity.SPOTTING to 0.4,
        FlowIntensity.LIGHT to 0.5,
        FlowIntensity.MEDIUM to 0.1,
    )
    private val heavyFlow = mapOf(
        FlowIntensity.MEDIUM to 0.3,
        FlowIntensity.HEAVY to 0.7,
    )

    private val mildSymptoms = mapOf(
        Symptom.CRAMPS to 0.2,
        Symptom.BLOATING to 0.15,
        Symptom.TIRED to 0.1,
    )
    private val pcosSymptoms = mapOf(
        Symptom.ACNE to 0.4,
        Symptom.TIRED to 0.3,
        Symptom.IRRITABLE to 0.25,
        Symptom.BLOATING to 0.25,
    )
    private val perimenopauseSymptoms = mapOf(
        Symptom.HOT_FLASHES to 0.4,
        Symptom.NIGHT_SWEATS to 0.3,
        Symptom.INSOMNIA to 0.3,
        Symptom.IRRITABLE to 0.2,
    )

    val all: List<PersonaSpec> = listOf(
        // Group 1: Typical adult
        PersonaSpec(
            id = "TYPICAL_25",
            populationSegment = PopulationSegment.TYPICAL,
            age = 28,
            historyMonths = 18,
            cycleLengthDays = DistParams(mean = 29.0, sd = 1.5, minClamp = 25, maxClamp = 33),
            periodLengthDays = DistParams(mean = 5.0, sd = 1.0, minClamp = 3, maxClamp = 7),
            anovulationRate = 0.05,
            symptomPrevalence = mildSymptoms,
            flowDistribution = typicalFlow,
            goals = emptySet(),
            birthControl = null,
        ),
        PersonaSpec(
            id = "TYPICAL_35",
            populationSegment = PopulationSegment.TYPICAL,
            age = 35,
            historyMonths = 24,
            // sd 3 + clamp 24-35 yields max-min variation up to 11 days, which
            // exceeds the FIGO CYCLE_IRREGULAR threshold (>7d). Tighten so a
            // "typical 35yo" stays within the regularity band.
            cycleLengthDays = DistParams(mean = 29.0, sd = 1.8, minClamp = 26, maxClamp = 32),
            periodLengthDays = DistParams(mean = 4.5, sd = 1.0, minClamp = 3, maxClamp = 7),
            anovulationRate = 0.1,
            symptomPrevalence = mildSymptoms,
            flowDistribution = typicalFlow,
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = null,
        ),
        PersonaSpec(
            id = "TYPICAL_TTC",
            populationSegment = PopulationSegment.TYPICAL,
            age = 30,
            historyMonths = 12,
            cycleLengthDays = DistParams(mean = 28.0, sd = 1.5, minClamp = 26, maxClamp = 31),
            periodLengthDays = DistParams(mean = 5.0, sd = 0.8, minClamp = 4, maxClamp = 6),
            anovulationRate = 0.05,
            symptomPrevalence = mildSymptoms,
            flowDistribution = typicalFlow,
            goals = setOf(Goal.TRYING_TO_CONCEIVE),
            birthControl = null,
        ),

        // Group 2: Irregular
        PersonaSpec(
            id = "PCOS_LONG",
            populationSegment = PopulationSegment.IRREGULAR_PCOS,
            age = 26,
            historyMonths = 12,
            cycleLengthDays = DistParams(mean = 52.0, sd = 14.0, minClamp = 35, maxClamp = 90),
            periodLengthDays = DistParams(mean = 5.0, sd = 1.5, minClamp = 3, maxClamp = 8),
            anovulationRate = 0.5,
            symptomPrevalence = pcosSymptoms,
            flowDistribution = typicalFlow,
            goals = setOf(Goal.TRYING_TO_CONCEIVE),
            birthControl = null,
        ),
        PersonaSpec(
            id = "PERIMENOPAUSE_47",
            populationSegment = PopulationSegment.IRREGULAR_PERIMENOPAUSE,
            age = 47,
            historyMonths = 18,
            cycleLengthDays = DistParams(mean = 32.0, sd = 12.0, minClamp = 18, maxClamp = 80),
            periodLengthDays = DistParams(mean = 5.0, sd = 2.0, minClamp = 2, maxClamp = 9),
            anovulationRate = 0.3,
            symptomPrevalence = perimenopauseSymptoms,
            flowDistribution = mapOf(
                FlowIntensity.LIGHT to 0.3,
                FlowIntensity.MEDIUM to 0.4,
                FlowIntensity.HEAVY to 0.3,
            ),
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = null,
        ),
        PersonaSpec(
            id = "ADOLESCENT_15",
            populationSegment = PopulationSegment.IRREGULAR_ADOLESCENT,
            age = 15,
            historyMonths = 14,
            cycleLengthDays = DistParams(mean = 35.0, sd = 14.0, minClamp = 21, maxClamp = 90),
            periodLengthDays = DistParams(mean = 4.5, sd = 1.5, minClamp = 2, maxClamp = 8),
            anovulationRate = 0.7,
            symptomPrevalence = mildSymptoms,
            flowDistribution = typicalFlow,
            goals = emptySet(),
            birthControl = null,
        ),

        // Group 3: Life transitions
        PersonaSpec(
            id = "POSTPARTUM_RESUMED",
            populationSegment = PopulationSegment.TRANSITION_POSTPARTUM,
            age = 32,
            historyMonths = 12,
            cycleLengthDays = DistParams(mean = 39.0, sd = 8.0, minClamp = 28, maxClamp = 65),
            periodLengthDays = DistParams(mean = 5.0, sd = 1.5, minClamp = 3, maxClamp = 8),
            anovulationRate = 0.4,
            symptomPrevalence = mildSymptoms,
            flowDistribution = typicalFlow,
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = null,
            // Generator emits cycles for the full 12 months; harness post-trims
            // the first 9 months to simulate lochia-free amenorrhea then 2-3
            // resumed cycles. Trim happens in PersonaTestHarness.
        ),
        PersonaSpec(
            id = "BC_STOPPED_3MO",
            populationSegment = PopulationSegment.TRANSITION_BC_STOPPED,
            age = 27,
            historyMonths = 15,
            cycleLengthDays = DistParams(mean = 32.0, sd = 6.0, minClamp = 25, maxClamp = 50),
            periodLengthDays = DistParams(mean = 5.0, sd = 1.0, minClamp = 3, maxClamp = 7),
            anovulationRate = 0.2,
            symptomPrevalence = mildSymptoms,
            flowDistribution = typicalFlow,
            goals = setOf(Goal.TRYING_TO_CONCEIVE),
            birthControl = null,
        ),
        PersonaSpec(
            id = "BC_STARTED_6MO",
            populationSegment = PopulationSegment.TRANSITION_BC_STARTED,
            age = 24,
            historyMonths = 18,
            cycleLengthDays = DistParams(mean = 28.0, sd = 0.5, minClamp = 28, maxClamp = 28),
            periodLengthDays = DistParams(mean = 4.0, sd = 0.5, minClamp = 3, maxClamp = 5),
            anovulationRate = 1.0,
            symptomPrevalence = mildSymptoms,
            flowDistribution = lightFlow,
            goals = emptySet(),
            birthControl = BirthControlMethod.PILL,
            bcStartedMonthsAgo = 6,
        ),

        // Group 4: Special — hormonal IUD variants + copper + athlete
        PersonaSpec(
            id = "HORMONAL_IUD_ADJUSTMENT",
            populationSegment = PopulationSegment.SPECIAL_HORMONAL_IUD,
            age = 28,
            historyMonths = 3,
            cycleLengthDays = DistParams(mean = 7.0, sd = 4.0, minClamp = 3, maxClamp = 14),
            periodLengthDays = DistParams(mean = 2.5, sd = 1.5, minClamp = 1, maxClamp = 5),
            anovulationRate = 1.0,
            symptomPrevalence = emptyMap(),
            // Adjustment phase is modeled as pure spotting to test the
            // hormonal-IUD spotting-floor assertion cleanly. Real adjustment
            // phase has occasional LIGHT breakthrough too, but mixing those in
            // here produces legitimately-detected short cycles between LIGHT
            // events — that's a separate clinical-detector question handled
            // by HORMONAL_IUD_SPOTTING_STEADY for the longer-gap case.
            flowDistribution = mapOf(FlowIntensity.SPOTTING to 1.0),
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = BirthControlMethod.HORMONAL_IUD,
            bcStartedMonthsAgo = 3,
        ),
        PersonaSpec(
            id = "HORMONAL_IUD_AMENORRHEA",
            populationSegment = PopulationSegment.SPECIAL_HORMONAL_IUD,
            age = 33,
            historyMonths = 18,
            // No bleeding at all — generator emits zero cycle entries when
            // anovulationRate is 1.0 AND flow distribution is all NONE.
            cycleLengthDays = DistParams(mean = 999.0, sd = 1.0, minClamp = 999, maxClamp = 999),
            periodLengthDays = DistParams(mean = 0.0, sd = 0.0, minClamp = 0, maxClamp = 0),
            anovulationRate = 1.0,
            symptomPrevalence = emptyMap(),
            flowDistribution = mapOf(FlowIntensity.NONE to 1.0),
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = BirthControlMethod.HORMONAL_IUD,
            bcStartedMonthsAgo = 18,
        ),
        PersonaSpec(
            id = "HORMONAL_IUD_SPOTTING_STEADY",
            populationSegment = PopulationSegment.SPECIAL_HORMONAL_IUD,
            age = 31,
            historyMonths = 12,
            cycleLengthDays = DistParams(mean = 50.0, sd = 25.0, minClamp = 30, maxClamp = 90),
            periodLengthDays = DistParams(mean = 1.5, sd = 0.5, minClamp = 1, maxClamp = 2),
            anovulationRate = 1.0,
            symptomPrevalence = emptyMap(),
            flowDistribution = mapOf(
                FlowIntensity.SPOTTING to 0.8,
                FlowIntensity.LIGHT to 0.2,
            ),
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = BirthControlMethod.HORMONAL_IUD,
            bcStartedMonthsAgo = 12,
        ),
        PersonaSpec(
            id = "HORMONAL_IUD_LOW_DOSE",
            populationSegment = PopulationSegment.SPECIAL_HORMONAL_IUD,
            age = 27,
            historyMonths = 12,
            cycleLengthDays = DistParams(mean = 30.0, sd = 3.0, minClamp = 26, maxClamp = 35),
            periodLengthDays = DistParams(mean = 3.0, sd = 1.0, minClamp = 2, maxClamp = 5),
            anovulationRate = 1.0,
            symptomPrevalence = mildSymptoms,
            flowDistribution = lightFlow,
            goals = setOf(Goal.TRYING_TO_CONCEIVE),
            birthControl = BirthControlMethod.HORMONAL_IUD,
            bcStartedMonthsAgo = 12,
        ),
        PersonaSpec(
            id = "COPPER_IUD",
            populationSegment = PopulationSegment.SPECIAL_COPPER_IUD,
            age = 29,
            historyMonths = 18,
            cycleLengthDays = DistParams(mean = 28.0, sd = 1.5, minClamp = 25, maxClamp = 32),
            periodLengthDays = DistParams(mean = 6.5, sd = 1.0, minClamp = 5, maxClamp = 8),
            anovulationRate = 0.05,
            symptomPrevalence = mapOf(
                Symptom.CRAMPS to 0.5,
                Symptom.BLOATING to 0.2,
            ),
            flowDistribution = heavyFlow,
            goals = setOf(Goal.TRYING_TO_CONCEIVE),
            birthControl = BirthControlMethod.COPPER_IUD,
            bcStartedMonthsAgo = 18,
        ),
        PersonaSpec(
            id = "ATHLETE_AMENORRHEA",
            populationSegment = PopulationSegment.SPECIAL_ATHLETE,
            age = 24,
            historyMonths = 18,
            cycleLengthDays = DistParams(mean = 120.0, sd = 40.0, minClamp = 80, maxClamp = 180),
            periodLengthDays = DistParams(mean = 3.0, sd = 1.0, minClamp = 2, maxClamp = 5),
            anovulationRate = 0.8,
            symptomPrevalence = mildSymptoms,
            flowDistribution = lightFlow,
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = null,
            // Deterministic ≥100d trailing gap → reliably triggers FIGO
            // AMENORRHEA (threshold is ≥90d since last period start).
            trailingAmenorrheaPadDays = 100,
        ),
    )
}
