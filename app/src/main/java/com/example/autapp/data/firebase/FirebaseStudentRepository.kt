package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.autapp.data.models.StudentCourseCrossRef // Import local StudentCourseCrossRef
import android.util.Log

class FirebaseStudentRepository(
    private val courseRepository: FirebaseCourseRepository // Inject FirebaseCourseRepository
) : BaseFirebaseRepository<FirebaseStudent>("students") {
    private val userRepository = FirebaseUserRepository()
    private val studentCourseCollection = FirebaseFirestore.getInstance().collection("studentCourses")

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseStudent {
        return try {
            FirebaseStudent(
                id = documentId,
                firstName = document["firstName"] as? String ?: "",
                lastName = document["lastName"] as? String ?: "",
                username = document["username"] as? String ?: "",
                password = document["password"] as? String ?: "",
                role = document["role"] as? String ?: "Student",
                studentId = document["studentId"] as? String ?: "",
                enrollmentDate = document["enrollmentDate"] as? String ?: "",
                majorId = document["majorId"] as? String ?: "",
                minorId = document["minorId"] as? String,
                yearOfStudy = (document["yearOfStudy"] as? Number)?.toInt() ?: 0,
                gpa = (document["gpa"] as? Number)?.toDouble() ?: 0.0,
                dob = document["dob"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error mapping document $documentId: ${e.message}", e)
            throw e // Re-throw the exception to be caught by getAll()
        }
    }

    override fun objectToDocument(obj: FirebaseStudent): Map<String, Any?> {
        return mapOf(
            "firstName" to obj.firstName,
            "lastName" to obj.lastName,
            "username" to obj.username,
            "password" to obj.password,
            "role" to obj.role,
            "studentId" to obj.studentId,
            "enrollmentDate" to obj.enrollmentDate,
            "majorId" to obj.majorId,
            "minorId" to obj.minorId,
            "yearOfStudy" to obj.yearOfStudy,
            "gpa" to obj.gpa,
            "dob" to obj.dob
        )
    }

    suspend fun createStudentWithUser(
        email: String,
        password: String,
        student: FirebaseStudent,
        user: FirebaseUser
    ): String {
        return try {
            val userId = userRepository.registerUser(email, password, user.copy(role = "Student"))
            val studentWithUserId = student.copy(id = userId)
            val studentId = create(studentWithUserId)
            studentId
        } catch (e: Exception) {
            try {
                userRepository.deleteAccount()
            } catch (deleteError: Exception) {
                e.addSuppressed(deleteError)
            }
            throw FirebaseException("Error creating student", e)
        }
    }

    suspend fun getStudentByUsername(username: String): FirebaseStudent? {
        return try {
            val result = collection.whereEqualTo("username", username).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student by username", e)
        }
    }

    suspend fun getStudentByStudentId(studentId: String): FirebaseStudent? {
        return try {
            val result = collection.whereEqualTo("studentId", studentId).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student by ID", e)
        }
    }

    suspend fun getStudentsByMajor(majorId: String): List<FirebaseStudent> {
        return try {
            val result = collection.whereEqualTo("majorId", majorId).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting students by major", e)
        }
    }

    suspend fun getStudentsByMinor(minorId: String): List<FirebaseStudent> {
        return try {
            val result = collection.whereEqualTo("minorId", minorId).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting students by minor", e)
        }
    }

    suspend fun updateGPA(studentId: String, gpa: Double) {
        try {
            val student = getById(studentId) ?: throw FirebaseException("Student not found")
            update(studentId, student.copy(gpa = gpa))
        } catch (e: Exception) {
            throw FirebaseException("Error updating student GPA", e)
        }
    }

    suspend fun updateYearOfStudy(studentId: String, yearOfStudy: Int) {
        try {
            val student = getById(studentId) ?: throw FirebaseException("Student not found")
            update(studentId, student.copy(yearOfStudy = yearOfStudy))
        } catch (e: Exception) {
            throw FirebaseException("Error updating student year of study", e)
        }
    }

    suspend fun getStudentWithCourses(studentId: String): Pair<FirebaseStudent, List<FirebaseCourse>> {
        return try {
            val student = getStudentByStudentId(studentId) ?: throw FirebaseException("Student not found")
            val enrollments = getEnrollmentsByStudent(studentId)
            val courseIds = enrollments.map { it.courseId }
            val courses = courseRepository.getCoursesByIds(courseIds)
            Pair(student, courses)
        } catch (e: Exception) {
            throw FirebaseException("Error getting student with courses", e)
        }
    }

    suspend fun getEnrollmentsByStudent(studentId: String): List<FirebaseStudentCourse> {
        return try {
            Log.d("FirebaseStudentRepo", "Fetching enrollments for studentId: $studentId")
            val result = studentCourseCollection.whereEqualTo("studentId", studentId).get().await()
            Log.d("FirebaseStudentRepo", "Enrollment query result size: ${result.documents.size}")
            result.documents.mapNotNull { doc ->
                doc.toObject(FirebaseStudentCourse::class.java)
            }
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error getting student enrollments", e)
            throw FirebaseException("Error getting student enrollments", e)
        }
    }

    suspend fun enrollStudentInCourse(studentCourse: FirebaseStudentCourse) {
        try {
            Log.d("FirebaseStudentRepo", "Enrolling student ${studentCourse.studentId} in course ${studentCourse.courseId}")
            studentCourseCollection.add(studentCourse).await()
            Log.d("FirebaseStudentRepo", "Enrollment successful")
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error enrolling student in course", e)
            throw FirebaseException("Error enrolling student in course", e)
        }
    }

    // New method to delete a specific enrollment
    suspend fun deleteEnrollment(studentId: String, courseId: String, year: Int, semester: String) {
        try {
            Log.d("FirebaseStudentRepo", "Deleting enrollment: studentId=$studentId, courseId=$courseId, year=$year, semester=$semester")
            val result = studentCourseCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("year", year)
                .whereEqualTo("semester", semester)
                .get()
                .await()
            if (result.documents.isEmpty()) {
                Log.w("FirebaseStudentRepo", "No enrollment found for deletion")
                return
            }
            for (doc in result.documents) {
                studentCourseCollection.document(doc.id).delete().await()
                Log.d("FirebaseStudentRepo", "Deleted enrollment document: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error deleting enrollment", e)
            throw FirebaseException("Error deleting enrollment", e)
        }
    }

    suspend fun deleteStudent(studentId: String) {
        try {
            val student = getById(studentId) ?: throw FirebaseException("Student not found")
            deleteEnrollmentsByStudent(studentId)
            delete(studentId)
            userRepository.deleteAccount()
            Log.d("FirebaseStudentRepo", "Student deleted: $studentId")
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error deleting student", e)
            throw FirebaseException("Error deleting student", e)
        }
    }

    private suspend fun deleteEnrollmentsByStudent(studentId: String) {
        try {
            Log.d("FirebaseStudentRepo", "Deleting all enrollments for studentId: $studentId")
            val enrollments = studentCourseCollection.whereEqualTo("studentId", studentId).get().await()
            for (doc in enrollments.documents) {
                studentCourseCollection.document(doc.id).delete().await()
                Log.d("FirebaseStudentRepo", "Deleted enrollment document: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error deleting student enrollments", e)
            throw FirebaseException("Error deleting student enrollments", e)
        }
    }

    suspend fun deleteAllStudentCourseEnrollments(studentId: String) {
        try {
            Log.d("FirebaseStudentRepo", "Deleting all course enrollments for studentId: $studentId")
            val enrollments = studentCourseCollection.whereEqualTo("studentId", studentId).get().await()
            for (doc in enrollments.documents) {
                studentCourseCollection.document(doc.id).delete().await()
                Log.d("FirebaseStudentRepo", "Deleted enrollment document: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseStudentRepo", "Error deleting all student enrollments", e)
            throw FirebaseException("Error deleting all student enrollments", e)
        }
    }
}