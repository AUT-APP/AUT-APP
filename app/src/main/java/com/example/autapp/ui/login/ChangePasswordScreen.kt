package com.example.autapp.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.autapp.ui.components.AUTTopAppBar

@Composable
fun ChangePasswordScreen(
    viewModel: LoginViewModel,
    navController: NavController,
    username: String,
    role: String,
    userId: String,
    isDarkTheme: Boolean,
    onPasswordChanged: () -> Unit
) {
    val newPassword = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    fun isPasswordSecure(password: String): Boolean {
        val minLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { "!@#$%^&*".contains(it) }
        return minLength && hasUppercase && hasLowercase && hasDigit && hasSpecial
    }

    Scaffold(
        topBar = {
            AUTTopAppBar(
                title = "Change Password",
                isDarkTheme = isDarkTheme,
                navController = navController,
                showBackButton = true,
                currentRoute = "change_password/$username/$role/$userId",
                currentUserId = userId,
                isTeacher = role == "Teacher",
                currentUserRole = role
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Please set a new secure password",
                fontSize = 16.sp,
                color = colorScheme.onBackground
            )

            OutlinedTextField(
                value = newPassword.value,
                onValueChange = { newPassword.value = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    cursorColor = colorScheme.primary,
                    focusedLabelColor = colorScheme.primary,
                    unfocusedLabelColor = colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    cursorColor = colorScheme.primary,
                    focusedLabelColor = colorScheme.primary,
                    unfocusedLabelColor = colorScheme.onSurface
                )
            )

            Text(
                text = "Password must be at least 8 characters, include uppercase, lowercase, digit, and special character (!@#$%^&*).",
                fontSize = 12.sp,
                color = colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            errorMessage.value?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Button(
                onClick = {
                    when {
                        newPassword.value != confirmPassword.value -> {
                            errorMessage.value = "Passwords do not match"
                        }
                        !isPasswordSecure(newPassword.value) -> {
                            errorMessage.value = "Password does not meet security requirements"
                        }
                        newPassword.value == "${username.lowercase()}${viewModel.dob?.replace("-", "") ?: ""}" -> {
                            errorMessage.value = "New password cannot be the same as the initial password"
                        }
                        else -> {
                            viewModel.updateUserPassword(
                                username = username,
                                newPassword = newPassword.value,
                                onSuccess = {
                                    onPasswordChanged()
                                    when (role) {
                                        "Admin" -> navController.navigate("admin_dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                        "Student" -> navController.navigate("dashboard/$userId") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                        "Teacher" -> navController.navigate("dashboard/$userId") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                        else -> {
                                            errorMessage.value = "Invalid role"
                                        }
                                    }
                                },
                                onFailure = { error ->
                                    errorMessage.value = error
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .width(160.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Text(
                    text = "SUBMIT",
                    fontSize = 16.sp,
                    color = colorScheme.onPrimary
                )
            }
        }
    }
}