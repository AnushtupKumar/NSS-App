package com.example.nssapp.feature.admin.domain.repository

import com.example.nssapp.core.domain.model.Admin

import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    // Admins
    fun getAdmins(): Flow<List<Admin>>

    // Wings
    fun getWings(): Flow<List<Wing>> // Only active
    fun getAllWings(): Flow<List<Wing>> // Including deleted
    fun getWingById(wingId: String): Flow<Wing?>
    suspend fun createWing(wing: Wing, adminIds: List<String> = emptyList()): Result<Unit>
    suspend fun updateWing(wing: Wing): Result<Unit>
    suspend fun deleteWing(wingId: String): Result<Unit>
    
    // Students
    fun getStudents(wingId: String? = null): Flow<List<Student>>
    suspend fun createStudent(student: Student): Result<Unit>
    suspend fun updateStudent(student: Student): Result<Unit>
    suspend fun deleteStudent(studentId: String): Result<Unit>
    
    // Events
    fun getEvents(): Flow<List<Event>>
    suspend fun createEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
    suspend fun updateEventStatus(eventId: String, status: String): Result<Unit>
    fun getEventsByCreator(creatorId: String): Flow<List<Event>>
    suspend fun updateEvent(event: Event): Result<Unit>
}
