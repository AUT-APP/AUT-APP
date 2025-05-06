package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_table")
data class Admin(
    var firstName: String,
    var lastName: String,
    var username: String,
    var password: String,
    var role: String = "Admin",
    @PrimaryKey var adminId: Int,
    var department: String,
    var accessLevel: Int // Changed from Integer to Int for simplicity
) {
    override fun toString(): String {
        return "Admin(firstName='$firstName', lastName='$lastName', role='$role', username='$username', password='$password', " +
                "adminId=$adminId, department='$department', accessLevel=$accessLevel)"
    }
}