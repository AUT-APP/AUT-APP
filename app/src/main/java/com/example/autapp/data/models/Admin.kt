package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_table")

class Admin(
    firstName: String,
    lastName: String,
    id: Int = 0,
    username: String,
    password: String,
    @PrimaryKey var adminID: Int,
    var department: String,
    var accessLevel: Integer
) : User(firstName, lastName, id, "Admin", username, password) {
    override fun toString(): String {
        return "Student(firstName='$firstName', lastName='$lastName', id=$id, role='$role', username='$username', password='$password', " +
                "adminID=$adminID, department='$department', accessLevel=$accessLevel)"
    }
}