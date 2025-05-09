    package com.example.autapp.ui.notification

    import android.content.Context
    import android.content.Intent
    import android.os.Build
    import android.provider.Settings
    import android.util.Log
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.setValue
    import android.net.Uri
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.autapp.data.models.Course
    import com.example.autapp.data.models.Notification
    import com.example.autapp.data.models.TimetableEntry
    import com.example.autapp.data.models.TimetableNotificationPreference

    import kotlinx.coroutines.launch
    import java.time.LocalDate
    import android.app.AlarmManager
    import androidx.compose.runtime.mutableIntStateOf
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
    import androidx.lifecycle.viewmodel.initializer
    import androidx.lifecycle.viewmodel.viewModelFactory
    import com.example.autapp.AUTApplication
    import com.example.autapp.data.repository.StudentRepository
    import com.example.autapp.data.datastores.SettingsDataStore
    import com.example.autapp.data.repository.CourseRepository
    import com.example.autapp.data.repository.NotificationRepository
    import com.example.autapp.data.repository.TimetableEntryRepository
    import com.example.autapp.data.repository.TimetableNotificationPreferenceRepository
    import com.example.autapp.ui.calendar.CalendarViewModel
    import com.example.autapp.util.NotificationScheduler
    import kotlinx.coroutines.flow.SharingStarted
    import kotlinx.coroutines.flow.stateIn

    const val TAG = "NotificationViewModel"
    class NotificationViewModel(
        private val studentRepository: StudentRepository,
        private val timetableEntryRepository: TimetableEntryRepository,
        private val courseRepository: CourseRepository,
        private val notificationRepository: NotificationRepository,
        private val timetableNotificationPreferenceRepository: TimetableNotificationPreferenceRepository,
        settingsDataStore: SettingsDataStore
    ) : ViewModel() {

        var studentId by mutableIntStateOf(0)
        var courses by mutableStateOf<List<Course>>(emptyList())
        var notifications by mutableStateOf<List<Notification>>(emptyList())
        var notificationPrefs by mutableStateOf<Map<Int, Int>>(emptyMap()) // classSessionId -> minutesBefore
        var errorMessage by mutableStateOf<String?>(null)
        var timetableEntries by mutableStateOf<List<TimetableEntry>>(emptyList())

        val notificationsEnabled = settingsDataStore.isNotificationsEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

        val classRemindersEnabled = settingsDataStore.isClassRemindersEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

        fun initialize(studentId: Int) {
            this.studentId = studentId
            Log.d(TAG, "Initializing ViewModel for student ID: $studentId")
            fetchNotificationData()
            loadNotificationPreferences()
        }

        private fun loadNotificationPreferences() {
            viewModelScope.launch {
                try {
                    // Assuming you have a method in your repository to get all prefs for a student
                    val prefsList = timetableNotificationPreferenceRepository.getPreferencesByStudent(studentId)
                    notificationPrefs = prefsList.associate { it.classSessionId to it.minutesBefore }
                    Log.d(TAG, "Loaded notification preferences: $notificationPrefs")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading notification preferences: ${e.message}", e)
                    errorMessage = "Error loading notification preferences: ${e.message}"
                }
            }
        }

        fun getTimetableForCourse(courseId: Int) {
           // You’ll probably want to cache this or load asynchronously in a real app
            viewModelScope.launch {
                try {
                    timetableEntries = courseRepository.getTimetableForCourse(courseId)
                } catch (e: Exception) {
                    errorMessage = "Error loading notification screen: ${e.message}"
                }
            }
        }

        fun setNotificationPreference(
            context: Context,
            studentId: Int,
            classSessionId: Int,
            courseName: String,
            minutesBefore: Int
        ) {
            viewModelScope.launch {
                try {
                    val pref = TimetableNotificationPreference(
                        studentId = studentId,
                        classSessionId = classSessionId,
                        minutesBefore = minutesBefore,
                        enabled = true
                    )

                    // Cancel any existing alarm for this class session
                    val notificationId = classSessionId.hashCode()
                    NotificationScheduler.cancelScheduledNotification(context, notificationId)
                    Log.d(TAG, "Cancelled existing alarm for classSessionId: $classSessionId before setting new one.")

                    // Save to DB
                    timetableNotificationPreferenceRepository.insertOrUpdatePreference(pref)

                    // Update local state
                    notificationPrefs = notificationPrefs.toMutableMap().apply {
                        put(classSessionId, minutesBefore)
                    }

                    // Schedule notification
                    val session = timetableEntryRepository.getTimetableEntryById(classSessionId)

                    if (session != null) {
                        val minutesBefore = pref.minutesBefore
                        val notificationText = when (minutesBefore) {
                            0 -> "Your $courseName ${session.type} at ${session.room} is starting now!"
                            60 -> "Your $courseName ${session.type} at ${session.room} is coming up in an hour!"
                            else -> "Your $courseName ${session.type} at ${session.room} is coming up in $minutesBefore minutes!"
                        }
                        NotificationScheduler.scheduleClassNotification(
                            context = context,
                            notificationId = pref.classSessionId.hashCode(),
                            title = "$courseName starts soon!",
                            text = notificationText,
                            dayOfWeek = session.dayOfWeek,
                            startTime = session.startTime,
                            deepLinkUri = "myapp://dashboard/$studentId",
                            minutesBefore = minutesBefore
                        )
                    } else {
                        errorMessage = "Class session not found."
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to set notification preference: ${e.message}"
                }
            }
        }

        fun deleteNotificationPreference(context: Context, studentId: Int, classSessionId: Int) {
            viewModelScope.launch {
                try {
                    // Cancel any existing alarm for this class session
                    val notificationId = classSessionId.hashCode()
                    NotificationScheduler.cancelScheduledNotification(context, notificationId)
                    Log.d(TAG, "Cancelled existing alarm for classSessionId: $classSessionId before setting new one.")

                    // Delete from DB
                    timetableNotificationPreferenceRepository.deletePreference(studentId, classSessionId)

                    // Update local state
                    notificationPrefs = notificationPrefs.toMutableMap().apply {
                        remove(classSessionId)
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

                    val courseWithEnrollmentInfo = studentRepository.getStudentCoursesWithEnrollmentInfo(studentId)
                    Log.d(TAG, "Fetched courses with enrollment: ${courseWithEnrollmentInfo.size}")

                    val filteredCourses = courseWithEnrollmentInfo
                        .filter { it.year == currentYear && it.semester == currentSemester }
                        .map { it.course }
                    courses = filteredCourses
                    Log.d(TAG, "Filtered courses: ${courses.map { it.name }}")

                    // Preload all timetable entries for the relevant courses
                    val allTimetableEntries = mutableListOf<TimetableEntry>()
                    for (course in filteredCourses) {
                        Log.d(TAG, "Fetching timetable for course ${course.name}")
                        val entries = courseRepository.getTimetableForCourse(course.courseId)
                        Log.d(TAG, "Entries for course ${course.name}: ${entries.map { it.courseId }}")
                        allTimetableEntries += entries
                    }
                    timetableEntries = allTimetableEntries
                    Log.d(TAG, "Total timetable entries: ${timetableEntries.size}, Entries: ${timetableEntries.map { it.courseId }}")

                    // Load notifications and preferences
                    notifications = notificationRepository.getAllNotifications()
                    Log.d(TAG, "Fetched notifications: ${notifications.size}")
                    //notificationPrefs = notificationRepository.getNotificationPreferences(studentId)
                    //Log.d(TAG, "Fetched notification preferences")

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