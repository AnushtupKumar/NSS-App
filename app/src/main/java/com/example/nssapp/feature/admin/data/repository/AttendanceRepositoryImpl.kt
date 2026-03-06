package com.example.nssapp.feature.admin.data.repository

import com.example.nssapp.core.domain.model.AttendanceStatus
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.EventStatus
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.feature.admin.domain.repository.AttendanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AttendanceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AttendanceRepository {

    private fun mapStudent(doc: DocumentSnapshot): Student {
        val student = doc.toObject(Student::class.java) ?: return Student()
        val primaryWing = doc.getString("primaryWing")
        return if (student.enrolledWings.isEmpty() && !primaryWing.isNullOrEmpty()) {
            student.copy(id = doc.id, enrolledWings = listOf(primaryWing))
        } else {
            student.copy(id = doc.id)
        }
    }


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
            val student = mapStudent(studentDoc)

            markAttendanceInternal(eventId, student, status, bypassRestrictions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun markAttendanceInternal(
        eventId: String,
        student: com.example.nssapp.core.domain.model.Student,
        status: String,
        bypassRestrictions: Boolean
    ): Result<Unit> {
        return try {
            val studentId = student.id
            val studentEnrolledWings = student.enrolledWings
            val isBypassed = bypassRestrictions

            // Fetch Event Details to check restrictions
            val eventDoc = firestore.collection("events").document(eventId).get().await()
            if (!eventDoc.exists()) {
                return Result.failure(Exception("Event not found"))
            }

            val eventStatus = eventDoc.getString("status") ?: EventStatus.IDLE.value

            if (!bypassRestrictions) {
                if (eventStatus != EventStatus.ACTIVE.value) {
                    return Result.failure(Exception("Event is not currently active"))
                }
                val targetWings = eventDoc.get("targetWings") as? List<String> ?: emptyList()

                // Check Wing Target
                // If targetWings is not empty, student must belong to at least one target wing
                if (targetWings.isNotEmpty()) {
                    val hasMatchingWing = targetWings.any {
                        studentEnrolledWings.contains(it)
                    }
                    if (!hasMatchingWing) {
                        return Result.failure(Exception("Student's Wing is not targeted for this event"))
                    }
                }
            }

            val attendanceRef = firestore.collection("events").document(eventId)
                .collection("attendance").document(studentId)

            val attendanceData = hashMapOf(
                "status" to AttendanceStatus.PRESENT.value,
                "timestamp" to System.currentTimeMillis(),
                "scannedBy" to (auth.currentUser?.uid ?: "unknown"),
                "studentId" to studentId
            )

            // Data Aggregation: Update student totalHours
            val studentRef = firestore.collection("students").document(studentId)
            val positiveHours = eventDoc.getDouble("positiveHours") ?: 0.0
            
            firestore.runTransaction { transaction ->
                val currentAttendance = transaction.get(attendanceRef)
                val currentStatus = currentAttendance.getString("status")
                
                if (currentStatus != AttendanceStatus.PRESENT.value) {
                    // Increment hours if they weren't already PRESENT
                    val studentSnapshot = transaction.get(studentRef)
                    val currentHours = studentSnapshot.getDouble("totalHours") ?: 0.0
                    
                    var hoursToAdd = positiveHours
                    // If they were penalized, we should restore those hours too? 
                    // Actually, the user says "mark attendance... status of present". 
                    // If they were already penalized, maybe we should just override.
                    if (currentStatus == AttendanceStatus.PENALTY.value) {
                        hoursToAdd += eventDoc.getDouble("negativeHours") ?: 0.0
                    }
                    
                    transaction.update(studentRef, "totalHours", currentHours + hoursToAdd)
                }
                transaction.set(attendanceRef, attendanceData)
            }.await()
            
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
        
        // Firestore 'in' queries are limited to 30 items
        val chunks = rollNumbers.distinct().chunked(30)
        
        for (chunk in chunks) {
            try {
                val studentsQuery = firestore.collection("students")
                    .whereIn("roll", chunk)
                    .get()
                    .await()
                
                val foundRolls = studentsQuery.documents.mapNotNull { it.getString("roll") }.toSet()
                val missingRolls = chunk.filter { it !in foundRolls }
                failedRolls.addAll(missingRolls)
                
                // For found students, we can now mark them directly
                for (doc in studentsQuery.documents) {
                    val student = mapStudent(doc)
                    val result = markAttendanceInternal(eventId, student, status, bypassRestrictions)
                    if (result.isFailure) {
                        failedRolls.add(student.roll)
                    }
                }
            } catch (e: Exception) {
                failedRolls.addAll(chunk)
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

            if (event.status != EventStatus.ACTIVE.value && event.status != EventStatus.IDLE.value) {
                return Result.failure(Exception("Penalty can only be applied to active or idle events"))
            }

            val mandatoryWings = event.mandatoryWings.ifEmpty { event.targetWings }

            if (mandatoryWings.isEmpty()) {
                return Result.failure(Exception("No wings targeted for mandatory attendance"))
            }

            val targetStudentIds = mutableSetOf<String>()

            // Optimize: Use 'in' filter if wing count is small, or loop. Firestore 'in' has limit of 10.
            // For now, loop is fine but let's ensure we fetch student IDs once.
            for (wingId in mandatoryWings) {
                // Check enrolledWings list
                val enrolledQuery = firestore.collection("students")
                    .whereArrayContains("enrolledWings", wingId)
                    .get()
                    .await()
                targetStudentIds.addAll(enrolledQuery.documents.map { it.id })
            }

            targetStudentIds.removeAll(event.studentsExcluded.toSet())

            val attendanceQuery =
                firestore.collection("events").document(eventId).collection("attendance")
                    .get()
                    .await()
            val presentStudentIds = attendanceQuery.documents
                .filter { it.getString("status") == AttendanceStatus.PRESENT.value }
                .map { it.id }.toSet()

            var penaltyCount = 0
            val batch = firestore.batch()
            val penaltyTimestamp = System.currentTimeMillis()

            for (studentId in targetStudentIds) {
                if (!presentStudentIds.contains(studentId)) {
                    val attendanceRef = firestore.collection("events").document(eventId)
                        .collection("attendance").document(studentId)

                    val attendanceData = hashMapOf(
                        "status" to AttendanceStatus.PENALTY.value,
                        "timestamp" to penaltyTimestamp,
                        "scannedBy" to "SYSTEM_PENALTY",
                        "studentId" to studentId
                    )
                    batch.set(attendanceRef, attendanceData)
                    
                    // Update student totalHours (negative)
                    val studentRef = firestore.collection("students").document(studentId)
                    batch.update(studentRef, "totalHours", com.google.firebase.firestore.FieldValue.increment(-event.negativeHours))
                    
                    penaltyCount++
                }
            }

            val eventRef = firestore.collection("events").document(eventId)
            batch.update(eventRef, "isPenaltyApplied", true)

            batch.commit().await()

            Result.success(penaltyCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
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

