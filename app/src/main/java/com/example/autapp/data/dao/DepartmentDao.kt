package com.example.autapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.autapp.data.models.Department

@Dao
interface DepartmentDao {
    @Insert
    suspend fun insertDepartment(department: Department)

    @Query("SELECT * FROM department_table WHERE departmentId = :departmentId")
    suspend fun getDepartmentById(departmentId: Int): Department?

    @Query("SELECT * FROM department_table")
    suspend fun getAllDepartments(): List<Department>

    @Update
    suspend fun updateDepartment(department: Department)

    @Delete
    suspend fun deleteDepartment(department: Department)
}