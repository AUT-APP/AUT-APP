package com.example.autapp.ui.theme

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

@Composable
fun LoginScreen(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val TAG = "LoginScreen"
    Log.d(TAG, "LoginScreen composed")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (Teal bar with "AUT")
        Box(
            modifier = Modifier
                .width(284.dp)
                .height(53.dp)
                .background(Color(0xFF008080)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AUT",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.monoton_regular)),
                    fontWeight = FontWeight(400),
                    color = Color(0xFFFFFFFF),
                    textAlign = TextAlign.Center
                )
            )
        }

        // Main Login Container
        Box(
            modifier = Modifier
                .width(275.dp)
                .height(594.dp)
                .background(Color(0xFFFFFFFF))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // "AUT Login" Title
                Text(
                    text = "AUT Login",
                    style = TextStyle(
                        fontSize = 25.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF008080),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .width(131.dp)
                        .height(32.dp)
                )

                // Instruction Text
                Text(
                    text = "Please login with your AUT Username and Password.",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight(700),
                        color = Color(0xFF000000)
                    ),
                    modifier = Modifier.width(234.dp)
                )

                // Username Label
                Text(
                    text = "Username",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF000000),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.width(57.dp)
                )

                // Username Input
                OutlinedTextField(
                    value = viewModel.username,  // Binds to observable state
                    onValueChange = { viewModel.updateUsername(it) },  // Updates state
                    modifier = Modifier
                        .width(200.dp)
                        .height(56.dp)
                        .border(1.dp, Color(0x80000000))
                        .background(Color(0xFFFFFFFF)),
                    textStyle = TextStyle(fontSize = 16.sp),
                    singleLine = true
                )

                // Password Label
                Text(
                    text = "Password",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF000000)
                    ),
                    modifier = Modifier.width(53.dp)
                )

                // Password Input
                OutlinedTextField(
                    value = viewModel.password,  // Binds to observable state
                    onValueChange = { viewModel.updatePassword(it) },  // Updates state
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .width(200.dp)
                        .height(56.dp)
                        .border(1.dp, Color(0x80000000))
                        .background(Color(0xFFFFFFFF)),
                    textStyle = TextStyle(fontSize = 16.sp),
                    singleLine = true
                )

                // Login Button
                Button(
                    onClick = {
                        Log.d(TAG, "Login button clicked")
                        viewModel.checkLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080)),
                    modifier = Modifier
                        .width(84.dp)
                        .height(21.dp)
                ) {
                    Text(
                        text = "LOGIN",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.molengo_regular)),
                            fontWeight = FontWeight(400),
                            color = Color(0xFFFFFFFF),
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Login Result
                viewModel.loginResult?.let {
                    Text(
                        text = it,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                            color = if (it.contains("successful")) Color.Green else Color.Red
                        )
                    )
                }

                // Forgotten Password Link
                Text(
                    text = "Forgotten password?",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF0466D6),
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.width(113.dp)
                )

                // Disclaimer Text
                Text(
                    text = "Remember to always log out by\ncompletely exiting your browser when \nyou leave the computer. This will protect\nyour personal information from being \naccessed by subsequent users.",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF000000)
                    ),
                    modifier = Modifier.width(216.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Push footer to bottom

        // Footer (Teal bar with ICT Service Desk info)
        Box(
            modifier = Modifier
                .width(284.dp)
                .height(84.dp)
                .background(Color(0xFF008080)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = " ICT SERVICE DESK | (09)921 9888 \n | https://ithelp.aut.ac.nz",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_variablefont_wght)),
                    fontWeight = FontWeight(400),
                    color = Color(0xFFFFFFFF)
                )
            )
        }
    }
}