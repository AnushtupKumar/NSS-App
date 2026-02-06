package com.example.nssapp.feature.admin.presentation.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _selectedWing = MutableStateFlow<String?>(null)
    val selectedWing: StateFlow<String?> = _selectedWing.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getWings(),
                repository.getStudents(null), // Initially get all, filtering locally or re-fetching could be better but let's filter locally for small datasets
                _selectedWing
            ) { wings, students, selectedWingId ->
                val filteredStudents = if (selectedWingId == null) {
                    students
                } else {
                    students.filter { it.primaryWing == selectedWingId }
                }
                StudentUiState.Success(wings, filteredStudents)
            }.catch { e ->
                _uiState.value = StudentUiState.Error(e.message ?: "Unknown Error")
            }.collect { state ->
                val successState = state as StudentUiState.Success
                _uiState.value = successState
            }
        }
    }
    
    fun selectWing(wingId: String?) {
        _selectedWing.value = wingId
    }

    fun addStudent(name: String, email: String, roll: String, wingId: String) {
        viewModelScope.launch {
            val student = Student(name = name, email = email, roll = roll, primaryWing = wingId)
            repository.createStudent(student)
        }
    }
    
    fun addWing(name: String) {
        viewModelScope.launch {
            val wing = Wing(name = name)
            repository.createWing(wing)
        }
    }
}

sealed class StudentUiState {
    object Loading : StudentUiState()
    data class Success(val wings: List<Wing>, val students: List<Student>) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}
