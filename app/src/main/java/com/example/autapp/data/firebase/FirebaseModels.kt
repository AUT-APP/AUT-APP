package com.example.autapp.data.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import androidx.core.app.NotificationCompat
import com.example.autapp.data.models.StudySpace
import com.example.autapp.data.models.Booking
import com.example.autapp.data.models.Notification
import java.util.UUID

// Base user model for authentication
data class FirebaseUser(
    var firstName: String = "",
    var lastName: String = "",
    @DocumentId
    var id: String = "",
    var role: String = "",
    var username: String = "", // This will now store the full email for Firebase Auth
    var password: String = "",
    var isFirstLogin: Boolean = true // Tracks if user needs to change password
) {
    override fun toString(): String {
        return "User(firstName='$firstName', lastName='$lastName', id=$id, role='$role', username='$username', password='$password')"
    }
}

// Student model
data class FirebaseStudent(
    var id: String = "", // Matches User id
    var firstName: String = "",
    var lastName: String = "",
    var username: String = "", // This will now store the full email for Firebase Auth
    var password: String = "",
    var role: String = "Student",
    @DocumentId
    var studentId: String = "",
    var enrollmentDate: String = "",
    var majorId: String = "", // References Department entity with type "Major"
    var minorId: String? = null, // Optional, references Department entity with type "Minor"
    var yearOfStudy: Int = 0,
    var gpa: Double = 0.0,
    var dob: String = ""
) {
    override fun toString(): String {
        return "Student(id=$id, firstName='$firstName', lastName='$lastName', role='$role', username='$username', password='$password', " +
                "studentId=$studentId, enrollmentDate='$enrollmentDate', majorId=$majorId, minorId=$minorId, yearOfStudy=$yearOfStudy, gpa=$gpa, dob='$dob')"
    }
}

// Teacher model
data class FirebaseTeacher(
    var id: String = "", // Matches User id
    var firstName: String = "",
    var lastName: String = "",
    var username: String = "",
    var password: String = "",
    @DocumentId
    var teacherId: String = "",
    var departmentId: String = "", // References Department entity with type "Department"
    var role: String = "Teacher",
    var title: String = "",
    var officeNumber: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var officeHours: String = "",
    var courses: List<String> = emptyList(),
    var dob: String = ""
) {
    override fun toString(): String {
        return "Teacher(id=$id, firstName='$firstName', lastName='$lastName', username='$username', password='$password', " +
                "teacherId=$teacherId, departmentId=$departmentId, title='$title', officeNumber='$officeNumber', " +
                "email='$email', phoneNumber='$phoneNumber', officeHours='$officeHours', courses=$courses, dob='$dob')"
    }
}

// Admin model
data class FirebaseAdmin(
    var firstName: String = "",
    var lastName: String = "",
    var username: String = "",
    var password: String = "",
    var role: String = "Admin",
    @DocumentId
    var adminId: String = "",
    var department: String = "",
    var accessLevel: Int = 0
) {
    override fun toString(): String {
        return "Admin(firstName='$firstName', lastName='$lastName', role='$role', username='$username', password='$password', " +
                "adminId=$adminId, department='$department', accessLevel=$accessLevel)"
    }
}

// Course model
data class FirebaseCourse(
    @DocumentId
    var courseId: String = "",
    var name: String = "",
    var title: String = "",
    var description: String = "",
    var objectives: String = "",
    var location: String? = null,
    var teacherId: String = "", // Reference to FirebaseTeacher
    var departmentId: String = "" // Reference to FirebaseDepartment
) {
    fun updateCourseDescription(title: String, description: String, objectives: String) {
        this.title = title
        this.description = description
        this.objectives = objectives
    }
}

// Student-Course relationship
data class FirebaseStudentCourse(
    @DocumentId
    val id: String = "",
    val studentId: String = "", // Reference to FirebaseStudent
    val courseId: String = "", // Reference to FirebaseCourse
    val year: Int = 0,
    val semester: String = "", // e.g., "S1", "S2"
    val status: String = "ENROLLED", // ENROLLED, COMPLETED, DROPPED
    val enrollmentDate: Date = Date()
)

// Assignment model
data class FirebaseAssignment(
    @DocumentId
    var assignmentId: String = "",
    var name: String = "",
    var location: String = "",
    var due: Date = Date(),
    var weight: Double = 0.0,
    var maxScore: Double = 0.0,
    var type: String = "",
    var courseId: String = "" // Reference to FirebaseCourse
) {
    override fun toString(): String {
        return "Assignment(assignmentId=$assignmentId, name='$name', location='$location', due=$due, weight=$weight, maxScore=$maxScore, type='$type', courseId=$courseId)"
    }
}

