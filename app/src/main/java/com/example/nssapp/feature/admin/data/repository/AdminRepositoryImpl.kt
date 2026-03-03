package com.example.nssapp.feature.admin.data.repository

import com.example.nssapp.core.domain.model.Admin
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AdminRepository {

    override fun getAdmins(): Flow<List<Admin>> {
        return firestore.collection("admins")
            .snapshots()
            .map { snapshot -> 
                snapshot.documents.map { doc ->
                    doc.toObject(Admin::class.java)!!.copy(id = doc.id)
                }
            }
    }

    override fun getWings(): Flow<List<Wing>> {
        return firestore.collection("wings")
            .snapshots()
            .map { snapshot -> 
                snapshot.documents.map { doc ->
                    doc.toObject(Wing::class.java)!!.copy(id = doc.id)
                }.filter { !it.isDeleted }
            }
    }

    override fun getAllWings(): Flow<List<Wing>> {
        return firestore.collection("wings")
            .snapshots()
            .map { snapshot -> 
                snapshot.documents.map { doc ->
                    doc.toObject(Wing::class.java)!!.copy(id = doc.id)
                }
            }
    }

    override fun getWingById(wingId: String): Flow<Wing?> {
        return firestore.collection("wings").document(wingId)
            .snapshots()
            .map { doc -> 
                doc.toObject(Wing::class.java)?.copy(id = doc.id)
            }
    }

    override suspend fun createWing(wing: Wing, adminIds: List<String>): Result<Unit> {
        return try {
            val docRef = if (wing.id.isEmpty()) {
                firestore.collection("wings").document()
            } else {
                firestore.collection("wings").document(wing.id)
            }
            val validAdminIds = adminIds.filter { it.isNotBlank() }
            val newWing = wing.copy(id = docRef.id, adminIds = validAdminIds)
            
            val batch = firestore.batch()
            batch.set(docRef, newWing)
            
            // 1. Remove this wing ID from ALL admins who currently have it (to handle removals)
            val allAdminsQuery = firestore.collection("admins").whereArrayContains("wings", docRef.id).get().await()
            for (adminDoc in allAdminsQuery.documents) {
                batch.update(adminDoc.reference, "wings", FieldValue.arrayRemove(docRef.id))
            }
            
            // 2. Add this wing ID to the SELECTED admins
            for (adminId in validAdminIds) {
                val adminRef = firestore.collection("admins").document(adminId)
                batch.update(adminRef, "wings", FieldValue.arrayUnion(docRef.id))
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWing(wing: Wing): Result<Unit> {
        return try {
            firestore.collection("wings").document(wing.id).set(wing).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWing(wingId: String): Result<Unit> {
        return try {
            // SOFT DELETE: Just set the flag. References in students/events stay intact.
            firestore.collection("wings").document(wingId).update("isDeleted", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStudents(wingId: String?): Flow<List<Student>> {
        return firestore.collection("students")
            .snapshots()
            .map { snapshot -> 
                snapshot.documents.map { doc ->
                    val student = doc.toObject(Student::class.java)!!.copy(id = doc.id)
                    val primaryWing = doc.getString("primaryWing")
                    // MIGRATE: If enrolledWings is empty but primaryWing exists, use it.
                    if (student.enrolledWings.isEmpty() && !primaryWing.isNullOrEmpty()) {
                         student.copy(enrolledWings = listOf(primaryWing))
                    } else student
                }.let { list ->
                    if (wingId != null) {
                         list.filter { it.enrolledWings.contains(wingId) }
                    } else list
                }
            }
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

    override suspend fun updateStudent(student: Student): Result<Unit> {
        return try {
            firestore.collection("students").document(student.id).set(student).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStudent(studentId: String): Result<Unit> {
        return try {
            firestore.collection("students").document(studentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEvents(): Flow<List<Event>> {
        return firestore.collection("events")
            .snapshots()
            .map { snapshot -> 
                snapshot.documents.map { doc ->
                    val event = doc.toObject(Event::class.java)!!
                    val legacyPenalty = doc.getBoolean("penaltyApplied") ?: false
                    if (!event.isPenaltyApplied && legacyPenalty) {
                        event.copy(id = doc.id, isPenaltyApplied = true)
                    } else {
                        event.copy(id = doc.id)
                    }
                }
            }
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

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            
            // Fetch all attendance records for this event
            val attendanceSnapshot = eventRef.collection("attendance").get().await()
            
            val batch = firestore.batch()
            
            // Delete each attendance document
            for (doc in attendanceSnapshot.documents) {
                batch.delete(doc.reference)
            }
            
            // Finally delete the event document itself
            batch.delete(eventRef)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEventStatus(eventId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEventsByCreator(creatorId: String): Flow<List<Event>> {
        return firestore.collection("events")
            .whereEqualTo("createdBy", creatorId)
            .snapshots()
            .map { it.toObjects(Event::class.java) }
    }

    override suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            firestore.collection("events").document(event.id).set(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
