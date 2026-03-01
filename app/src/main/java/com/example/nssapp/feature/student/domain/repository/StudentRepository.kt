package com.example.nssapp.feature.student.domain.repository

import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    suspend fun getStudentProfile(studentId: String): Result<Student>
    
    // Get all events appropriate for the student's wings
    fun getEventsForWings(wingIds: List<String>): Flow<List<Event>>
    
    // Get attendance records to calculate stats. 
    // Return list of Event IDs or Attendance objects.
    // Simpler for MVP: Get list of events where student is marked present.
    suspend fun getAttendedEvents(studentId: String): Result<List<String>> 
    
    suspend fun markAttendance(eventId: String, studentId: String): Result<Unit>
    
    suspend fun checkAttendanceStatus(eventId: String, studentId: String): Result<Boolean> 
}
