package com.example.nssapp.feature.admin.domain.repository

import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    // Wings
    fun getWings(): Flow<List<Wing>>
    suspend fun createWing(wing: Wing): Result<Unit>
    
    // Students
    fun getStudents(wingId: String? = null): Flow<List<Student>>
    suspend fun createStudent(student: Student): Result<Unit>
    
    // Events
    fun getEvents(): Flow<List<Event>>
    suspend fun createEvent(event: Event): Result<Unit>
}
