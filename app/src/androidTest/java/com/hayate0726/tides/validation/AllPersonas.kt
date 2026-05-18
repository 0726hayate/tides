package com.hayate0726.tides.validation

import java.time.LocalDate

/**
 * Union of synthetic and case-report personas. Used by [PersonaScenarioTest]
 * via JUnit's @Parameters.
 *
 * Synthetic personas are deterministic — each id maps to a fixed seed,
 * so identical builds produce identical histories. Case-report personas
 * are static instances.
 */
object AllPersonas {

    private val today: LocalDate = LocalDate.of(2026, 5, 1)

    val all: List<Persona> by lazy {
        SyntheticPersonas.all.map { spec ->
            PersonaGenerator.generate(spec, seed = seedFor(spec.id), today = today)
        } + CaseReportPersonas.all
    }

    private fun seedFor(id: String): Long = id.hashCode().toLong()
}
