package com.example.nssapp.feature.auth.domain.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser: FirebaseUser?
    
    suspend fun login(emailOrRoll: String, password: String): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun logout()
    suspend fun getUserRole(): Result<String> // "admin" or "student"
}
