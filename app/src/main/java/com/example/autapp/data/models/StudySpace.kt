package com.example.autapp.data.models
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_space_table")
data class StudySpace(
    @PrimaryKey val spaceId: String,
    val building: String,
    val campus: String,
    val level: String,
    val capacity: Int,
    val isAvailable: Boolean
)

