package com.example.autapp.ui.login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.autapp.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Int, String) -> Unit,
    navController: NavController,
    isDarkTheme: Boolean
) {
    val TAG = "LoginScreen"
    Log.d(TAG, "LoginScreen composed")

    val username by remember { derivedStateOf { viewModel.username } }
    val password by remember { derivedStateOf { viewModel.password } }
    val loginResult by remember { derivedStateOf { viewModel.loginResult } }
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    // Show snackbar for login errors
    LaunchedEffect(loginResult) {
        loginResult?.let {
            if (it.contains("error", ignoreCase = true)) {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AUT",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.monoton_regular)),
                        color = colorScheme.onPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hello!\nPlease login using your AUT credentials",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                    textAlign = TextAlign.Center,
                    color = colorScheme.onBackground
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        cursorColor = colorScheme.primary,
                        focusedLabelColor = colorScheme.primary,
                        unfocusedLabelColor = colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        cursorColor = colorScheme.primary,
                        focusedLabelColor = colorScheme.primary,
                        unfocusedLabelColor = colorScheme.onSurface
                    )
                )

                Button(
                    onClick = {
                        viewModel.login(
                            onSuccess = { userId, role, isFirstLogin, _ ->
                                if (isFirstLogin && role != "Admin") {
                                    navController.navigate("change_password/$username/$role/$userId")
                                } else {
                                    onLoginSuccess(userId, role)
                                }
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Login failed: $error")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        text = "LOGIN",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                }

                Text(
                    text = "Forgotten password?",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                    color = Color(0xFF0466D6),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { /* Handle forgotten password click */ }
                        .padding(vertical = 8.dp)
                )

                Text(
                    text = "Remember to always log out by\n" +
                            "completely exiting your browser when\n" +
                            "you leave the computer. This will protect\n" +
                            "your personal information from being\n" +
                            "accessed by subsequent users.",
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ICT SERVICE DESK | (09)921 9888",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                    color = colorScheme.onPrimary
                )
            }
        }
    }
}