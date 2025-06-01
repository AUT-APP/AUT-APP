package com.example.autapp.data.repository

import com.example.autapp.data.dao.CourseMaterialDao
import com.example.autapp.data.models.CourseMaterial

class CourseMaterialRepository (private val courseMaterialDao: CourseMaterialDao) {
    suspend fun insertMaterial(material: CourseMaterial) {
        courseMaterialDao.insertMaterial(material)
    }

    suspend fun getMaterialById(materialId: Int): CourseMaterial? {
        return courseMaterialDao.getMaterialById(materialId)
    }

    suspend fun getMaterialsForCourse(courseId: Int): List<CourseMaterial> {
        return courseMaterialDao.getMaterialsForCourse(courseId)
    }

    suspend fun getAllMaterials(): List<CourseMaterial> {
        return courseMaterialDao.getAllMaterials()
    }

    suspend fun updateMaterial(material: CourseMaterial) {
        courseMaterialDao.updateMaterial(material)
    }

    suspend fun deleteMaterial(material: CourseMaterial) {
        courseMaterialDao.deleteMaterial(material)
    }
}
