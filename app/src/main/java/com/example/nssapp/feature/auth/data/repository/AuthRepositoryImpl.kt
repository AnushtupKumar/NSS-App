package com.example.nssapp.feature.auth.data.repository

import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun login(emailOrRoll: String, pass: String): Result<Unit> {
        return try {
            val email = if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrRoll).matches()) {
                emailOrRoll
            } else {
                // Assume it's a Roll No, find email
                val studentQuery = firestore.collection("students")
                    .whereEqualTo("roll", emailOrRoll)
                    .limit(1)
                    .get()
                    .await()
                
                if (!studentQuery.isEmpty) {
                     val email = studentQuery.documents.first().getString("email")
                     email ?: return Result.failure(Exception("Email not found for this Roll No"))
                } else {
                     // Check Admin
                     val adminQuery = firestore.collection("admins")
                        .whereEqualTo("roll", emailOrRoll)
                        .limit(1)
                        .get()
                        .await()
                     
                     if (!adminQuery.isEmpty) {
                         val email = adminQuery.documents.first().getString("email")
                         email ?: return Result.failure(Exception("Email not found for this Roll No"))
                     } else {
                         return Result.failure(Exception("User with this Roll No not found"))
                     }
                }
            }
            
            val authResult = try {
                auth.signInWithEmailAndPassword(email, pass).await()
                Result.success(Unit)
            } catch (e: Exception) {
                // Check if it's a "User Not Found" scenario but valid student credentials exist
                // Logic: 
                // 1. Check if user exists in Firestore (Student) with this email/roll and password
                // 2. If yes, create Auth user
                // 3. If no, return original error
                
                val studentQuery = firestore.collection("students")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()
                
                if (!studentQuery.isEmpty) {
                    val document = studentQuery.documents.first()
                    val storedPassword = document.getString("password")
                    
                    if (storedPassword == pass) {
                        // Valid credentials in Firestore, but no Auth User. Create one.
                        try {
                            auth.createUserWithEmailAndPassword(email, pass).await()
                            
                            // Migrate Data (Update ID)
                            val newUid = auth.currentUser?.uid ?: throw Exception("Auth failed after creation")
                            val oldDocRef = document.reference
                            val newDocRef = firestore.collection("students").document(newUid)
                            
                            firestore.runTransaction { transaction ->
                                val snapshot = transaction.get(oldDocRef)
                                val data = snapshot.data ?: return@runTransaction
                                val newData = data.toMutableMap()
                                newData["id"] = newUid
                                transaction.set(newDocRef, newData)
                                transaction.delete(oldDocRef)
                            }.await()
                            
                            Result.success(Unit)
                        } catch (createEx: Exception) {
                            Result.failure(createEx)
                        }
                    } else {
                         // Password mismatch for existing student record
                         Result.failure(Exception("Invalid Credentials"))
                    }
                } else {
                    // Not found in Firestore either (or maybe Admin? Admins should be pre-created mostly, but same logic could apply if needed. For now assume Admins have Auth.)
                    Result.failure(e)
                }
            }
            authResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun signup(email: String, pass: String, roll: String): Result<Unit> {
        return try {
            // 1. Check if student exists in Firestore with matching Roll and Password
            val query = firestore.collection("students")
                .whereEqualTo("roll", roll)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(Exception("Student record not found. Ask Admin to add you."))
            }

            val document = query.documents.first()
            val storedPassword = document.getString("password")

            if (storedPassword != pass) {
                return Result.failure(Exception("Invalid Password. Use the one provided by Admin."))
            }

            // 2. Create Firebase Auth User
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
            } catch (e: Exception) {
                // If user already exists in Auth but maybe just logging in via signup flow? 
                // Or maybe clean up? For now return failure.
                return Result.failure(Exception("Account creation failed: ${e.message}"))
            }

            // 3. Update Firestore Document ID to match Auth UID (Crucial for Role Check)
            val newUid = auth.currentUser?.uid ?: return Result.failure(Exception("Auth logic error"))
            
            // We need to move the document to the new ID or update the existing one?
            // Existing logic relies on Document ID == UID.
            // So we must COPY the data to a new document with ID = UID and DELETE the old one (which had auto-ID).
            
            val oldDocRef = document.reference
            val newDocRef = firestore.collection("students").document(newUid)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(oldDocRef)
                val data = snapshot.data ?: return@runTransaction
                // Update ID in data if consistent
                val newData = data.toMutableMap()
                newData["id"] = newUid
                
                transaction.set(newDocRef, newData)
                transaction.delete(oldDocRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun getUserRole(): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            
            // Check Admin collection first
            val adminDoc = firestore.collection("admins").document(uid).get().await()
            if (adminDoc.exists()) {
                return Result.success("admin")
            }

            // Check Student collection
            val studentDoc = firestore.collection("students").document(uid).get().await()
            if (studentDoc.exists()) {
                return Result.success("student")
            }

            Result.failure(Exception("User role not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
