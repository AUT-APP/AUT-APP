package com.example.autapp.data.models

data class StudySpace(
    val documentId: String,
    val spaceId: String,
    val building: String,
    val campus: String,
    val level: String,
    val capacity: Int,
    val isAvailable: Boolean
)

