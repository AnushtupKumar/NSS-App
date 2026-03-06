package com.example.nssapp.core.domain.model

data class Attendance(
    val studentId: String = "",
    val status: String = AttendanceStatus.ABSENT.value,
    val timestamp: Long = System.currentTimeMillis(),
    val scannedBy: String = "unknown"
)
