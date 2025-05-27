package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Material

@Dao
interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: Material)

    @Query("SELECT * FROM material_table WHERE courseId = :courseId ORDER BY uploadDate DESC")
    suspend fun getMaterialsByCourse(courseId: Int): List<Material>

    @Query("SELECT * FROM material_table WHERE id = :materialId")
    suspend fun getMaterialById(materialId: Int): Material?

    @Query("SELECT * FROM material_table WHERE courseId = :courseId AND uploadDate = :uploadDate")
    suspend fun getMaterialsForCourseOnDate(courseId: Int, uploadDate: Long): List<Material>

    @Update
    suspend fun updateMaterial(material: Material)

    @Delete
    suspend fun deleteMaterial(material: Material)

    @Query("DELETE FROM material_table")
    suspend fun deleteAll()
}