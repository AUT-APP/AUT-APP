package com.example.autapp.ui.notification

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.firebase.QueryCondition
import com.example.autapp.data.firebase.QueryOperator
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.datastores.SettingsDataStore
import com.example.autapp.util.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import com.example.autapp.data.firebase.FirebaseTimetableNotificationPreference
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import com.example.autapp.data.firebase.FirebaseCourse
import com.example.autapp.data.firebase.FirebaseNotification
import com.example.autapp.data.firebase.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

const val TAG = "NotificationViewModel"
class NotificationViewModel(
    private val studentRepository: FirebaseStudentRepository,
    private val timetableEntryRepository: FirebaseTimetableRepository,
    private val courseRepository: FirebaseCourseRepository,
    private val notificationRepository: FirebaseNotificationRepository,
    private val timetableNotificationPreferenceRepository: FirebaseTimetableNotificationPreferenceRepository,
    settingsDataStore: SettingsDataStore
) : ViewModel() {

    var studentId by mutableStateOf("")
    var courses by mutableStateOf<List<FirebaseCourse>>(emptyList())
    var notifications by mutableStateOf<List<FirebaseNotification>>(emptyList())
    var notificationPrefs by mutableStateOf<Map<Int, Int>>(emptyMap()) // classSessionId (Int) -> minutesBefore (Int)
    var errorMessage by mutableStateOf<String?>(null)
    var timetableEntries by mutableStateOf<List<FirebaseTimetableEntry>>(emptyList())

    val notificationsEnabled = settingsDataStore.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val classRemindersEnabled = settingsDataStore.isClassRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Get Firebase Firestore instance
    private val notificationPreferencesCollection = FirebaseFirestore.getInstance().collection("timetableNotificationPreferences") // Assuming collection name

    fun initialize(studentId: String) {
        this.studentId = studentId
        Log.d(TAG, "Initializing ViewModel for student ID: $studentId")
        fetchNotificationData()
        loadNotificationPreferences()
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            try {
                // Use direct Firestore query
                val prefsList = notificationPreferencesCollection
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()
                    .documents.mapNotNull { it.toObject(FirebaseTimetableNotificationPreference::class.java) }
                
                notificationPrefs = prefsList.associate { it.classSessionId.toInt() to it.notificationTime }
                Log.d(TAG, "Loaded notification preferences: $notificationPrefs")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notification preferences: ${e.message}", e)
                errorMessage = "Error loading notification preferences: ${e.message}"
            }
        }
    }

    suspend fun setNotificationPreference(
        context: Context,
        studentId: String,
        classSessionId: String,
        courseName: String,
        minutesBefore: Int
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val pref = FirebaseTimetableNotificationPreference(
                studentId = studentId,
                classSessionId = classSessionId,
                notificationTime = minutesBefore,
                isEnabled = true
            )

            // Cancel any existing alarm for this class session
            val notificationId = classSessionId.hashCode()
            NotificationScheduler.cancelScheduledNotification(context, notificationId)

            // Check if preference already exists and update, otherwise create
            val existingPrefs = notificationPreferencesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("classSessionId", classSessionId)
                .get()
                .await()
                .documents.mapNotNull { it.toObject(FirebaseTimetableNotificationPreference::class.java) }

            if (existingPrefs.isNotEmpty()) {
                val existingPref = existingPrefs.first()
                // Update existing document
                notificationPreferencesCollection.document(existingPref.id)
                    .set(pref, SetOptions.merge())
                    .await()
            } else {
                // Create a new document with an auto-generated ID
                notificationPreferencesCollection.add(pref).await()
            }

            // Update local state
            notificationPrefs = notificationPrefs.toMutableMap().apply {
                put(classSessionId.toInt(), minutesBefore)
            }

            // Schedule notification
            val session = timetableEntryRepository.getById(classSessionId)

            if (session != null) {
                val minutesBefore = pref.notificationTime
                val notificationText = when (minutesBefore) {
                    0 -> "Your $courseName ${session.type} at ${session.room} is starting now!"
                    60 -> "Your $courseName ${session.type} at ${session.room} is coming up in an hour!"
                    else -> "Your $courseName ${session.type} at ${session.room} is coming up in $minutesBefore minutes!"
                }
                val scheduledTimeMillis = NotificationScheduler.scheduleClassNotification(
                    context = context,
                    notificationId = pref.classSessionId.hashCode(),
                    title = "$courseName starts soon!",
                    text = notificationText,
                    dayOfWeek = session.dayOfWeek,
                    startTime = session.startTime,
                    deepLinkUri = "myapp://dashboard/$studentId",
                    minutesBefore = minutesBefore
                )
                return@withContext scheduledTimeMillis
            } else {
                errorMessage = "Class session not found."
                return@withContext null
            }
        } catch (e: Exception) {
            errorMessage = "Failed to set notification preference: ${e.message}"
            return@withContext null
        }
    }

    fun deleteNotificationPreference(context: Context, studentId: String, classSessionId: String) {
        viewModelScope.launch {
            try {
                // Cancel any existing alarm for this class session
                val notificationId = classSessionId.hashCode()
                NotificationScheduler.cancelScheduledNotification(context, notificationId)
                Log.d(TAG, "Cancelled existing alarm for classSessionId: $classSessionId before setting new one.")

                // Delete from DB
                val conditions = listOf(
                    QueryCondition("studentId", QueryOperator.EQUAL_TO, studentId),
                    QueryCondition("classSessionId", QueryOperator.EQUAL_TO, classSessionId)
                )
                val prefs = notificationPreferencesCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("classSessionId", classSessionId)
                    .get()
                    .await()

                for (doc in prefs.documents) {
                    notificationPreferencesCollection.document(doc.id).delete().await()
                }

                // Update local state
                notificationPrefs = notificationPrefs.toMutableMap().apply {
                    remove(classSessionId.toInt())
                }
                Log.d(TAG, "Deleted notification preference for classSessionId: $classSessionId.")
            } catch (e: Exception) {
                errorMessage = "Failed to delete notification preference: ${e.message}"
            }
        }
    }

    private fun fetchNotificationData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting fetchNotificationData")
                val currentYear = LocalDate.now().year
                val currentSemester = if (LocalDate.now().monthValue in 1..6) 1 else 2
                Log.d(TAG, "Current year: $currentYear, semester: $currentSemester")

                val (student, studentCourses) = studentRepository.getStudentWithCourses(studentId)
                Log.d(TAG, "Fetched student courses: ${studentCourses.size}")

                val conditions = listOf(
                    QueryCondition("year", QueryOperator.EQUAL_TO, currentYear),
                    QueryCondition("semester", QueryOperator.EQUAL_TO, currentSemester)
                )
                val filteredCourses = courseRepository.query(conditions)
                    .filter { it.courseId in studentCourses.map { it.courseId } }
                courses = filteredCourses
                Log.d(TAG, "Filtered courses: ${courses.map { it.name }}")

                // Preload all timetable entries for the relevant courses
                val allTimetableEntries = mutableListOf<FirebaseTimetableEntry>()
                for (course in filteredCourses) {
                    Log.d(TAG, "Fetching timetable for course ${course.name}")
                    val entries = timetableEntryRepository.queryByField("courseId", course.courseId.toInt())
                    Log.d(TAG, "Entries for course ${course.name}: ${entries.map { it.courseId }}")
                    allTimetableEntries += entries
                }
                timetableEntries = allTimetableEntries
                Log.d(TAG, "Total timetable entries: ${timetableEntries.size}, Entries: ${timetableEntries.map { it.courseId }}")

                // Load notifications
                notifications = notificationRepository.getAll()
                Log.d(TAG, "Fetched notifications: ${notifications.size}")

                errorMessage = null
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchNotificationData: ${e.message}", e)
                errorMessage = "Error loading notification screen: ${e.message}"
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication
                val context = application.applicationContext
                NotificationViewModel(
                    application.studentRepository,
                    application.timetableEntryRepository,
                    application.courseRepository,
                    application.notificationRepository,
                    application.timetableNotificationPreferenceRepository,
                    SettingsDataStore(context)
                )
            }
        }
    }
}