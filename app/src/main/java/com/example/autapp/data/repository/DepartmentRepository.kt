package com.example.autapp.data.repository

import com.example.autapp.data.dao.DepartmentDao
import com.example.autapp.data.models.Department

class DepartmentRepository(private val departmentDao: DepartmentDao) {
    suspend fun insertDepartment(department: Department) {
        departmentDao.insertDepartment(department)
    }

    suspend fun getDepartmentById(departmentId: Int): Department? {
        return departmentDao.getDepartmentById(departmentId)
    }

    suspend fun getAllDepartments(): List<Department> {
        return departmentDao.getAllDepartments()
    }

    suspend fun updateDepartment(department: Department) {
        departmentDao.updateDepartment(department)
    }

    suspend fun deleteDepartment(department: Department) {
        departmentDao.deleteDepartment(department)
    }
}