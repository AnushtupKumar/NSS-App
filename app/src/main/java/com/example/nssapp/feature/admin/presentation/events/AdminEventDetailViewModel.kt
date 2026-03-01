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

    private val _attendees = MutableStateFlow<List<Attendee>>(emptyList())
    val attendees: StateFlow<List<Attendee>> = _attendees.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            combine(
                repository.getEvents(),
                repository.getAllWings()
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
                
                if (attendanceSnapshot.isEmpty) {
                    _attendees.value = emptyList()
                    return@launch
                }

                // Map studentId to status
                val attendanceData = attendanceSnapshot.documents.associateBy({ it.id }, { it.getString("status") ?: "PRESENT" })
                val studentIds = attendanceData.keys.toList()

                // 2. Fetch those students
                 val studentsQuery = firestore.collection("students").get().await()
                 val allStudents = studentsQuery.documents.map { doc ->
                     val student = doc.toObject(Student::class.java)!!
                     val primaryWing = doc.getString("primaryWing")
                     if (student.enrolledWings.isEmpty() && !primaryWing.isNullOrEmpty()) {
                         student.copy(id = doc.id, enrolledWings = listOf(primaryWing))
                     } else {
                         student.copy(id = doc.id)
                     }
                 }
                 
                 val wingsSnapshot = firestore.collection("wings").get().await()
                 val wingsMap = wingsSnapshot.documents.associateBy({ it.id }, { it.getString("name") ?: "Unknown" })

                 val attendedStudents = allStudents.filter { studentIds.contains(it.id) }.map { student ->
                     Attendee(
                         id = student.id,
                         roll = student.roll,
                         name = student.name,
                         wing = wingsMap[student.enrolledWings.firstOrNull()] ?: "Unknown Wing",
                         status = attendanceData[student.id] ?: "PRESENT"
                     )
                 }
                 _attendees.value = attendedStudents
            } catch (e: Exception) {
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
            repository.updateEvent(event)
            if (resetPenalty) {
                attendanceRepository.clearEventPenalties(event.id)
            }
        }
    }
}

sealed class EventDetailUiState {
    object Loading : EventDetailUiState()
    data class Success(val event: Event, val wings: List<Wing>) : EventDetailUiState()
    data class Error(val message: String) : EventDetailUiState()
}

data class Attendee(
    val id: String,
    val roll: String,
    val name: String,
    val wing: String,
    val status: String
)
