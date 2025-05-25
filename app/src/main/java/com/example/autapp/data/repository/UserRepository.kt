package com.example.autapp.data.repository

import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.User

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun checkUser(username: String, password: String): Boolean {
        return userDao.checkUser(username, password) != null
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers()
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

}