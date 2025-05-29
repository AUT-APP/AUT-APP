package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "material_table")
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val title: String,
    val description: String,
    val fileUrl: String,
    val uploadDate: Date
)
