package com.example.nssapp.feature.admin.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.example.nssapp.feature.admin.domain.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _activeEvents = MutableStateFlow<List<Event>>(emptyList())
    val activeEvents: StateFlow<List<Event>> = _activeEvents.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            // For MVP, just get all events. In real world, filter "Today's events" locally.
            // A better query would be "Where date > yesterday and date < tomorrow"
            adminRepository.getEvents().collect { events ->
                // Filter events that are roughly "today" or allow all for demo
                // Let's filter events date >= Today - 1 day
                val oneDayAgo = System.currentTimeMillis() - 86400000
                _activeEvents.value = events.filter { it.date > oneDayAgo }
            }
        }
    }

    fun markAttendance(eventId: String, rollNo: String) {
        if (rollNo.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Loading
            val result = attendanceRepository.markAttendance(eventId, rollNo)
            if (result.isSuccess) {
                _uiState.value = AttendanceUiState.Success("Marked Present: $rollNo")
            } else {
                _uiState.value = AttendanceUiState.Error(result.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AttendanceUiState.Idle
    }
}

sealed class AttendanceUiState {
    object Idle : AttendanceUiState()
    object Loading : AttendanceUiState()
    data class Success(val message: String) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
}
