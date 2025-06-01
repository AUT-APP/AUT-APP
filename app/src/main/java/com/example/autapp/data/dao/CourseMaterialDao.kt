package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.CourseMaterial

@Dao
interface CourseMaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: CourseMaterial)

    @Query("SELECT * FROM course_material_table WHERE courseId = :courseId")
    suspend fun getMaterialsForCourse(courseId: Int): List<CourseMaterial>

    @Query("SELECT * FROM course_material_table")
    suspend fun getAllMaterials(): List<CourseMaterial>

    @Query("SELECT * FROM course_material_table WHERE materialId = :materialId")
    suspend fun getMaterialById(materialId: Int): CourseMaterial?

    @Update
    suspend fun updateMaterial(material: CourseMaterial)

    @Delete
    suspend fun deleteMaterial(material: CourseMaterial)
}
