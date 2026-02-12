package com.example.nssapp.feature.admin.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.example.nssapp.feature.admin.domain.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminEventDetailViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            combine(
                repository.getEvents(),
                repository.getWings()
            ) { events, wings ->
                val event = events.find { it.id == eventId }
                if (event != null) {
                    EventDetailUiState.Success(event, wings)
                } else {
                    EventDetailUiState.Error("Event not found")
                }
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun deleteEvent(eventId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteEvent(eventId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                 // Handle error? Show toast?
                 // For now just stay.
            }
        }
    }

    fun updateStatus(eventId: String, status: String) {
        viewModelScope.launch {
            repository.updateEventStatus(eventId, status)
            // Flow will auto-update UI
        }
    }

    fun exportAttendance(eventId: String) {
        // Placeholder for CSV export logic
        // In a real app, query attendance subcollection, build CSV string, use Intent to share.
    }

    fun markBulkAttendance(eventId: String, rollString: String, status: String, bypass: Boolean, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val rolls = rollString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            if (rolls.isEmpty()) {
                onResult(emptyList()) // Nothing to do
                return@launch
            }

            val result = attendanceRepository.markAttendanceBulk(eventId, rolls, status, bypass)
            if (result.isSuccess) {
                onResult(result.getOrDefault(emptyList()))
            } else {
                 // Handle failure?
                 onResult(rolls) // All failed?
            }
        }
    }
    fun applyPenalty(eventId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val result = attendanceRepository.applyPenaltyForEvent(eventId)
            if (result.isSuccess) {
                // Refresh event to see status change
                loadEvent(eventId)
                onResult(result.getOrDefault(0))
            } else {
                onResult(-1) // Signal failure
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            repository.updateEvent(event)
             // Flow will auto-update UI
        }
    }
}

sealed class EventDetailUiState {
    object Loading : EventDetailUiState()
    data class Success(val event: Event, val wings: List<Wing>) : EventDetailUiState()
    data class Error(val message: String) : EventDetailUiState()
}
