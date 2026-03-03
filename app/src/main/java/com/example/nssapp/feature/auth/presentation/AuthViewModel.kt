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

    fun login(email: String, roll: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, roll)
            if (result.isSuccess) {
                checkUserRole()
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login Failed")
            }
        }
    }

    fun signup(name: String, email: String, pass: String, roll: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signup(name, email, pass, roll)
            if (result.isSuccess) {
                 _authState.value = AuthState.RequiresVerification // Instruct UI to show verification message
            } else {
                 _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Signup Failed")
            }
        }
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
    object RequiresVerification : AuthState()
    data class Error(val message: String) : AuthState()
}
