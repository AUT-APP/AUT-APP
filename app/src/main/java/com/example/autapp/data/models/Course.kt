package com.example.autapp.data.models

class Course(
    var name: String,
    var id: Int,
    var title: String,
    var description: String
) {
    fun getId(): Int = id
    fun getName(): String = name
    fun getTitle(): String = title
    fun getDescription(): String = description

    fun setDescription(desc: String) { description = desc }
}