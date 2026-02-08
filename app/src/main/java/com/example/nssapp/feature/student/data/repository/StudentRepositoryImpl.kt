package com.example.nssapp.feature.student.data.repository

import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
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

    override fun getEventsForWing(wingId: String): Flow<List<Event>> {
        // Query events where targetWings contains wingId
        return firestore.collection("events")
            .whereArrayContains("targetWings", wingId)
            .snapshots()
            .map { it.toObjects(Event::class.java) }
    }

    override suspend fun getAttendedEvents(studentId: String): Result<List<String>> {
        return try {
            val snapshot = firestore.collectionGroup("attendance")
                .whereEqualTo("status", "Present")
                .whereEqualTo("studentId", studentId) 
                .get()
                .await()
                
            val eventIds = snapshot.documents.mapNotNull { doc ->
                doc.reference.parent.parent?.id
            }
            
            Result.success(eventIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAttendance(eventId: String, studentId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            if (!eventRef.get().await().exists()) {
                return Result.failure(Exception("Event not found"))
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
                "status" to "Present"
            )

            docRef.set(attendanceData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
