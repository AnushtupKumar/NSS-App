package com.example.nssapp.feature.admin.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminProfileUiState>(AdminProfileUiState.Loading)
    val uiState: StateFlow<AdminProfileUiState> = _uiState.asStateFlow()

    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user == null) {
                _uiState.value = AdminProfileUiState.Error("User not logged in")
                return@launch
            }

            adminRepository.getEventsByCreator(user.uid)
                .catch { e ->
                    // Keep existing state if error is transient, or show error?
                    // Ideally combine with static profile data.
                    _uiState.value = AdminProfileUiState.Error(e.message ?: "Failed to load events")
                }
                .collect { events ->
                    _uiState.value = AdminProfileUiState.Success(
                        email = user.email ?: "Unknown",
                        events = events
                    )
                }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _passwordChangeState.value = PasswordChangeState.Loading
            val result = authRepository.updatePassword(newPassword)
            if (result.isSuccess) {
                _passwordChangeState.value = PasswordChangeState.Success
            } else {
                _passwordChangeState.value = PasswordChangeState.Error(result.exceptionOrNull()?.message ?: "Failed to update password")
            }
        }
    }

    fun clearPasswordChangeState() {
        _passwordChangeState.value = PasswordChangeState.Idle
    }
}

sealed class AdminProfileUiState {
    object Loading : AdminProfileUiState()
    data class Success(val email: String, val events: List<Event>) : AdminProfileUiState()
    data class Error(val message: String) : AdminProfileUiState()
}

sealed class PasswordChangeState {
    object Idle : PasswordChangeState()
    object Loading : PasswordChangeState()
    object Success : PasswordChangeState()
    data class Error(val message: String) : PasswordChangeState()
}