// Grade model
data class FirebaseGrade(
    @DocumentId
    var gradeId: String = "",
    var assignmentId: String = "", // Reference to FirebaseAssignment
    var studentId: String = "", // Reference to FirebaseStudent
    private var _score: Double = 0.0,
    var grade: String = "",
    var feedback: String? = null
) {
    enum class GradeValue(val minScore: Double, val maxScore: Double, val description: String, val numericValue: Int) {
        A_PLUS(89.50, 100.00, "Pass with High Distinction", 9),
        A(84.50, 89.49, "Pass with Clear Distinction", 8),
        A_MINUS(79.50, 84.49, "Pass with Distinction", 7),
        B_PLUS(74.50, 79.49, "Pass with High Merit", 6),
        B(69.50, 74.49, "Pass with Clear Merit", 5),
        B_MINUS(64.50, 69.49, "Pass with Merit", 4),
        C_PLUS(59.50, 64.49, "High Pass", 3),
        C(54.50, 59.49, "Clear Pass", 2),
        C_MINUS(49.50, 54.49, "Pass", 1),
        D(0.00, 49.49, "Specified Fail", 0);

        override fun toString(): String {
            return when (this) {
                A_PLUS -> "A+"
                A -> "A"
                A_MINUS -> "A-"
                B_PLUS -> "B+"
                B -> "B"
                B_MINUS -> "B-"
                C_PLUS -> "C+"
                C -> "C"
                C_MINUS -> "C-"
                D -> "D"
            }
        }
    }

    var score: Double
        get() = _score
        set(value) {
            require(value in 0.00..100.00) { "Score must be between 0.00 and 100.00" }
            _score = value
            grade = calculateGrade(value).toString()
        }

    init {
        require(_score in 0.00..100.00) { "Score must be between 0.00 and 100.00" }
        grade = calculateGrade(_score).toString()
    }

    private fun calculateGrade(score: Double): GradeValue {
        return GradeValue.entries.firstOrNull { score in it.minScore..it.maxScore } ?: GradeValue.D
    }

    fun getNumericValue(): Int {
        val gradeValue = GradeValue.entries.firstOrNull { it.toString() == grade } ?: GradeValue.D
        return gradeValue.numericValue
    }

    override fun toString(): String {
        return "Grade(gradeId=$gradeId, assignmentId=$assignmentId, studentId=$studentId, score=$score, grade='$grade', feedback='$feedback')"
    }

    fun getGradeDescription(): String {
        val gradeValue = GradeValue.entries.firstOrNull { it.toString() == grade } ?: GradeValue.D
        return gradeValue.description
    }
}

// Timetable Entry model
data class FirebaseTimetableEntry(
    @DocumentId
    val entryId: String = "",
    var courseId: String = "",
    var dayOfWeek: Int,
    var startTime: Date,
    var endTime: Date,
    var room: String,
    var type: String // Lecture, Lab, Tutorial, etc.
)

// Timetable Notification Preference
data class FirebaseTimetableNotificationPreference(
    @DocumentId
    val id: String = "",
    val studentId: String? = null, // Reference to FirebaseStudent
    val teacherId: String? = null, // Reference to FirebaseTeacher
    val isTeacher: Boolean = false,
    val classSessionId: String = "", // Reference to FirebaseTimetableEntry
    val notificationTime: Int = 15, // Minutes before class
    val isEnabled: Boolean = true
)

// Timetable Booking Preference
data class FirebaseBookingNotificationPreference(
    @DocumentId
    val id: String = "",
    val studentId: String = "", // Reference to FirebaseStudent
    val teacherId: String? = "", // Reference to FirebaseTeacher
    val isTeacher: Boolean = false,
    val bookingId: String = "", // Reference to FirebaseBooking
    val notificationTime: Int = 15, // Minutes before class
    val isEnabled: Boolean = true
)


// Timetable Event Preference
data class FirebaseEventNotificationPreference(
    @DocumentId
    val id: String = "",
    val studentId: String = "", // Reference to FirebaseStudent
    val eventId: String = "", // Reference to FirebaseEvent
    val notificationTime: Int = 15, // Minutes before class
    val isEnabled: Boolean = true
)


