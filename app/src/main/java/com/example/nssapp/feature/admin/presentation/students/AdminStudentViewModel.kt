package com.example.nssapp.feature.admin.presentation.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Admin
import com.example.nssapp.core.domain.model.Student
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
class AdminStudentViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentUiState>(StudentUiState.Loading)
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    private val _selectedWings = MutableStateFlow<Set<String>>(emptySet())
    val selectedWings: StateFlow<Set<String>> = _selectedWings.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getWings(),
                repository.getStudents(null), // Initially get all, filtering locally or re-fetching could be better but let's filter locally for small datasets
                repository.getAdmins(),
                _selectedWings
            ) { wings, students, admins, selectedWingIds ->
                val filteredStudents = if (selectedWingIds.isEmpty()) {
                    students
                } else {
                    students.filter { student -> 
                        // Show student if they are in ANY of the selected wings (OR logic) or ALL? Usually OR for filtering lists.
                        // "filter by selecting multiple wings" -> usually means "show students who belong to Wing A OR Wing B"
                        student.enrolledWings.any { it in selectedWingIds }
                    }
                }
                StudentUiState.Success(wings, filteredStudents, admins)
            }.catch { e ->
                _uiState.value = StudentUiState.Error(e.message ?: "Unknown Error")
            }.collect { state ->
                val successState = state as StudentUiState.Success
                _uiState.value = successState
            }
        }
    }
    
    fun toggleWingFilter(wingId: String) {
        val current = _selectedWings.value.toMutableSet()
        if (current.contains(wingId)) {
            current.remove(wingId)
        } else {
            current.add(wingId)
        }
        _selectedWings.value = current
    }

    fun clearFilters() {
        _selectedWings.value = emptySet()
    }

    fun addStudent(name: String, email: String, roll: String, wingIds: List<String>, password: String) {
        viewModelScope.launch {
            val student = Student(name = name, email = email, roll = roll, enrolledWings = wingIds, password = password)
            repository.createStudent(student)
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
        }
    }
    
    fun addWing(name: String, adminIds: List<String>) {
        viewModelScope.launch {
            val wing = Wing(name = name)
            repository.createWing(wing, adminIds)
        }
    }
}

sealed class StudentUiState {
    object Loading : StudentUiState()
    data class Success(val wings: List<Wing>, val students: List<Student>, val admins: List<Admin> = emptyList()) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}
