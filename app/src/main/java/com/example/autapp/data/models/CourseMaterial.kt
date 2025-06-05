package com.example.autapp.data.models

data class CourseMaterial(
    val materialId: String = "",
    val courseId: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val contentUrl: String? = null
)
