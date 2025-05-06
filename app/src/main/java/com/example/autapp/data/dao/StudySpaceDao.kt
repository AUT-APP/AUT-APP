package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.StudySpace

@Dao
interface StudySpaceDao {
    @Insert
    suspend fun insertStudySpace(studySpace: StudySpace)

    @Query("SELECT * FROM study_space_table")
    suspend fun getAllStudySpaces(): List<StudySpace>

    @Query("SELECT * FROM study_space_table WHERE spaceId = :spaceId LIMIT 1")
    suspend fun getStudySpaceById(spaceId: String): StudySpace?

    @Query("SELECT DISTINCT campus FROM study_space_table")
    suspend fun getCampuses(): List<String>

    @Query("SELECT DISTINCT building FROM study_space_table WHERE campus = :campus")
    suspend fun getBuildingsByCampus(campus: String): List<String>

    @Query("SELECT * FROM study_space_table WHERE campus = :campus AND building = :building")
    suspend fun getStudySpacesByCampusAndBuilding(campus: String, building: String): List<StudySpace>

    @Query("DELETE FROM study_space_table")
    suspend fun deleteAll()

    @Transaction
    suspend fun withTransaction(block: suspend () -> Unit) {
        block()
    }
}