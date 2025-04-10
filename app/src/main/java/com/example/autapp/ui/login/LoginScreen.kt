package com.example.autapp.ui.theme

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.autapp.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun LoginScreen(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val TAG = "LoginScreen"
    Log.d(TAG, "LoginScreen composed")

    // Define colors
    val regularTeal = Color(0xFF008080) // Regular teal for other elements
    val linkBlue = Color(0xFF0466D6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars) // This accounts for the status bar
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Add spacing for camera cutout
            Spacer(modifier = Modifier.height(16.dp))

            // Top AUT Header with darker teal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(regularTeal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AUT",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.monoton_regular)),
                        color = Color.White
                    )
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Text
                Text(
                    text = "Hello!\nPlease login using your AUT credentials",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF000000)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username Input
                OutlinedTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = regularTeal,
                        unfocusedBorderColor = Color(0xFF666666)
                    )
                )

                // Password Input
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = regularTeal,
                        unfocusedBorderColor = Color(0xFF666666)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Login Button
                Button(
                    onClick = { viewModel.checkLogin() },
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = regularTeal),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        text = "LOGIN",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // Login Result Message
                viewModel.loginResult?.let {
                    Text(
                        text = it,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                            color = if (it.contains("successful")) regularTeal else Color.Red,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Forgotten Password Link
                Text(
                    text = "Forgotten password?",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        color = linkBlue,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .clickable { /* Handle forgotten password click */ }
                        .padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Security Reminder Text
                Text(
                    text = "Remember to always log out by\n" +
                          "completely exiting your browser when\n" +
                          "you leave the computer. This will protect\n" +
                          "your personal information from being\n" +
                          "accessed by subsequent users.",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom Footer with darker teal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(regularTeal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ICT SERVICE DESK | (09)921 9888",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        color = Color.White
                    )
                )
            }
        }
    }
}