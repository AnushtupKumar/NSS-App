package com.example.nssapp.feature.student.data.repository

import com.example.nssapp.core.domain.model.AttendanceStatus
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.EventStatus
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StudentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StudentRepository {

    override suspend fun getStudentProfile(studentId: String): Result<Student> {
        return try {
             val snapshot = firestore.collection("students").document(studentId).get().await()
             val student = snapshot.toObject(Student::class.java)
             if (student != null) {
                 Result.success(student.copy(id = snapshot.id))
             } else {
                 Result.failure(Exception("Student not found"))
             }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllEvents(): Flow<List<Event>> {
        return firestore.collection("events")
            .snapshots()
            .map { snapshot -> 
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }
            }
            .catch { emit(emptyList()) }
    }

    override fun getAttendedEvents(studentId: String): Flow<List<String>> {
        return firestore.collectionGroup("attendance")
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("status", AttendanceStatus.PRESENT.value)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.reference.parent.parent?.id
                }
            }
            .catch { emit(emptyList()) }
    }

    override fun getAttendanceStatuses(studentId: String): Flow<Map<String, String>> {
        return firestore.collectionGroup("attendance")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val eventId = doc.reference.parent.parent?.id
                    val status = doc.getString("status")
                    if (eventId != null && status != null) {
                        eventId to status
                    } else null
                }.toMap()
            }
            .catch { emit(emptyMap()) }
    }

    override suspend fun markAttendance(eventId: String, studentId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            val eventDoc = eventRef.get().await()
            if (!eventDoc.exists()) {
                return Result.failure(Exception("Event not found"))
            }
            val event = eventDoc.toObject(Event::class.java) ?: return Result.failure(Exception("Event data error"))
            
            if (event.status != EventStatus.ACTIVE.value) {
               return Result.failure(Exception("Event is not currently active"))
            }

            // Check if student belongs to an ACTIVE wing that is targeted
            val studentProfile = getStudentProfile(studentId).getOrThrow()
            val allWings = getAllWingsList()
            val hasActiveTargetedWing = event.targetWings.isEmpty() || 
                event.targetWings.any { it in studentProfile.enrolledWings }
            
            if (!hasActiveTargetedWing) {
                return Result.failure(Exception("You do not belong to a wing targeted for this event"))
            }

            // Check if already present to avoid overwrites
            val docRef = eventRef.collection("attendance").document(studentId)
            
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                return Result.failure(Exception("Attendance already marked"))
            }

            val attendanceData = mapOf(
                "studentId" to studentId,
                "timestamp" to System.currentTimeMillis(),
                "status" to AttendanceStatus.PRESENT.value
            )

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStatus = snapshot.getString("status")
                
                if (currentStatus == AttendanceStatus.PRESENT.value) {
                    throw Exception("Attendance already marked")
                }
                
                val studentRef = firestore.collection("students").document(studentId)
                val studentSnapshot = transaction.get(studentRef)
                val currentHours = studentSnapshot.getDouble("totalHours") ?: 0.0
                
                var hoursToAdd = event.positiveHours
                // If they were penalized, restore those hours manually (though Student marking is usually for Active status)
                if (currentStatus == AttendanceStatus.PENALTY.value) {
                    hoursToAdd += event.negativeHours
                }
                
                transaction.update(studentRef, "totalHours", currentHours + hoursToAdd)
                transaction.set(docRef, attendanceData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllWingsList(): List<com.example.nssapp.core.domain.model.Wing> {
        return try {
            val snapshot = firestore.collection("wings").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.example.nssapp.core.domain.model.Wing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun checkAttendanceStatus(eventId: String, studentId: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection("events").document(eventId)
                .collection("attendance").document(studentId)
                .get()
                .await()
            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
