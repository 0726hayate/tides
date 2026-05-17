package com.hayate0726.tides.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Backs [LogBottomSheet]. Loads any existing entry for a given date and
 * persists edits back to the DAOs. Symptom rows are rewritten as a set
 * (delete-by-date then insert), mirroring CycleEntry's per-date semantics.
 *
 * Note: not a `@HiltViewModel` — the database is opened post-onboarding
 * with a runtime SQLCipher key, so the DB is injected manually like
 * [com.hayate0726.tides.ui.calendar.CalendarViewModel].
 */
class LogViewModel(
    private val db: TidesDatabase,
) : ViewModel() {

    private val cycleEntryDao = db.cycleEntryDao()
    private val symptomEntryDao = db.symptomEntryDao()

    data class UiState(
        val date: LocalDate? = null,
        val flow: FlowIntensity? = null,
        val symptoms: Map<Symptom, Int> = emptyMap(),
        val painSeverity: Int? = null,
        val note: String = "",
        val otherText: String = "",
        val loaded: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(date: LocalDate) {
        viewModelScope.launch {
            val entry = cycleEntryDao.getByDate(date)
            val symptomRows = symptomEntryDao.getByDate(date)
            val otherText = symptomRows.firstOrNull { it.symptom == Symptom.OTHER }
                ?.otherText.orEmpty()
            _state.value = UiState(
                date = date,
                flow = entry?.flowIntensity,
                symptoms = symptomRows.associate { it.symptom to it.severity },
                painSeverity = entry?.painSeverity,
                note = entry?.notes.orEmpty(),
                otherText = otherText,
                loaded = true,
            )
        }
    }

    fun save(
        date: LocalDate,
        flow: FlowIntensity?,
        symptoms: Map<Symptom, Int>,
        painSeverity: Int?,
        note: String,
        otherText: String,
    ) {
        viewModelScope.launch {
            if (flow == null && symptoms.isEmpty() && painSeverity == null && note.isBlank()) {
                cycleEntryDao.deleteByDate(date)
                symptomEntryDao.deleteByDate(date)
                return@launch
            }
            cycleEntryDao.upsert(
                CycleEntryEntity(
                    date = date,
                    flowIntensity = flow ?: FlowIntensity.NONE,
                    painSeverity = painSeverity,
                    notes = note.takeIf { it.isNotBlank() },
                )
            )
            symptomEntryDao.deleteByDate(date)
            symptoms.forEach { (sym, sev) ->
                symptomEntryDao.insert(
                    SymptomEntryEntity(
                        date = date,
                        symptom = sym,
                        severity = sev,
                        otherText = if (sym == Symptom.OTHER) otherText.takeIf { it.isNotBlank() } else null,
                    )
                )
            }
        }
    }
}
