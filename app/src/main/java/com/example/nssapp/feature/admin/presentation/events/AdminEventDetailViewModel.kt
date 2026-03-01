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
import com.example.nssapp.core.domain.model.Student
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminEventDetailViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val attendanceRepository: AttendanceRepository,
    private val firestore: FirebaseFirestore // Injecting directly for quick query, ideally should be in repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private val _attendees = MutableStateFlow<List<Student>>(emptyList())
    val attendees: StateFlow<List<Student>> = _attendees.asStateFlow()

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

    fun loadAttendees(eventId: String) {
        viewModelScope.launch {
            try {
                // 1. Get all attendance documents for this event
                val attendanceSnapshot = firestore.collection("events").document(eventId).collection("attendance").get().await()
                val studentIds = attendanceSnapshot.documents.map { it.id }

                if (studentIds.isEmpty()) {
                    _attendees.value = emptyList()
                    return@launch
                }

                // 2. Fetch those students (batching in blocks of 10 if needed, but for simplicity we'll just query or fetch all active students and filter)
                 val studentsQuery = firestore.collection("students").get().await()
                 val allStudents = studentsQuery.toObjects(Student::class.java).mapIndexed { index, student -> student.copy(id = studentsQuery.documents[index].id) }
                 
                 val attendedStudents = allStudents.filter { studentIds.contains(it.id) }
                 _attendees.value = attendedStudents
            } catch (e: Exception) {
                 // Handle Error implicitly by empty list or log
                 _attendees.value = emptyList()
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
            }
        }
    }

    fun updateStatus(eventId: String, status: String) {
        viewModelScope.launch {
            repository.updateEventStatus(eventId, status)
        }
    }

    fun exportAttendance(eventId: String) {
        // Placeholder for CSV export logic
    }

    fun markBulkAttendance(eventId: String, rollString: String, status: String, bypass: Boolean, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val rolls = rollString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            if (rolls.isEmpty()) {
                onResult(emptyList()) 
                return@launch
            }

            val result = attendanceRepository.markAttendanceBulk(eventId, rolls, status, bypass)
            if (result.isSuccess) {
                onResult(result.getOrDefault(emptyList()))
            } else {
                 onResult(rolls) 
            }
        }
    }
    
    fun applyPenalty(eventId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val result = attendanceRepository.applyPenaltyForEvent(eventId)
            if (result.isSuccess) {
                loadEvent(eventId)
                onResult(result.getOrDefault(0))
            } else {
                onResult(-1) // Signal failure
            }
        }
    }

    fun updateEvent(event: Event, resetPenalty: Boolean = false) {
        viewModelScope.launch {
            if (resetPenalty) {
                attendanceRepository.clearEventPenalties(event.id)
            }
            repository.updateEvent(event)
        }
    }
}

sealed class EventDetailUiState {
    object Loading : EventDetailUiState()
    data class Success(val event: Event, val wings: List<Wing>) : EventDetailUiState()
    data class Error(val message: String) : EventDetailUiState()
}
