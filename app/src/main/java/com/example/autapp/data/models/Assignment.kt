package com.example.autapp.data.models

import java.util.Date

data class Assignment(
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