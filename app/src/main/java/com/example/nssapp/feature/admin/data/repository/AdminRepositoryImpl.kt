package com.example.nssapp.feature.admin.data.repository

import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AdminRepository {

    override fun getWings(): Flow<List<Wing>> {
        return firestore.collection("wings")
            .snapshots()
            .map { snapshot -> 
                snapshot.toObjects(Wing::class.java)
            }
    }

    override suspend fun createWing(wing: Wing): Result<Unit> {
        return try {
            val docRef = if (wing.id.isEmpty()) {
                firestore.collection("wings").document()
            } else {
                firestore.collection("wings").document(wing.id)
            }
            val newWing = wing.copy(id = docRef.id)
            docRef.set(newWing).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStudents(wingId: String?): Flow<List<Student>> {
        var query = firestore.collection("students")
        if (wingId != null) {
            // Need to cast to correct type for whereEqualTo if needed, but collection reference is generic
             // .whereEqualTo("primaryWing", wingId) // This returns Query
             return firestore.collection("students")
                .whereEqualTo("primaryWing", wingId)
                .snapshots()
                .map { it.toObjects(Student::class.java) }
        }
        return firestore.collection("students")
            .snapshots()
            .map { it.toObjects(Student::class.java) }
    }

    override suspend fun createStudent(student: Student): Result<Unit> {
        return try {
            val docRef = if (student.id.isEmpty()) {
                firestore.collection("students").document()
            } else {
                firestore.collection("students").document(student.id)
            }
            val newStudent = student.copy(id = docRef.id)
            docRef.set(newStudent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEvents(): Flow<List<Event>> {
        return firestore.collection("events")
            .snapshots()
            .map { it.toObjects(Event::class.java) }
    }

    override suspend fun createEvent(event: Event): Result<Unit> {
         return try {
            val docRef = if (event.id.isEmpty()) {
                firestore.collection("events").document()
            } else {
                firestore.collection("events").document(event.id)
            }
            val newEvent = event.copy(id = docRef.id)
            docRef.set(newEvent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
