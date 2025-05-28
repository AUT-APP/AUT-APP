package com.example.autapp.data.models
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class User(
    var firstName: String,
    var lastName: String,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var role: String,
    var username: String,
    var password: String,
    var isFirstLogin: Boolean = true // Tracks if user needs to change password
) {
    override fun toString(): String {
        return "User(firstName='$firstName', lastName='$lastName', id=$id, role='$role', username='$username', password='$password')"
    }
}

