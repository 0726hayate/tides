package com.hayate0726.tides.ui.export

import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

object CsvBuilder {

    fun build(
        cycles: List<Cycle>,
        symptomsByDate: Map<LocalDate, List<Symptom>>,
        notesByDate: Map<LocalDate, String>,
    ): String {
        val sb = StringBuilder()
        sb.append("cycle_start,cycle_length,period_length,period_end,symptoms,note\n")
        for (c in cycles) {
            sb.append(c.start)
            sb.append(',')
            sb.append(c.length ?: "")
            sb.append(',')
            sb.append(c.periodLength ?: "")
            sb.append(',')
            sb.append(c.periodEnd ?: "")
            sb.append(',')
            val syms = symptomsByDate[c.start]?.joinToString(";") { it.name } ?: ""
            sb.append(escape(syms))
            sb.append(',')
            sb.append(escape(notesByDate[c.start] ?: ""))
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun escape(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n'))
            "\"" + s.replace("\"", "\"\"") + "\""
        else s
}
