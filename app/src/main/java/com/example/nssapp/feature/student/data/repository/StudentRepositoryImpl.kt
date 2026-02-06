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
}
