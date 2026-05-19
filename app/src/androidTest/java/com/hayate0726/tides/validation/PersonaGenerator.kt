package com.hayate0726.tides.validation

import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

object PersonaGenerator {

    fun generate(
        spec: PersonaSpec,
        seed: Long,
        today: LocalDate = LocalDate.of(2026, 5, 1),
    ): Persona {
        val rng = Random(seed)
        val cycleEntries = mutableListOf<CycleEntryEntity>()
        val symptomEntries = mutableListOf<SymptomEntryEntity>()
        val bcRow = spec.birthControl?.let { method ->
            BirthControlEntity(
                method = method,
                startDate = today.minusMonths(
                    (spec.bcStartedMonthsAgo ?: spec.historyMonths).toLong()
                ),
                endDate = null,
            )
        }

        val historyStart = today.minusMonths(spec.historyMonths.toLong())
        var cycleStart = historyStart
        // Stop emitting once we'd land inside the trailing amenorrhea window.
        // This is how SPECIAL_ATHLETE deterministically triggers FIGO
        // AMENORRHEA (≥90d since last period start).
        val cutoff = today.minusDays(spec.trailingAmenorrheaPadDays.toLong())

        while (cycleStart.isBefore(cutoff)) {
            val cycleLen = sampleClamped(spec.cycleLengthDays, rng)
            val periodLen = sampleClamped(spec.periodLengthDays, rng).coerceAtMost(cycleLen - 1)
            val isAnovulatory = rng.nextDouble() < spec.anovulationRate

            for (d in 0 until periodLen) {
                if (rng.nextDouble() < spec.skipLogProbability) continue
                val date = cycleStart.plusDays(d.toLong())
                if (!date.isBefore(today)) break
                val flow = sampleFlow(spec.flowDistribution, rng)
                cycleEntries += CycleEntryEntity(
                    date = date,
                    flowIntensity = flow,
                    painSeverity = null,
                    notes = null,
                )
            }

            for (d in 0 until cycleLen) {
                if (d >= periodLen + 2 && rng.nextDouble() < 0.15) {
                    val date = cycleStart.plusDays(d.toLong())
                    if (!date.isBefore(today)) break
                    spec.symptomPrevalence.forEach { (symptom, prevalence) ->
                        if (rng.nextDouble() < prevalence) {
                            symptomEntries += SymptomEntryEntity(
                                date = date,
                                symptom = symptom,
                                severity = 1,
                                otherText = null,
                            )
                        }
                    }
                }
            }

            cycleStart = cycleStart.plusDays(cycleLen.toLong())
            // isAnovulatory is unused for now; reserved for future ovulation marker logging.
            @Suppress("UNUSED_VARIABLE")
            val noop = isAnovulatory
        }

        return Persona(
            id = spec.id,
            spec = spec,
            cycleEntries = cycleEntries.sortedBy { it.date },
            symptomEntries = symptomEntries.sortedBy { it.date },
            goals = spec.goals,
            birthControl = bcRow,
        )
    }

    private fun sampleClamped(p: DistParams, rng: Random): Int {
        val raw = when (p.shape) {
            DistShape.NORMAL -> p.mean + rng.gaussian() * p.sd
            DistShape.LOG_NORMAL -> {
                // Parameterize log-normal by *target* mean/sd of the underlying
                // distribution, converted to mu/sigma of the log.
                val mu = ln(p.mean * p.mean / sqrt(p.sd * p.sd + p.mean * p.mean))
                val sigma = sqrt(ln(1.0 + p.sd * p.sd / (p.mean * p.mean)))
                exp(mu + rng.gaussian() * sigma)
            }
        }
        var v = raw.toInt().coerceAtLeast(1)
        p.minClamp?.let { v = v.coerceAtLeast(it) }
        p.maxClamp?.let { v = v.coerceAtMost(it) }
        return v
    }

    private fun sampleFlow(dist: Map<FlowIntensity, Double>, rng: Random): FlowIntensity {
        if (dist.isEmpty()) return FlowIntensity.MEDIUM
        val total = dist.values.sum()
        var r = rng.nextDouble() * total
        for ((flow, weight) in dist) {
            r -= weight
            if (r <= 0) return flow
        }
        return dist.keys.last()
    }

    private fun Random.gaussian(): Double {
        // Box-Muller; pair the two samples implicitly by calling twice per pair
        // is fine since we re-enter — this is good enough for test data.
        val u1 = this.nextDouble().coerceAtLeast(1e-12)
        val u2 = this.nextDouble()
        return sqrt(-2.0 * ln(u1)) * Math.cos(2.0 * Math.PI * u2)
    }
}
