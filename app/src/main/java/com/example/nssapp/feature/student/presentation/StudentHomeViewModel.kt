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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class EventCategory {
    ALL, INCOMING, ATTENDED, ABSENT, PENALIZED
}

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentHomeUiState>(StudentHomeUiState.Loading)
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    private var allEvents = emptyList<Event>()
    private var attendedEventIds = emptyList<String>()
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
                        attendedEventIds = emptyList(),
                        totalHours = 0.0,
                        selectedCategory = EventCategory.ALL,
                        selectedMonth = calendar.get(Calendar.MONTH),
                        selectedYear = calendar.get(Calendar.YEAR)
                    )

                    // Start observing data changes reactively
                    combine(
                        studentRepository.getAllEvents(),
                        studentRepository.getAttendedEvents(currentUser.uid)
                    ) { events, attendanceIds ->
                        attendedEventIds = attendanceIds
                        allEvents = events.filter { event ->
                            val isTargeted = event.targetWings.isEmpty() || event.targetWings.any { it in student.enrolledWings }
                            val isMandatory = event.mandatoryWings.isEmpty() || event.mandatoryWings.any { it in student.enrolledWings }
                            val isAttended = attendanceIds.contains(event.id)
                            isTargeted || isMandatory || isAttended
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
        
        // Calculate total hours (all time)
        var totalHours = 0.0
        
        viewModelScope.launch {
            // First we need to get ALL attendance objects for this student to know if they were actually penalized
            try {
                // Since this requires a new query, we will use a simpler approach for now 
                // based on the existing `attendedEventIds`
                // Actually, let's keep the existing logic but refine it.
                // If a student is in `attendedEventIds`, they are PRESENT.
                // If `isPenaltyApplied` is true, and they are NOT in `attendedEventIds`, and they are NOT in `studentsExcluded`, they are PENALIZED.
                for (event in allEvents) {
                    if (attendedEventIds.contains(event.id)) {
                        totalHours += event.positiveHours
                    } else if (event.mandatory && event.isPenaltyApplied && event.date < now) {
                        // Check if they were excluded
                        if (!event.studentsExcluded.contains(student.roll)) {
                            // Check if they were targeted by mandatory wings
                            val isMandatoryForStudent = event.mandatoryWings.isEmpty() || event.mandatoryWings.any { it in student.enrolledWings }
                            if (isMandatoryForStudent) {
                                totalHours -= event.negativeHours
                            }
                        }
                    }
                }

                // Attendance percentage (based on passed mandatory/targeted events)
                val passedTargetedEvents = allEvents.filter { event ->
                    event.date < now && (
                        event.targetWings.isEmpty() || event.targetWings.any { it in student.enrolledWings } ||
                        event.mandatoryWings.isEmpty() || event.mandatoryWings.any { it in student.enrolledWings }
                    )
                }.size
                
                val attendancePercentage = if (passedTargetedEvents > 0) {
                    (attendedEventIds.size.toFloat() / passedTargetedEvents.toFloat()) * 100
                } else 0f

                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                _uiState.value = StudentHomeUiState.Success(
                    student = student,
                    allEvents = allEvents,
                    filteredEvents = emptyList(), // Will be updated below
                    attendancePercentage = attendancePercentage,
                    attendedEventIds = attendedEventIds,
                    totalHours = totalHours,
                    selectedCategory = EventCategory.ALL,
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
            // Fetch all wings to check active status
            val allWingsResult = studentRepository.getAllWingsList()
            val allWingsMap = allWingsResult.associateBy { it.id }

            val filtered = allEvents.filter { event ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = event.date
                
                val studentEnrolledInTarget = event.targetWings.isEmpty() || event.targetWings.any { it in student.enrolledWings }
                val studentEnrolledInMandatory = event.mandatoryWings.isEmpty() || event.mandatoryWings.any { it in student.enrolledWings }
                
                val hasActiveTargetWing = event.targetWings.isEmpty() || 
                    event.targetWings.any { it in student.enrolledWings && allWingsMap[it]?.isDeleted == false }
                
                when (state.selectedCategory) {
                    EventCategory.ALL -> true
                    EventCategory.INCOMING -> {
                        event.date >= now && studentEnrolledInTarget && hasActiveTargetWing
                    }
                    EventCategory.ATTENDED -> attendedEventIds.contains(event.id)
                    EventCategory.ABSENT -> {
                        val isPassed = event.date < now
                        val notAttended = !attendedEventIds.contains(event.id)
                        
                        val isMandatoryForStudent = event.mandatoryWings.isEmpty() || event.mandatoryWings.any { it in student.enrolledWings }
                        val isPenalized = isPassed && event.mandatory && isMandatoryForStudent && event.isPenaltyApplied && 
                                          !attendedEventIds.contains(event.id) && !event.studentsExcluded.contains(student.roll)
                        
                        isPassed && notAttended && studentEnrolledInTarget && !isPenalized
                    }
                    EventCategory.PENALIZED -> {
                        val isPassed = event.date < now
                        val isMandatoryForStudent = event.mandatoryWings.isEmpty() || event.mandatoryWings.any { it in student.enrolledWings }
                        val isPenalized = isPassed && event.mandatory && isMandatoryForStudent && event.isPenaltyApplied && 
                                          !attendedEventIds.contains(event.id) && !event.studentsExcluded.contains(student.roll)
                        isPenalized
                    }
                }
            }.sortedByDescending { it.date }

            _uiState.value = state.copy(filteredEvents = filtered)
        }
    }
}

sealed class StudentHomeUiState {
    object Loading : StudentHomeUiState()
    data class Success(
        val student: Student,
        val allEvents: List<Event>,
        val filteredEvents: List<Event>,
        val attendancePercentage: Float,
        val attendedEventIds: List<String>,
        val totalHours: Double,
        val selectedCategory: EventCategory = EventCategory.ALL,
        val selectedMonth: Int,
        val selectedYear: Int
    ) : StudentHomeUiState()
    data class Error(val message: String) : StudentHomeUiState()
}
