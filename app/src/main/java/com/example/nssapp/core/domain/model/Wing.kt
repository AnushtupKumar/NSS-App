package com.example.nssapp.core.domain.model

data class Wing(
    val id: String = "",
    val name: String = "",
    val maxEnrollment: Int = 0,
    val modifiedDate: Long = System.currentTimeMillis()
)
