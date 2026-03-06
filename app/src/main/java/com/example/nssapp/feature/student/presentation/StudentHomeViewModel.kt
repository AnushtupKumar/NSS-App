package com.example.nssapp.feature.student.presentation

import android.R.attr.category
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.AttendanceStatus
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.EventStatus
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class EventCategory {
    ALL, INCOMING, ATTENDED, ABSENT
}

data class EventWithWings(
    val event: Event,
    val matchingWings: List<String> = emptyList()
)

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentHomeUiState>(StudentHomeUiState.Loading)
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    private var allEvents = emptyList<Event>()
    private var attendanceStatuses = emptyMap<String, String>() // eventId -> status
    private var currentStudent: Student? = null

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

            try {
                val studentResult = studentRepository.getStudentProfile(currentUser.uid)
                if (studentResult.isSuccess) {
                    val student = studentResult.getOrThrow()
                    currentStudent = student
                    
                    // Initialize Success state immediately with empty data to remove loading spinner
                    val calendar = Calendar.getInstance()
                    _uiState.value = StudentHomeUiState.Success(
                        student = student,
                        allEvents = emptyList(),
                        filteredEvents = emptyList(),
                        attendancePercentage = 0f,
                        attendanceStatuses = emptyMap(),
                        totalHours = 0.0,
                        selectedCategory = EventCategory.ALL,
                        selectedMonth = calendar.get(Calendar.MONTH),
                        selectedYear = calendar.get(Calendar.YEAR)
                    )

                    // Start observing data changes reactively
                    combine(
                        studentRepository.getAllEvents(),
                        studentRepository.getAttendanceStatuses(currentUser.uid)
                    ) { events, statuses ->
                        attendanceStatuses = statuses
                        allEvents = events.filter { event ->
                            val isTargeted = event.targetWings.any { it in student.enrolledWings } || 
                                           event.mandatoryWings.any { it in student.enrolledWings }
                            val hasRecord = statuses.containsKey(event.id)
                            isTargeted || hasRecord
                        }
                        updateFilteredState()
                    }.collect()
                } else {
                    _uiState.value = StudentHomeUiState.Error("Failed to load profile")
                }
            } catch (e: Exception) {
                _uiState.value = StudentHomeUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun setCategory(category: EventCategory) {
        val currentState = _uiState.value
        if (currentState is StudentHomeUiState.Success) {
            _uiState.value = currentState.copy(selectedCategory = category)
            updateFilteredEvents()
        }
    }

    fun setMonth(month: Int) { // 0-indexed like Calendar
        val currentState = _uiState.value
        if (currentState is StudentHomeUiState.Success) {
            _uiState.value = currentState.copy(selectedMonth = month)
            updateFilteredEvents()
        }
    }

    private fun updateFilteredState() {
        val student = currentStudent ?: return
        val now = System.currentTimeMillis()
        
        viewModelScope.launch {
            try {
                // Attendance percentage (based on passed events the student was targeted for)
                val passedTargetedEvents = allEvents.filter { event ->
                    val isPast = event.endTime < now || event.status == EventStatus.IDLE.value
                    val isTargeted = event.targetWings.any { it in student.enrolledWings } || 
                                    event.mandatoryWings.any { it in student.enrolledWings }
                    isPast && isTargeted
                }.size
                
                val presentCount = attendanceStatuses.values.count { it == AttendanceStatus.PRESENT.value }
                val attendancePercentage = if (passedTargetedEvents > 0) {
                    (presentCount.toFloat() / passedTargetedEvents.toFloat()) * 100
                } else 0f

                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                val currentCategory = (_uiState.value as? StudentHomeUiState.Success)?.selectedCategory ?: EventCategory.ALL

                _uiState.value = StudentHomeUiState.Success(
                    student = student,
                    allEvents = allEvents,
                    filteredEvents = emptyList(), // Will be updated below
                    attendancePercentage = attendancePercentage,
                    attendanceStatuses = attendanceStatuses,
                    totalHours = student.totalHours,
                    selectedCategory = currentCategory,
                    selectedMonth = currentMonth,
                    selectedYear = currentYear
                )
                updateFilteredEvents()
            } catch (e: Exception) {
                // Handle
            }
        }
    }

    private fun updateFilteredEvents() {
        val state = _uiState.value as? StudentHomeUiState.Success ?: return
        val now = System.currentTimeMillis()
        val student = state.student

        viewModelScope.launch {
            // Fetch all wings to resolve names
            val allWingsResult = studentRepository.getAllWingsList()
            val allWingsMap = allWingsResult.associateBy { it.id }

            val filtered = allEvents.filter { event ->
                val status = attendanceStatuses[event.id]
                
                when (state.selectedCategory) {
                    EventCategory.ALL -> true
                    EventCategory.INCOMING -> {
                        val isTargeted = event.targetWings.any { it in student.enrolledWings } || 
                                        event.mandatoryWings.any { it in student.enrolledWings }
                        val notReachedStartTime = event.startTime > now
                        val noAttendanceRecord = status == null
                        
                        // Targeted AND (time not reached OR no record)
                        isTargeted && (notReachedStartTime || noAttendanceRecord)
                    }
                    EventCategory.ATTENDED -> status == AttendanceStatus.PRESENT.value
                    EventCategory.ABSENT -> status == AttendanceStatus.ABSENT.value || status == AttendanceStatus.PENALTY.value
                }
            }.map { event ->
                // Identify which of the student's wings match this event
                val matchingWingIds = student.enrolledWings.filter { it in event.targetWings || it in event.mandatoryWings }
                val matchingWingNames = matchingWingIds.mapNotNull { allWingsMap[it]?.name }
                
                EventWithWings(
                    event = event,
                    matchingWings = matchingWingNames
                )
            }.sortedByDescending { it.event.date }

            _uiState.value = state.copy(filteredEvents = filtered)
        }
    }
}

sealed class StudentHomeUiState {
    object Loading : StudentHomeUiState()
    data class Success(
        val student: Student,
        val allEvents: List<Event>,
        val filteredEvents: List<EventWithWings>,
        val attendancePercentage: Float,
        val attendanceStatuses: Map<String, String>,
        val totalHours: Double,
        val selectedCategory: EventCategory = EventCategory.ALL,
        val selectedMonth: Int,
        val selectedYear: Int
    ) : StudentHomeUiState()
    data class Error(val message: String) : StudentHomeUiState()
}
