package com.example.nssapp.core.domain.model

enum class AttendanceStatus(val value: String) {
    PRESENT("PRESENT"),
    ABSENT("ABSENT"),
    PENALTY("PENALTY");

    companion object {
        fun fromString(value: String): AttendanceStatus {
            return entries.find { it.value == value } ?: PRESENT
        }
    }
}
