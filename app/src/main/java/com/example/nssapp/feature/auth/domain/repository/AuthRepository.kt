package com.example.nssapp.feature.auth.domain.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser: FirebaseUser?
    
    suspend fun login(email: String, roll: String): Result<Unit> // Using Roll No as password initially? Or Email/Password? Plan said Email.
    suspend fun signup(name: String, email: String, pass: String, roll: String): Result<Unit>
    suspend fun logout()
    suspend fun getUserRole(): Result<String> // "admin" or "student"
}
