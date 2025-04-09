package com.example.autapp.ui.theme

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import com.example.autapp.data.database.AUTDatabase
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.UserRepository

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private val loginViewModel: LoginViewModel by viewModels {
        val db = AUTDatabase.getDatabase(this)
        LoginViewModelFactory(
            UserRepository(db.userDao()),
            StudentRepository(db.studentDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        try {
            Log.d(TAG, "Database and repositories initialized via ViewModel")
            setContent {
                Log.d(TAG, "Setting content")
                AUTAPPTheme {
                    LoginScreen(loginViewModel)
                }
            }
            loginViewModel.insertTestData()

            // Log database version and tables
            val db = AUTDatabase.getDatabase(this)
            val currentVersion = db.openHelper.readableDatabase.version
            Log.d(TAG, "Database version: $currentVersion")
            db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                val tables = generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
                Log.d(TAG, "Current tables in database: $tables")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }
}

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository, studentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}