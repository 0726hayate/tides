package com.hayate0726.tides.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.UserPrivacyRepository
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class BirthControlViewModel(
    private val db: TidesDatabase,
    private val userPrivacyRepository: UserPrivacyRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    data class UiState(
        val current: BirthControlMethod? = null,
        val selected: BirthControlMethod = BirthControlMethod.NONE,
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val active = db.birthControlDao().activeOnce()?.method ?: BirthControlMethod.NONE
            _state.value = _state.value.copy(current = active, selected = active)
        }
    }

    fun select(m: BirthControlMethod) {
        _state.value = _state.value.copy(selected = m, saved = false)
    }

    fun save() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                db.withTransaction {
                    val active = db.birthControlDao().activeOnce()
                    if (active != null) {
                        db.birthControlDao().update(active.copy(endDate = today.minusDays(1)))
                    }
                    db.birthControlDao().insert(
                        BirthControlEntity(
                            id = 0,
                            method = _state.value.selected,
                            startDate = today,
                            endDate = null,
                        )
                    )
                }
                userPrivacyRepository.refresh(db)
                // Publish a fresh widget snapshot so the Normal widget reflects the
                // hormonal/non-hormonal change without waiting for the next Calendar refresh.
                val from = YearMonth.now().atDay(1).minusMonths(1)
                val to = YearMonth.now().atEndOfMonth().plusMonths(1)
                val entries = db.cycleEntryDao().rangeOnce(from, to)
                val cycles = CycleDetector.detect(
                    entries.map { CycleDetector.Entry(it.date, it.flowIntensity) },
                    activeBirthControl = _state.value.selected,
                )
                widgetUpdater.publish(
                    cycles = cycles,
                    showOvulation = userPrivacyRepository.view.value.showOvulation,
                    today = today,
                )
            }
            _state.value = _state.value.copy(current = _state.value.selected, saved = true)
        }
    }
}
