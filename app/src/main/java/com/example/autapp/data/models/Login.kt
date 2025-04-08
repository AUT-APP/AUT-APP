package com.example.autapp.data.models

class Login(
    private var id: String,
    private var name: String,
    private var email: String,
    private var password: String
) {
    fun getId(): String = id
    fun getName(): String = name
    fun getEmail(): String = email
    fun getPassword(): String = password

    fun setId(newId: String) {id = newId}
    fun setName(newName: String) {name = newName}
    fun setEmail(newEmail: String) {email = newEmail}
    fun setPassword(newPassword: String) {password = newPassword}
    fun checkLogin(inputId: String, inputPassword: String): Boolean {return id == inputId && password == inputPassword}
}