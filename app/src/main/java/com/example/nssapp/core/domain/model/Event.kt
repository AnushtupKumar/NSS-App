package com.example.nssapp.core.domain.model

data class Event(
    val id: String = "",
    val title: String = "",
    val type: String = "", // "Camp", "Meeting"
    val date: Long = 0,
    val positiveHours: Double = 0.0,
    val negativeHours: Double = 0.0,
    val targetWings: List<String> = emptyList(), // Wing IDs
    val mandatory: Boolean = false,
    val studentsExcluded: List<String> = emptyList(),
    val createdBy: String = "", // Admin ID
    val isPenaltyApplied: Boolean = false
)
