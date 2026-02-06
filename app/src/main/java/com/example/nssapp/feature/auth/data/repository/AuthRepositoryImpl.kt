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
            
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun getUserRole(): Result<String> {
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

        return Result.failure(Exception("User role not found"))
    }
}
