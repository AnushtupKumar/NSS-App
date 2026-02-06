package com.example.nssapp.core.domain.model

data class Admin(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val roll: String = "",
    val wings: List<String> = emptyList() // IDs of wings they manage
)
