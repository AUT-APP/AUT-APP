package com.example.autapp.ui.material

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.foundation.layout.PaddingValues
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import com.example.autapp.util.MaterialValidator
import org.junit.Assert.*
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(AndroidJUnit4::class)
class CourseMaterialScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLinkShouldNotAcceptFileExtensions() {
        // Given
        val linkWithPdf = "https://example.com/file.pdf"
        val linkWithMp4 = "https://example.com/video.mp4"

        // When
        val isPdfLinkValid = MaterialValidator.isValidContent("Link", linkWithPdf)
        val isMp4LinkValid = MaterialValidator.isValidContent("Link", linkWithMp4)

        // Then
        assertFalse("Links ending with .pdf should not be valid for type Link", isPdfLinkValid)
        assertFalse("Links ending with .mp4 should not be valid for type Link", isMp4LinkValid)
    }

    @Test
    fun noMaterialsMessage() {
        // Given
        val materials = emptyList<String>()

        // When
        val message = getNoMaterialsMessage(materials)

        // Then 
        assertEquals("No materials available for this course.", message)
    }

    @Test
    fun notificationSystem_shouldFail() {
        // Given
        val isTeacher = true
        val materialUploaded = true

        // When
        val result = sendNotification(isTeacher, materialUploaded)

        // Then
        assertEquals("Notification sent", result)
    }

    fun sendNotification(isTeacher: Boolean, materialUploaded: Boolean): String {
        return if (isTeacher && materialUploaded) "Notification sent" else "No notification"
    }

    fun getNoMaterialsMessage(materials: List<String>): String {
        return if (materials.isEmpty()) "No materials available for this course." else "Materials found."
    }




}
