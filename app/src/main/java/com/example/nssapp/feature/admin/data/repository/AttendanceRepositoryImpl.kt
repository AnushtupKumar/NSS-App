package com.example.nssapp.feature.admin.data.repository

import com.example.nssapp.core.domain.model.Event
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
        status: String,
        bypassRestrictions: Boolean
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
            val studentPrimaryWing = studentDoc.getString("primaryWing") ?: ""
            val studentEnrolledWings =
                studentDoc.get("enrolledWings") as? List<String> ?: emptyList()

            // Fetch Event Details to check restrictions
            val eventDoc = firestore.collection("events").document(eventId).get().await()
            if (!eventDoc.exists()) {
                return Result.failure(Exception("Event not found"))
            }

            val eventStatus = eventDoc.getString("status") ?: "UPCOMING"

            if (!bypassRestrictions) {
                if (eventStatus != "ACTIVE") {
                    return Result.failure(Exception("Event is not currently active"))
                }
                val targetWings = eventDoc.get("targetWings") as? List<String> ?: emptyList()
                val studentsExcluded =
                    eventDoc.get("studentsExcluded") as? List<String> ?: emptyList()

                // Check Exclusion
                if (studentsExcluded.contains(studentId)) {
                    return Result.failure(Exception("Student is excluded from this event"))
                }

                // Check Wing Target
                // If targetWings is not empty, student must belong to at least one target wing
                if (targetWings.isNotEmpty()) {
                    val hasMatchingWing = targetWings.any {
                        it == studentPrimaryWing || studentEnrolledWings.contains(it)
                    }
                    if (!hasMatchingWing) {
                        return Result.failure(Exception("Student's Wing is not targeted for this event"))
                    }
                }
            }

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

    override suspend fun markAttendanceBulk(
        eventId: String,
        rollNumbers: List<String>,
        status: String,
        bypassRestrictions: Boolean
    ): Result<List<String>> {
        val failedRolls = mutableListOf<String>()
        // Naive implementation: Loop and call markAttendance. 
        // Optimization: Fetch all students in one query if possible (using 'in' query limited to 10 at a time? or just 1 by 1)
        // 1 by 1 is safe for now.

        for (roll in rollNumbers) {
            val result = markAttendance(eventId, roll, status, bypassRestrictions)
            if (result.isFailure) {
                failedRolls.add(roll) // Could store reason too, but simple list for now
            }
        }

        return Result.success(failedRolls)
    }

    override suspend fun applyPenaltyForEvent(eventId: String): Result<Int> {
        return try {
            val eventDoc = firestore.collection("events").document(eventId).get().await()
            val event = eventDoc.toObject(Event::class.java)?.copy(id = eventDoc.id)
                ?: return Result.failure(Exception("Event not found"))

            if (event.isPenaltyApplied) {
                return Result.failure(Exception("Penalty already applied for this event"))
            }

            if (event.status != "ACTIVE" && event.status != "COMPLETED") {
                return Result.failure(Exception("Penalty can only be applied to Active or Completed events"))
            }

            val mandatoryWings = event.mandatoryWings.ifEmpty { event.targetWings }

            if (mandatoryWings.isEmpty()) {
                return Result.failure(Exception("No wings targeted for mandatory attendance"))
            }

            val targetStudentIds = mutableSetOf<String>()

            for (wingId in mandatoryWings) {
                val primaryQuery =
                    firestore.collection("students").whereEqualTo("primaryWing", wingId).get()
                        .await()
                targetStudentIds.addAll(primaryQuery.documents.map { it.id })

                val enrolledQuery =
                    firestore.collection("students").whereArrayContains("enrolledWings", wingId)
                        .get().await()
                targetStudentIds.addAll(enrolledQuery.documents.map { it.id })
            }

            targetStudentIds.removeAll(event.studentsExcluded.toSet())

            val attendanceQuery =
                firestore.collection("events").document(eventId).collection("attendance").get()
                    .await()
            val presentStudentIds = attendanceQuery.documents.map { it.id }.toSet()

            var penaltyCount = 0
            val batch = firestore.batch()
            val penaltyTimestamp = System.currentTimeMillis()

            for (studentId in targetStudentIds) {
                if (!presentStudentIds.contains(studentId)) {
                    val attendanceRef = firestore.collection("events").document(eventId)
                        .collection("attendance").document(studentId)

                    val attendanceData = hashMapOf(
                        "status" to "PENALTY",
                        "timestamp" to penaltyTimestamp,
                        "scannedBy" to "SYSTEM_PENALTY",
                        "studentId" to studentId
                    )
                    batch.set(attendanceRef, attendanceData)
                    penaltyCount++
                }
            }

            val eventRef = firestore.collection("events").document(eventId)
            batch.update(eventRef, "isPenaltyApplied", true)

            batch.commit().await()

            Result.success(penaltyCount)
        } catch (e: Exception) {
            Result.failure(e)
        }}
        override suspend fun clearEventPenalties(eventId: String): Result<Unit> {
            return try {
                val query = firestore.collection("events").document(eventId)
                    .collection("attendance")
                    .whereEqualTo("status", "PENALTY")
                    .get()
                    .await()

                if (query.isEmpty) return Result.success(Unit)

                val batch = firestore.batch()
                for (doc in query.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

