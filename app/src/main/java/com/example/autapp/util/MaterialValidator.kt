package com.example.autapp.util

object MaterialValidator {

    fun isValidContent(type: String, url: String): Boolean {
        val lower = url.lowercase()

        return when (type) {
            "Link" -> (lower.startsWith("http://") || lower.startsWith("https://")) &&
                      !listOf(".pdf", ".doc", ".docx", ".mp4", ".mov", ".avi", ".mkv", ".ppt", ".pptx").any { lower.endsWith(it) }
            "PDF" -> listOf(".pdf", ".doc", ".docx").any { lower.endsWith(it) } || isValidGoogleDriveLink(url)
            "Video" -> listOf(".mp4", ".mov", ".avi", ".mkv").any { lower.endsWith(it) } || url.contains("drive.google.com")
            "Slides" -> listOf(".ppt", ".pptx").any { lower.endsWith(it) } || url.contains("docs.google.com")
            else -> false
        }
    }

    private fun isValidGoogleDriveLink(url: String): Boolean {
        return url.contains("drive.google.com") ||
                url.contains("docs.google.com")
                (url.contains("/file/") || url.contains("id=")) || url.contains("/presentation/") || url.contains("/document/") || url.contains("/spreadsheets/")&&
                url.startsWith("https://")
    }

}