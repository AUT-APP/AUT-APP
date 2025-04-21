package com.example.autapp.data.repository

import com.example.autapp.data.dao.StudySpaceDao
import com.example.autapp.data.models.StudySpace

class StudySpaceRepository(private val studySpaceDao: StudySpaceDao) {

    suspend fun insertStudySpace(studySpace: StudySpace) {
        studySpaceDao.insertStudySpace(studySpace)
    }

    suspend fun getAllStudySpaces(): List<StudySpace> {
        return studySpaceDao.getAllStudySpaces()
    }

    suspend fun getStudySpaceById(spaceId: String): StudySpace? {
        return studySpaceDao.getStudySpaceById(spaceId)
    }

    suspend fun getCampuses(): List<String> {
        return studySpaceDao.getCampuses()
    }

    suspend fun getBuildingsByCampus(campus: String): List<String> {
        return studySpaceDao.getBuildingsByCampus(campus)
    }

    suspend fun getStudySpacesByCampusAndBuilding(campus: String, building: String): List<StudySpace> {
        return studySpaceDao.getStudySpacesByCampusAndBuilding(campus, building)
    }

    suspend fun deleteAll() {
        studySpaceDao.deleteAll()
    }

    suspend fun withTransaction(block: suspend () -> Unit) {
        studySpaceDao.withTransaction(block)
    }
}