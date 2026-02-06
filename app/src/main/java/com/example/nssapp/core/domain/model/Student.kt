package com.example.nssapp.core.domain.model

data class Student(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val roll: String = "",
    val primaryWing: String = "", // Wing ID
    val eventsAttended: List<String> = emptyList() // Keeping as list for quick checks, but main logic via sub-collections
)
