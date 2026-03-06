package com.example.nssapp.core.domain.model

enum class EventStatus(val value: String) {
    IDLE("idle"),
    ACTIVE("active");

    companion object {
        fun fromString(value: String): EventStatus {
            return entries.find { it.value == value } ?: IDLE
        }
    }
}
