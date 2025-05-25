package com.example.autapp.data.repository

import com.example.autapp.data.dao.AssignmentDao
import com.example.autapp.data.models.Assignment

class AssignmentRepository(private val assignmentDao: AssignmentDao) {

    suspend fun insertAssignment(assignment: Assignment) {
        assignmentDao.insertAssignment(assignment)
    }

    suspend fun getAssignmentById(assignmentId: Int): Assignment? {
        return assignmentDao.getAssignmentById(assignmentId)
    }

    suspend fun getAllAssignments(): List<Assignment> {
        return assignmentDao.getAllAssignments()
    }

    suspend fun getAssignmentsForTeacherCourses(teacherId: Int): List<Assignment> {
        return assignmentDao.getAssignmentsForTeacherCourses(teacherId)
    }

    suspend fun updateAssignment(assignment: Assignment) {
        assignmentDao.updateAssignment(assignment)
    }

    suspend fun deleteAssignment(assignment: Assignment) {
        assignmentDao.deleteAssignment(assignment)
    }
}