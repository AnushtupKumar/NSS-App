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

    private var currentEventId: String? = null
    private var studentFaceEmbedding: FloatArray? = null
    private var isFaceVerified = false

    fun onCodeScanned(code: String) {
        if (_uiState.value is ScanUiState.Loading || _uiState.value is ScanUiState.Success || _uiState.value is ScanUiState.RequiresFaceScan) {
            return
        }

        _uiState.value = ScanUiState.Loading

        viewModelScope.launch {
            val studentId = authRepository.currentUser?.uid
            if (studentId == null) {
                _uiState.value = ScanUiState.Error("User not logged in")
                return@launch
            }

            // Fetch student profile to get their registered face embedding
            val studentResult = studentRepository.getStudentProfile(studentId)
            if (studentResult.isSuccess) {
                val student = studentResult.getOrThrow()
                studentFaceEmbedding = student.faceEmbedding?.toFloatArray()
                
                if (studentFaceEmbedding == null) {
                    _uiState.value = ScanUiState.Error("No Face Registered. Please relogin to register your face.")
                    return@launch
                }

                currentEventId = code
                _uiState.value = ScanUiState.RequiresFaceScan(studentFaceEmbedding!!)
            } else {
                _uiState.value = ScanUiState.Error("Failed to load student profile for verification.")
            }
        }
    }

    private var failedFaceFrames = 0

    fun onFaceVerificationResult(isMatch: Boolean) {
        if (isFaceVerified || _uiState.value !is ScanUiState.RequiresFaceScan) return // Prevent multiple calls
        
        if (isMatch) {
            isFaceVerified = true
            markAttendance()
        } else {
            failedFaceFrames++
            if (failedFaceFrames > 40) { // ~3-4 seconds of continuous failed matches
                _uiState.value = ScanUiState.Error("Face Verification Failed! Identity mismatch or could not align face.")
                failedFaceFrames = 0
            }
        }
    }

    private fun markAttendance() {
        val eventId = currentEventId ?: return
        val studentId = authRepository.currentUser?.uid ?: return
        
        _uiState.value = ScanUiState.Loading
        
        viewModelScope.launch {
            val result = studentRepository.markAttendance(eventId = eventId, studentId = studentId)
            
            result.onSuccess {
                _uiState.value = ScanUiState.Success("Attendance marked successfully!")
            }.onFailure {
                _uiState.value = ScanUiState.Error(it.message ?: "Failed to mark attendance")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = ScanUiState.Idle
        currentEventId = null
        isFaceVerified = false
        failedFaceFrames = 0
    }
}

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class RequiresFaceScan(val targetEmbedding: FloatArray) : ScanUiState()
    data class Success(val message: String) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}
