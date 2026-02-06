package com.example.nssapp.feature.student.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentHomeUiState>(StudentHomeUiState.Loading)
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = StudentHomeUiState.Error("User not logged in")
                return@launch
            }

            val studentResult = studentRepository.getStudentProfile(currentUser.uid)
            if (studentResult.isSuccess) {
                val student = studentResult.getOrThrow()
                
                // Fetch stats
                val attendedEventsResult = studentRepository.getAttendedEvents(currentUser.uid)
                val attendedEventIds = attendedEventsResult.getOrNull() ?: emptyList()
                
                // Fetch events
                studentRepository.getEventsForWing(student.primaryWing).collect { events ->
                    val totalEvents = events.size
                    val attendancePercentage = if (totalEvents > 0) {
                        // This is approximate. Logic: Count events that have passed? Or all events?
                        // For stats, usually "Events held so far" vs "Attended".
                        // Assuming 'events' list contains all events (past and future).
                        // Refinement: Filter passed events for denominator.
                        
                        val passedEvents = events.filter { it.date < System.currentTimeMillis() }.size
                        if (passedEvents > 0) {
                            (attendedEventIds.size.toFloat() / passedEvents.toFloat()) * 100
                        } else 0f
                    } else 0f

                    _uiState.value = StudentHomeUiState.Success(
                        student = student,
                        events = events,
                        attendancePercentage = attendancePercentage,
                        attendedEventIds = attendedEventIds
                    )
                }
            } else {
                _uiState.value = StudentHomeUiState.Error("Failed to load profile")
            }
        }
    }
}

sealed class StudentHomeUiState {
    object Loading : StudentHomeUiState()
    data class Success(
        val student: Student,
        val events: List<Event>,
        val attendancePercentage: Float,
        val attendedEventIds: List<String>
    ) : StudentHomeUiState()
    data class Error(val message: String) : StudentHomeUiState()
}
