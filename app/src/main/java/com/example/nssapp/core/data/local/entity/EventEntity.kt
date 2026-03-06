package com.example.nssapp.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nssapp.core.domain.model.Event

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val date: Long,
    val positiveHours: Double,
    val negativeHours: Double,
    val targetWings: String, // Stored as comma-separated string
    val mandatory: Boolean,
    val studentsExcluded: String, // Stored as comma-separated string
    val createdBy: String,
    val isPenaltyApplied: Boolean,
    val status: String,
    val startTime: Long,
    val endTime: Long,
    val mandatoryWings: String // Stored as comma-separated string
) {
    fun toEvent(): Event {
        return Event(
            id = id,
            title = title,
            description = description,
            date = date,
            positiveHours = positiveHours,
            negativeHours = negativeHours,
            targetWings = if (targetWings.isBlank()) emptyList() else targetWings.split(","),
            mandatory = mandatory,
            studentsExcluded = if (studentsExcluded.isBlank()) emptyList() else studentsExcluded.split(","),
            createdBy = createdBy,
            isPenaltyApplied = isPenaltyApplied,
            status = status,
            startTime = startTime,
            endTime = endTime,
            mandatoryWings = if (mandatoryWings.isBlank()) emptyList() else mandatoryWings.split(",")
        )
    }

    companion object {
        fun fromEvent(event: Event): EventEntity {
            return EventEntity(
                id = event.id,
                title = event.title,
                description = event.description,
                date = event.date,
                positiveHours = event.positiveHours,
                negativeHours = event.negativeHours,
                targetWings = event.targetWings.joinToString(","),
                mandatory = event.mandatory,
                studentsExcluded = event.studentsExcluded.joinToString(","),
                createdBy = event.createdBy,
                isPenaltyApplied = event.isPenaltyApplied,
                status = event.status,
                startTime = event.startTime,
                endTime = event.endTime,
                mandatoryWings = event.mandatoryWings.joinToString(",")
            )
        }
    }
}
