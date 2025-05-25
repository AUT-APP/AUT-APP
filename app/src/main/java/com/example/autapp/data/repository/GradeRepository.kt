package com.example.autapp.data.repository

import com.example.autapp.data.dao.GradeDao
import com.example.autapp.data.models.Grade
import kotlin.math.round

class GradeRepository(
    private val gradeDao: GradeDao,
    private val assignmentRepository: AssignmentRepository
) {
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

    suspend fun updateGrade(grade: Grade) {
        gradeDao.updateGrade(grade)
    }

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

    suspend fun calculateGPA(studentId: Int): Double? {
        val gradesWithAssignments = getGradesWithAssignments(studentId)
        if (gradesWithAssignments.isEmpty()) return null

        val creditsPerCourse = 15.0
        val totalCredits = gradesWithAssignments.size * creditsPerCourse

        if (totalCredits < 240.0) return null

        val weightedGradeSum = gradesWithAssignments.sumOf { gradeWithAssignment ->
            val grade = gradeWithAssignment.grade
            val numericValue = grade.getNumericValue().toDouble()
            numericValue * creditsPerCourse
        }

        val gpa = weightedGradeSum / totalCredits
        return round(gpa * 1000) / 1000
    }

    fun formatGPA(gpa: Double?): String {
        return if (gpa != null) String.format("%.3f / 9.0", gpa) else "GPA: Not available (insufficient credits)"
    }

}