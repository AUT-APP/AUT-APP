package com.example.autapp.data.firebase

import com.example.autapp.data.models.Grade
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseGradeRepository : BaseFirebaseRepository<FirebaseGrade>("grades") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseGrade {
        return FirebaseGrade(
            gradeId = documentId,
            assignmentId = document["assignmentId"] as? String ?: "",
            studentId = document["studentId"] as? String ?: "",
            _score = (document["score"] as? Number)?.toDouble() ?: 0.0,
            grade = document["grade"] as? String ?: "",
            feedback = document["feedback"] as? String
        )
    }

    override fun objectToDocument(obj: FirebaseGrade): Map<String, Any?> {
        return mapOf(
            "assignmentId" to obj.assignmentId,
            "studentId" to obj.studentId,
            "score" to obj.score,
            "grade" to obj.grade,
            "feedback" to obj.feedback
        )
    }

    suspend fun getGradesByStudent(studentId: String): List<FirebaseGrade> {
        return try {
            val snapshot = collection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getGradesByAssignment(assignmentId: String): List<FirebaseGrade> {
        return try {
            val snapshot = collection
                .whereEqualTo("assignmentId", assignmentId)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudentGradeForAssignment(studentId: String, assignmentId: String): FirebaseGrade? {
        return try {
            val snapshot = collection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("assignmentId", assignmentId)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.data?.let { documentToObject(snapshot.documents.first().id, it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateGrade(grade: FirebaseGrade): Boolean {
        return try {
            val query = collection.whereEqualTo("gradeId", grade.gradeId).get().await()
            if (query.isEmpty) return false
            
            val doc = query.documents.first()
            doc.reference.set(objectToDocument(grade)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun calculateAverageGrade(studentId: String): Double {
        val grades = getGradesByStudent(studentId)
        if (grades.isEmpty()) return 0.0

        // Calculate weighted average based on grade numeric values and paper points (assuming 15 points per paper)
        val totalGradePoints = grades.sumOf { it.getNumericValue() * 15 }
        val totalPaperPoints = grades.size * 15
        return if (totalPaperPoints > 0) totalGradePoints.toDouble() / totalPaperPoints else 0.0
    }

    suspend fun getGradeDistribution(assignmentId: Int): Map<String, Int> {
        val grades = getGradesByAssignment(assignmentId.toString())
        return grades.groupingBy { it.grade }
            .eachCount()
    }
} 