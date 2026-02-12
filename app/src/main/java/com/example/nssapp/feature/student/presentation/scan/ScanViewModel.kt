package com.example.nssapp.feature.student.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onCodeScanned(code: String) {
        if (_uiState.value is ScanUiState.Loading || _uiState.value is ScanUiState.Success) {
            return
        }

        _uiState.value = ScanUiState.Loading

        viewModelScope.launch {
            val studentId = authRepository.currentUser?.uid
            if (studentId == null) {
                _uiState.value = ScanUiState.Error("User not logged in")
                return@launch
            }

            // Assume code is eventId
            val result = studentRepository.markAttendance(eventId = code, studentId = studentId)
            
            result.onSuccess {
                _uiState.value = ScanUiState.Success("Attendance Marked Successfully!")
            }.onFailure {
                _uiState.value = ScanUiState.Error(it.message ?: "Failed to mark attendance")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = ScanUiState.Idle
    }
}

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class Success(val message: String) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}
