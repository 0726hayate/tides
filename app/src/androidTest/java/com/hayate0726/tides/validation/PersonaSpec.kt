package com.hayate0726.tides.validation

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Symptom

enum class PopulationSegment {
    TYPICAL,
    IRREGULAR_PCOS,
    IRREGULAR_PERIMENOPAUSE,
    IRREGULAR_ADOLESCENT,
    TRANSITION_POSTPARTUM,
    TRANSITION_BC_STOPPED,
    TRANSITION_BC_STARTED,
    SPECIAL_HORMONAL_IUD,
    SPECIAL_COPPER_IUD,
    SPECIAL_ATHLETE,
    CASE_REPORT,
}

enum class DistShape { NORMAL, LOG_NORMAL }

data class DistParams(
    val mean: Double,
    val sd: Double,
    val shape: DistShape = DistShape.LOG_NORMAL,
    val minClamp: Int? = null,
    val maxClamp: Int? = null,
)

data class PersonaSpec(
    val id: String,
    val populationSegment: PopulationSegment,
    val age: Int,
    val historyMonths: Int,
    val cycleLengthDays: DistParams,
    val periodLengthDays: DistParams,
    /** 0.0 = always ovulatory, 1.0 = always anovulatory. */
    val anovulationRate: Double,
    val symptomPrevalence: Map<Symptom, Double>,
    val flowDistribution: Map<FlowIntensity, Double>,
    val goals: Set<Goal>,
    val birthControl: BirthControlMethod?,
    /** Non-null if BC was started or stopped mid-history. Counted as months ago from today. */
    val bcStartedMonthsAgo: Int? = null,
    /** Probability a daily bleeding entry is dropped to simulate skipped logging. */
    val skipLogProbability: Double = 0.0,
)
