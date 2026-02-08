package com.example.nssapp.core.domain.model

data class Student(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val roll: String = "",
    val primaryWing: String = "",
    val enrolledWings: List<String> = emptyList(), // List of Wing IDs
    val eventsAttended: List<String> = emptyList(),
    val password: String = "" // Added field for initial password management (Not production secure but fits requirement)
)
