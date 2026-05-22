package com.hayate0726.tides.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.UserPrivacyRepository
import com.hayate0726.tides.data.entity.GoalEntity
import com.hayate0726.tides.domain.model.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoalsEditorViewModel(
    private val db: TidesDatabase,
    private val userPrivacyRepository: UserPrivacyRepository,
) : ViewModel() {

    data class UiState(
        val selected: Set<Goal> = emptySet(),
        val loaded: Boolean = false,
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val current = db.goalDao().all().toSet()
            _state.value = _state.value.copy(selected = current, loaded = true)
        }
    }

    fun toggle(goal: Goal) {
        val s = _state.value
        val next = if (goal in s.selected) s.selected - goal else s.selected + goal
        _state.value = s.copy(selected = next, saved = false)
    }

    fun save() {
        val toSave = _state.value.selected
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.withTransaction {
                    db.goalDao().clearAll()
                    for (g in toSave) db.goalDao().insert(GoalEntity(g))
                }
                userPrivacyRepository.refresh(db)
            }
            _state.value = _state.value.copy(saved = true)
        }
    }
}
