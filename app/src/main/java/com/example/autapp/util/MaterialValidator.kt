package com.example.autapp.util

object MaterialValidator {

    fun isValidContent(type: String, url: String): Boolean {
        val lower = url.lowercase()

        return when (type) {
            "Link" -> url.startsWith("http://") || url.startsWith("https://")
            "PDF" -> listOf(".pdf", ".doc", ".docx").any { lower.endsWith(it) }
            "Video" -> listOf(".mp4", ".mov", ".avi", ".mkv").any { lower.endsWith(it) }
            "Slides" -> listOf(".ppt", ".pptx").any { lower.endsWith(it) }
            else -> false
        }
    }
}