package com.example.nssapp.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow()

    init {
        if (repository.currentUser != null) {
            _authState.value = AuthState.Loading
            checkUserRole()
        }
    }

    fun login(emailOrRoll: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(emailOrRoll, password)
            if (result.isSuccess) {
                checkUserRole()
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login Failed")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetState.Loading
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _passwordResetState.value = PasswordResetState.Success
            } else {
                _passwordResetState.value = PasswordResetState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }
    
    fun clearPasswordResetState() {
        _passwordResetState.value = PasswordResetState.Idle
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            // If already loading from login, keep loading. Else set loading?
            // Usually login -> loading -> checking role -> success
            
            val roleResult = repository.getUserRole()
            if (roleResult.isSuccess) {
                val role = roleResult.getOrNull()
                if (role == "admin") {
                    _authState.value = AuthState.SuccessAdmin
                } else if (role == "student") {
                    _authState.value = AuthState.SuccessStudent
                } else {
                     _authState.value = AuthState.Error("Unknown Role")
                }
            } else {
                _authState.value = AuthState.Error(roleResult.exceptionOrNull()?.message ?: "Role Check Failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object SuccessAdmin : AuthState()
    object SuccessStudent : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}
