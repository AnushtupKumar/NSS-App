package com.example.nssapp.feature.admin.presentation.wings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.example.nssapp.core.domain.model.Admin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

sealed class WingListUiState {
    object Loading : WingListUiState()
    data class Success(val wings: List<Wing>, val admins: List<Admin>) : WingListUiState()
    data class Error(val message: String) : WingListUiState()
}

@HiltViewModel
class WingManagementViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WingListUiState>(WingListUiState.Loading)
    val uiState: StateFlow<WingListUiState> = _uiState

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadWings()
    }

    private fun loadWings() {
        viewModelScope.launch {
            _uiState.value = WingListUiState.Loading
            combine(
                repository.getWings(),
                repository.getAdmins()
            ) { wings, admins ->
                WingListUiState.Success(wings, admins)
            }.catch { e -> 
                _uiState.value = WingListUiState.Error(e.message ?: "Failed to load wings") 
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addWing(name: String, maxEnrollment: Int, adminIds: List<String>) {
        viewModelScope.launch {
            val wing = Wing(name = name, maxEnrollment = maxEnrollment, modifiedDate = System.currentTimeMillis())
            repository.createWing(wing, adminIds)
                .onSuccess { _events.emit("Wing created successfully") }
                .onFailure { _events.emit("Error creating wing: ${it.message}") }
        }
    }

    fun updateWing(wing: Wing, adminIds: List<String>) {
        viewModelScope.launch {
            repository.createWing(wing.copy(modifiedDate = System.currentTimeMillis()), adminIds)
                .onSuccess { _events.emit("Wing updated successfully") }
                .onFailure { _events.emit("Error updating wing: ${it.message}") }
        }
    }

    fun deleteWing(wingId: String) {
        viewModelScope.launch {
            repository.deleteWing(wingId)
                .onSuccess { _events.emit("Wing deleted successfully") }
                .onFailure { _events.emit("Error deleting wing: ${it.message}") }
        }
    }
}
