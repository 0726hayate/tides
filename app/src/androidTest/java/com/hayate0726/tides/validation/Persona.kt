package com.hayate0726.tides.validation

import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.Goal

/**
 * A fully realized user history: cycle entries, symptom entries, goals, and
 * (optionally) an active birth-control row. Inserted directly into a test DB.
 *
 * Carries the source [PersonaSpec] so assertions can reason about the
 * intended population segment.
 */
data class Persona(
    val id: String,
    val spec: PersonaSpec?,
    val cycleEntries: List<CycleEntryEntity>,
    val symptomEntries: List<SymptomEntryEntity>,
    val goals: Set<Goal>,
    val birthControl: BirthControlEntity?,
) {
    override fun toString(): String = id
}
