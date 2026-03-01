package com.example.nssapp.core.domain.model

import com.google.firebase.firestore.PropertyName

data class Wing(
    val id: String = "",
    val name: String = "",
    val maxEnrollment: Int = 0,
    val adminIds: List<String> = emptyList(),
    val modifiedDate: Long = System.currentTimeMillis(),
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false
)
