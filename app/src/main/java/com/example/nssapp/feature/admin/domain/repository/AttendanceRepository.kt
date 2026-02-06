package com.example.nssapp.feature.admin.domain.repository

interface AttendanceRepository {
    // Returns success if attendance marked, failure if student not found or error
    suspend fun markAttendance(
        eventId: String, 
        studentRoll: String, // Using Roll No as it's easier to type manually than a UUID
        status: String = "Present"
    ): Result<Unit>
}
