package com.example.nssapp.feature.student.presentation.face

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceRegistrationViewModel @Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    fun saveFaceEmbedding(studentId: String, embedding: List<Float>) {
        viewModelScope.launch {
            studentRepository.saveFaceEmbedding(studentId, embedding)
        }
    }
}
