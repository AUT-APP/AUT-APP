package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "course_material_table",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)

data class CourseMaterial(
    @PrimaryKey(autoGenerate = true)
    val materialId: Int = 0,
    val courseId: Int,
    val title: String,
    val description: String,
    val type: String,
    val contentUrl: String? = null
)
