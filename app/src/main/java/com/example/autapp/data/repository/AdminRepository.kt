package com.example.autapp.data.repository

import android.util.Log
import com.example.autapp.data.dao.AdminDao
import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.Admin
import com.example.autapp.data.models.User

class AdminRepository(
    private val adminDao: AdminDao,
    private val userDao: UserDao
) {
    suspend fun insertAdmin(admin: Admin) {
        // Check if user already exists
        val existingUser = userDao.getUserByUsername(admin.username)
        if (existingUser == null) {
            // Insert user if it doesn't exist
            val user = User(
                firstName = admin.firstName,
                lastName = admin.lastName,
                username = admin.username,
                password = admin.password,
                role = admin.role
            )
            userDao.insertUser(user)
            Log.d("AdminRepository", "Inserted new user: ${admin.username}")
        } else {
            Log.d("AdminRepository", "User ${admin.username} already exists, skipping user insertion")
        }

        // Insert admin
        adminDao.insertAdmin(admin)
        Log.d("AdminRepository", "Inserted admin: ${admin.firstName} ${admin.lastName}")
    }

    suspend fun getAdminByUsername(username: String): Admin? {
        return adminDao.getAdminByUsername(username)
    }

    suspend fun getAdminById(adminId: Int): Admin? {
        return adminDao.getAdminById(adminId)
    }

    suspend fun getAllAdmins(): List<Admin> {
        return adminDao.getAllAdmins()
    }

    suspend fun updateAdmin(admin: Admin) {
        adminDao.updateAdmin(admin)
        val user = userDao.getUserByUsername(admin.username)
        user?.let {
            val updatedUser = user.copy(
                firstName = admin.firstName,
                lastName = admin.lastName,
                username = admin.username,
                password = admin.password,
                role = admin.role
            )
            userDao.updateUser(updatedUser)
            Log.d("AdminRepository", "Updated user: ${admin.username}")
        } ?: Log.w("AdminRepository", "No user found for username: ${admin.username}")
    }
}