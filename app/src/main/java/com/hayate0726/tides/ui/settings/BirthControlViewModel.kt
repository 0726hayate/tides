package com.hayate0726.tides.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.UserPrivacyRepository
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.domain.model.BirthControlMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class BirthControlViewModel(
    private val db: TidesDatabase,
    private val userPrivacyRepository: UserPrivacyRepository,
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
            }
            _state.value = _state.value.copy(current = _state.value.selected, saved = true)
        }
    }
}
