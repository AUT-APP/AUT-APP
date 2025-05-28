package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "department_table")
data class Department(
    @PrimaryKey(autoGenerate = true) val departmentId: Int = 0,
    val name: String,
    val type: String, // "Department", "Major", or "Minor"
    val description: String? = null
)