package com.example.autapp.data.repository

import com.example.autapp.data.dao.GradeDao
import com.example.autapp.data.models.Grade
import kotlin.math.round

class GradeRepository(
    private val gradeDao: GradeDao,
    private val assignmentRepository: AssignmentRepository
) {
    // CRUD Operations for Grade
    suspend fun insertGrade(grade: Grade) {
        val existingGrade = gradeDao.getGradesByAssignment(grade.assignmentId)
            .firstOrNull { it.studentId == grade.studentId }
        if (existingGrade != null) {
            throw IllegalStateException("A grade already exists for student ${grade.studentId} on assignment ${grade.assignmentId}")
        }
        gradeDao.insertGrade(grade)
    }

    suspend fun getGradeById(gradeId: Int): Grade? {
        return gradeDao.getGradeById(gradeId)
    }

    suspend fun getGradesByAssignment(assignmentId: Int): List<Grade> {
        return gradeDao.getGradesByAssignment(assignmentId)
    }

    suspend fun getGradesByStudent(studentId: Int): List<Grade> {
        return gradeDao.getGradesByStudent(studentId)
    }

    suspend fun getAllGrades(): List<Grade> {
        return gradeDao.getAllGrades()
    }

    suspend fun deleteGrade(grade: Grade) {
        gradeDao.deleteGrade(grade)
    }

    suspend fun updateGrade(grade: Grade) {
        gradeDao.updateGrade(grade)
    }

    // Join Queries for Grade + Assignment
    suspend fun getGradesWithAssignments(studentId: Int): List<GradeDao.GradeWithAssignment> {
        return gradeDao.getGradesWithAssignments(studentId)
    }

    suspend fun getGradesWithAssignmentsSortedByDate(studentId: Int): List<GradeDao.GradeWithAssignment> {
        return gradeDao.getGradesWithAssignmentsSortedByDate(studentId)
    }

    suspend fun getGradesWithAssignmentsByCourse(studentId: Int, courseId: Int): List<GradeDao.GradeWithAssignment> {
        return gradeDao.getGradesWithAssignmentsByCourse(studentId, courseId)
    }

    suspend fun getGradesWithAssignmentsByType(studentId: Int, type: String): List<GradeDao.GradeWithAssignment> {
        return gradeDao.getGradesWithAssignmentsByType(studentId, type)
    }

    // GPA Calculation
    suspend fun calculateGPA(studentId: Int): Double? {
        val studentGrades = getGradesByStudent(studentId)
        if (studentGrades.isEmpty()) return null

        val assignmentIds = studentGrades.map { it.assignmentId }.distinct()
        val assignments = assignmentIds.mapNotNull { assignmentRepository.getAssignmentById(it) }

        val totalPoints = assignments.sumOf { it.weight }
        if (totalPoints < 240.0) return null

        val weightedGradeSum = studentGrades.sumOf { grade ->
            val assignment = assignments.firstOrNull { it.assignmentId == grade.assignmentId }
            if (assignment != null) {
                val gradeValue = grade.getNumericValue()
                gradeValue * assignment.weight
            } else {
                0.0
            }
        }

        val gpa = weightedGradeSum / totalPoints
        return round(gpa * 1000) / 1000
    }

    // GPA Formatting
    fun formatGPA(gpa: Double?): String {
        return if (gpa != null) "GPA: $gpa / 9.0" else "GPA: Not available (insufficient credits)"
    }
}