// Event model
data class FirebaseEvent(
    @DocumentId
    var eventId: String = "",
    var title: String = "",
    var date: Date = Date(),
    var startTime: Date? = null,
    var endTime: Date? = null,
    var location: String? = null,
    var details: String? = null,
    var isToDoList: Boolean = false, // true for todo list items, false for regular events
    var frequency: String? = null, // "Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"
    var studentId: String = "", // the id of the student who created the event
    var teacherId: String? = null, // the id of the teacher who created the event
    var isTeacherEvent: Boolean = false // true if the event is a teacher event, false otherwise
) {
    fun toEvent(): com.example.autapp.data.models.Event {
        return com.example.autapp.data.models.Event(
            eventId = eventId,
            title = title,
            date = date,
            startTime = startTime,
            endTime = endTime,
            location = location,
            details = details,
            isToDoList = isToDoList,
            frequency = frequency,
            studentId = studentId,
            teacherId = teacherId,
            isTeacherEvent = isTeacherEvent
        )
    }
}

// Booking model
data class FirebaseBooking(
    @DocumentId
    val id: String = "",
    val studentId: String = "", // Reference to FirebaseStudent
    val roomId: String = "", // Reference to FirebaseStudySpace document ID
    val building: String = "",
    val campus: String = "",
    val level: String = "",
    val bookingDate: Date = Date(),
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val status: String = "ACTIVE", // ACTIVE, CANCELLED, COMPLETED
    @ServerTimestamp
    val createdAt: Date? = null
) {
    fun toBooking(): Booking {
        return Booking(
            bookingId = 0, // Assuming bookingId is not stored in FirebaseBooking, or is generated client-side
            studentId = studentId, // Keep as String
            roomId = roomId, // This will be the StudySpace document ID
            building = building,
            campus = campus,
            level = level,
            bookingDate = bookingDate,
            startTime = startTime,
            endTime = endTime,
            status = status,
            documentId = id // Map FirebaseBooking's document ID to Booking's documentId
        )
    }

    fun fromBooking(booking: Booking): FirebaseBooking {
        return FirebaseBooking(
            id = booking.documentId ?: "", // Use the documentId from Booking if available
            studentId = booking.studentId, // Keep as String
            roomId = booking.roomId, // This should be the StudySpace document ID
            building = booking.building,
            campus = booking.campus,
            level = booking.level,
            bookingDate = booking.bookingDate,
            startTime = booking.startTime,
            endTime = booking.endTime,
            status = booking.status
        )
    }
}

// Study Space model
data class FirebaseStudySpace(
    @DocumentId // Add DocumentId annotation
    val documentId: String = "", // Field to hold the Firestore document ID
    val spaceId: String = "", // Field to hold the human-readable name (e.g., "Room 4")
    val building: String = "",
    val campus: String = "",
    val level: String = "",
    val capacity: Int = 0,
    val isAvailable: Boolean = false
) {
    fun toStudySpace(): StudySpace {
        return StudySpace(
            documentId = documentId, // Map documentId
            spaceId = spaceId, // Map human-readable spaceId
            building = building,
            campus = campus,
            level = level,
            capacity = capacity,
            isAvailable = isAvailable
        )
    }
}

// Notification model
data class FirebaseNotification(
    @DocumentId
    val notificationId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val isTeacher: Boolean = false,
    val notificationType: String = "",
    val relatedItemId: String = "",
    val scheduledDeliveryTime: Date = Date(),
    val title: String = "",
    val text: String = "",
    var priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    var deepLinkUri: String? = null,
    var channelId: String,
    var timestamp: Long = System.currentTimeMillis()
)

// Department model
data class FirebaseDepartment(
    @DocumentId
    var departmentId: String = "",
    var name: String = "",
    var type: String = "", // "Department", "Major", or "Minor"
    var description: String? = null
)

// Activity Log model
data class FirebaseActivityLog(
    val id: String = "",
    val description: String,
    val timestamp: Long
)

// Chat Message model
data class FirebaseChatMessage(
    @DocumentId
    val id: String = "",
    val senderId: String = "", // Reference to FirebaseUser
    val receiverId: String = "", // Reference to FirebaseUser
    val message: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false,
    val type: String = "TEXT" // TEXT, IMAGE, FILE
)

// Extension function to convert FirebaseNotification to Notification
fun FirebaseNotification.toNotification(): Notification {
    return Notification(
        notificationId = this.notificationId,
        iconResId = this.iconResId,
        title = this.title,
        text = this.text,
        priority = this.priority,
        deepLinkUri = this.deepLinkUri,
        channelId = this.channelId,
        timestamp = this.timestamp
    )
}