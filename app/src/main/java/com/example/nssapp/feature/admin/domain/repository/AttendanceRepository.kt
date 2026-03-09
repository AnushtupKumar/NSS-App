package com.example.nssapp.feature.admin.domain.repository

interface AttendanceRepository {
    // Returns success if attendance marked, failure if student not found or error
    suspend fun markAttendance(
        eventId: String, 
        studentRoll: String,
        status: String = "PRESENT",
        bypassRestrictions: Boolean = false
    ): Result<Unit>

    suspend fun markAttendanceBulk(
        eventId: String,
        rollNumbers: List<String>,
        status: String = "PRESENT",
        bypassRestrictions: Boolean = false
    ): Result<List<String>>

    suspend fun applyPenaltyForEvent(eventId: String): Result<Int>
    suspend fun clearEventPenalties(eventId: String): Result<Unit>
    
    suspend fun deleteAllEventAttendance(eventId: String): Result<Unit>
    suspend fun deleteStudentAttendance(eventId: String, studentId: String): Result<Unit>
}
