package com.example.nssapp.feature.admin.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminEventViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventUiState>(EventUiState.Loading)
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getEvents(),
                repository.getWings()
            ) { events, wings ->
                EventUiState.Success(events, wings)
            }.catch { e ->
                _uiState.value = EventUiState.Error(e.message ?: "Unknown Error")
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun addEvent(title: String, type: String, mandatory: Boolean, targetWings: List<String>) {
        viewModelScope.launch {
            val event = Event(
                title = title,
                type = type,
                mandatory = mandatory,
                targetWings = targetWings,
                date = System.currentTimeMillis() // Defaulting to now for simplicity in MVP
            )
            repository.createEvent(event)
        }
    }
}

sealed class EventUiState {
    object Loading : EventUiState()
    data class Success(val events: List<Event>, val wings: List<Wing>) : EventUiState()
    data class Error(val message: String) : EventUiState()
}
