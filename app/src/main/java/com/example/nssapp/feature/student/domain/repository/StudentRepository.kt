package com.example.nssapp.feature.student.domain.repository

import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    suspend fun getStudentProfile(studentId: String): Result<Student>
    
    // Get all events appropriate for the student's wings
    fun getAllEvents(): Flow<List<Event>>
    
    // Get attendance records to calculate stats. 
    // Return list of Event IDs or Attendance objects.
    fun getAttendedEvents(studentId: String): Flow<List<String>>

    // Get map of eventId -> status string
    fun getAttendanceStatuses(studentId: String): Flow<Map<String, String>>
    
    suspend fun markAttendance(eventId: String, studentId: String): Result<Unit>
    
    suspend fun checkAttendanceStatus(eventId: String, studentId: String): Result<Boolean> 

    suspend fun getAttendanceStatus(eventId: String, studentId: String): String?
    fun getAttendanceForEvents(eventIds: List<String>, studentId: String): Flow<Map<String, String>>
    
    suspend fun getAllWingsList(): List<com.example.nssapp.core.domain.model.Wing>
    
    suspend fun saveFaceEmbedding(studentId: String, embedding: List<Float>): Result<Unit>
}
