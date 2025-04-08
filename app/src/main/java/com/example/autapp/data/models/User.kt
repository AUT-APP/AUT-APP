package com.example.autapp.com.example.autapp.data.models

open class User(
    var firstName: String,
    var lastName: String,
    var id: Int,
    var role: String,
    var username: String,
    var password: String
) {
    fun getFirstName(): String = firstName
    fun getLastName(): String = lastName
    fun setFirstName(name: String) { firstName = name }
    fun setLastName(name: String) { lastName = name }

    fun getId(): Int = id
    fun setId(id: Int) { this.id = id }

    fun getRole(): String = role
    fun setRole(role: String) { this.role = role }

    fun getUsername(): String = username
    fun getPassword(): String = password
    fun setPassword(password: String) { this.password = password }
}