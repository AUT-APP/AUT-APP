package com.example.autapp.data.repository

import com.example.autapp.data.dao.MaterialDao
import com.example.autapp.data.models.Material

class MaterialRepository (private val materialDao: MaterialDao) {
    suspend fun insertMaterial(material: Material) {
        materialDao.insertMaterial(material)
    }

    suspend fun getMaterialById(materialId: Int): Material? {
        return materialDao.getMaterialById(materialId)
    }

    suspend fun getMaterialsByCourse(courseId: Int): List<Material> {
        return materialDao.getMaterialsByCourse(courseId)
    }

    suspend fun deleteMaterial(material: Material) {
        materialDao.deleteMaterial(material)
    }

    suspend fun updateMaterial(material: Material) {
        materialDao.updateMaterial(material)
    }

    suspend fun deleteAllMaterials() {
        materialDao.deleteAll()
    }
}
