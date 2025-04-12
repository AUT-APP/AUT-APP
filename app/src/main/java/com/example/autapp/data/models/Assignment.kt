package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "assignment_table",
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
data class Assignment(
    @PrimaryKey(autoGenerate = true)
    val assignmentId: Int = 0,
    var name: String,
    var location: String,
    var due: Date,
    var weight: Double,
    var maxScore: Double,
    var type: String,
    var courseId: Int
) {
    override fun toString(): String {
        return "Assignment(assignmentId=$assignmentId, name='$name', location='$location', due=$due, weight=$weight, maxScore=$maxScore, type='$type', courseId=$courseId)"
    }
}