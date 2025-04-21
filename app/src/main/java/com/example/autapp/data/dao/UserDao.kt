package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM user_table WHERE username = :username AND password = :password")
    suspend fun checkUser(username: String, password: String): User?

    @Query("SELECT * FROM user_table WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>

    @Delete
    suspend fun deleteUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM user_table")
    suspend fun deleteAll()
}