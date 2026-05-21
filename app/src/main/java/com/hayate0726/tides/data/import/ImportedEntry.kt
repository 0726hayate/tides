package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

data class ImportedEntry(
    val date: LocalDate,
    val flow: FlowIntensity?,
    val painSeverity: Int?,
    val symptoms: List<Symptom>,
    val notes: String?,
)
