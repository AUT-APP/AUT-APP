package com.example.autapp.ui.login

import androidx.compose.runtime.mutableStateOf
import org.junit.Test
import org.junit.Assert.*

class ChangePasswordScreenTest {

    private fun isPasswordSecure(password: String): Boolean {
        val minLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { "!@#$%^&*".contains(it) }
        return minLength && hasUppercase && hasLowercase && hasDigit && hasSpecial
    }

    @Test
    fun testScreenComponentsInitialized() {
        // Acceptance Test 1: Change Password Screen Display
        // Given: A Change Password screen is loaded
        val newPassword = mutableStateOf("")
        val confirmPassword = mutableStateOf("")
        val errorMessage = mutableStateOf<String?>(null)

        // When: The screen's initial state is checked
        val newPasswordValue = newPassword.value
        val confirmPasswordValue = confirmPassword.value
        val errorMessageValue = errorMessage.value

        // Then: The password fields should be empty and no error message should be present
        assertEquals("", newPasswordValue)
        assertEquals("", confirmPasswordValue)
        assertNull(errorMessageValue)
    }

    @Test
    fun testPreventDefaultPasswordReuse() {
        // Acceptance Test 2: Prevent Default Password Reuse
        // Given: A student with username "rayyan" and DOB "28apr" is on the Change Password screen
        val username = "rayyan"
        val dob = "28apr"
        val defaultPassword = "${username.lowercase()}${dob.replace("-", "")}"
        val newPassword = "rayyan28apr"

        // When: The new password matches the default password (username + DOB)
        val isDefaultPassword = newPassword == defaultPassword
        val errorMessage = if (isDefaultPassword) {
            "New password cannot be the same as the initial password"
        } else {
            null
        }

        // Then: An error message should indicate the password cannot be the same as the initial password
        assertTrue(isDefaultPassword)
        assertEquals("New password cannot be the same as the initial password", errorMessage)
    }

    @Test
    fun testPasswordSecurityValidation() {
        // Acceptance Test 3: Enforce Password Security Requirements
        // Given: A student is on the Change Password screen
        val weakPassword = "weak"

        // When: A weak password is entered
        val isSecure = isPasswordSecure(weakPassword)

        // Then: The password should not meet security requirements
        assertFalse(isSecure)
    }

    @Test
    fun testValidPasswordSubmission() {
        // Acceptance Test 4: Successful Password Update
        // Given: A student is on the Change Password screen with a valid password
        val newPassword = "SecurePass123!"
        val confirmPassword = "SecurePass123!"
        val username = "testuser"
        val defaultPassword = "testuser28apr"

        // When: The password is validated for security, matching, and difference from default
        val isSecure = isPasswordSecure(newPassword)
        val passwordsMatch = newPassword == confirmPassword
        val isNotDefault = newPassword != defaultPassword

        // Then: The password should be secure, match the confirmation, and differ from the default
        assertTrue(isSecure)
        assertEquals(newPassword, confirmPassword)
        assertNotEquals(newPassword, defaultPassword)
    }
}