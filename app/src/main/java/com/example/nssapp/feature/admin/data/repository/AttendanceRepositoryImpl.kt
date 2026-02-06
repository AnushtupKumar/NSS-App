package com.example.nssapp.feature.admin.data.repository

import com.example.nssapp.feature.admin.domain.repository.AttendanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AttendanceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AttendanceRepository {

    override suspend fun markAttendance(
        eventId: String,
        studentRoll: String,
        status: String
    ): Result<Unit> {
        return try {
            val studentQuery = firestore.collection("students")
                .whereEqualTo("roll", studentRoll)
                .limit(1)
                .get()
                .await()

            if (studentQuery.isEmpty) {
                return Result.failure(Exception("Student with Roll No $studentRoll not found"))
            }

            val studentDoc = studentQuery.documents.first()
            val studentId = studentDoc.id

            val attendanceRef = firestore.collection("events").document(eventId)
                .collection("attendance").document(studentId)

            val attendanceData = hashMapOf(
                "status" to status,
                "timestamp" to System.currentTimeMillis(),
                "scannedBy" to (auth.currentUser?.uid ?: "unknown"),
                "studentId" to studentId
            )

            attendanceRef.set(attendanceData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
