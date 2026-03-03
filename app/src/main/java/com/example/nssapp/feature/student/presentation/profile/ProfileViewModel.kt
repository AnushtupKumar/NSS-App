package com.example.nssapp.feature.student.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository // To fetch Wing name
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = ProfileUiState.Error("User not logged in")
                return@launch
            }

            val studentResult = studentRepository.getStudentProfile(currentUser.uid)
            if (studentResult.isSuccess) {
                val student = studentResult.getOrThrow()
                
                // Fetch Wing Name
                // In a real app, we might have wings cached or query single wing.
                // Assuming AdminRepository has a way to get all wings or we can add getWing(id)
                // For MVP, we reused getWings() flow or add a specific function.
                // Let's just create a quick helper or fetch all (simple for small N).
                
                // Improve AdminRepository? Or just query 'wings' collection directly here? NOT GOOD.
                // Let's use AdminRepository.getWings() and find.
                // Or better, let's query firestore in repository layer.
                // But AdminRepository provides Flow<List<Wing>>. 
                
                val wings = adminRepository.getWings().firstOrNull() ?: emptyList()
                val wingNames = wings.filter { student.enrolledWings.contains(it.id) }.joinToString(", ") { it.name }
                val displayWing = wingNames.ifEmpty { "No Wings Enrolled" }

                // Fetch Stats again? Or pass them? Ideally fetch ensuring freshness.
                val attendedEventsResult = studentRepository.getAttendedEvents(currentUser.uid)
                val attendedCount = attendedEventsResult.singleOrNull()?.size ?: 0
                
                _uiState.value = ProfileUiState.Success(
                    student = student,
                    wingName = displayWing,
                    attendedCount = attendedCount
                )
            } else {
                _uiState.value = ProfileUiState.Error("Failed to load profile")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Navigation handled by UI observing state or callback?
            // Actually, AuthState in MainViewModel/RootNavigation observes this? 
            // We need to trigger a state change that RootNavigation sees.
            // RootNavigation observes AuthViewModel.authState.
            // If we logout here, AuthViewModel's state needs to update.
            // AuthViewModel should expose a flow based on FirebaseAuth state listener ideally.
            // But currently AuthViewModel has local flow.
            // We should probably call AuthViewModel.logout() or just authRepository.logout() 
            // AND ensure AuthViewModel updates.
            // Re-architect: AuthViewModel should listen to FirebaseAuth.AuthStateListener.
            // OR: We navigate to Login screen manually.
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val student: Student, val wingName: String, val attendedCount: Int) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
