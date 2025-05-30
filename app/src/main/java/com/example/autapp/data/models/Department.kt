package com.example.autapp.data.models

data class Department(
    val departmentId: Int = 0,
    val name: String,
    val type: String, // "Department", "Major", or "Minor"
    val description: String? = null
)