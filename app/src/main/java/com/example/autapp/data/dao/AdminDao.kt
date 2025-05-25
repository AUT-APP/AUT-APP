package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Admin

@Dao
interface AdminDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: Admin)

    @Query("SELECT * FROM admin_table WHERE username = :username")
    suspend fun getAdminByUsername(username: String): Admin?

    @Query("SELECT * FROM admin_table WHERE adminId = :adminId")
    suspend fun getAdminById(adminId: Int): Admin?

    @Query("SELECT * FROM admin_table")
    suspend fun getAllAdmins(): List<Admin>

    @Update
    suspend fun updateAdmin(admin: Admin)
}