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

    /**
     * Create a new student with user account
     */
    suspend fun createStudentWithUser(
        email: String,
        password: String,
        student: FirebaseStudent,
        user: FirebaseUser
    ): String {
        return try {
            // Create user account first
            val userId = userRepository.registerUser(email, password, user.copy(role = "Student"))
            
            // Create student document
            val studentWithUserId = student.copy(id = userId)
            val studentId = create(studentWithUserId)
            
            studentId
        } catch (e: Exception) {
            // If student creation fails, delete the user account
            try {
                userRepository.deleteAccount()
            } catch (deleteError: Exception) {
                // Log the error but throw the original exception
                e.addSuppressed(deleteError)
            }
            throw FirebaseException("Error creating student", e)
        }
    }

    /**
     * Get student by username
     */
    suspend fun getStudentByUsername(username: String): FirebaseStudent? {
        return try {
            val result = collection.whereEqualTo("username", username).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student by username", e)
        }
    }

    /**
     * Get student by student ID
     */
    suspend fun getStudentByStudentId(studentId: String): FirebaseStudent? {
        return try {
            val result = collection.whereEqualTo("studentId", studentId).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student by ID", e)
        }
    }

    /**
     * Get all students in a major department
     */
    suspend fun getStudentsByMajor(majorId: String): List<FirebaseStudent> {
        return try {
            val result = collection.whereEqualTo("majorId", majorId).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting students by major", e)
        }
    }

    /**
     * Get all students in a minor department
     */
    suspend fun getStudentsByMinor(minorId: String): List<FirebaseStudent> {
        return try {
            val result = collection.whereEqualTo("minorId", minorId).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting students by minor", e)
        }
    }

    /**
     * Update student GPA
     */
    suspend fun updateGPA(studentId: String, gpa: Double) {
        try {
            val student = getById(studentId) ?: throw FirebaseException("Student not found")
            update(studentId, student.copy(gpa = gpa))
        } catch (e: Exception) {
            throw FirebaseException("Error updating student GPA", e)
        }
    }

    /**
     * Update student year of study
     */
    suspend fun updateYearOfStudy(studentId: String, yearOfStudy: Int) {
        try {
            val student = getById(studentId) ?: throw FirebaseException("Student not found")
            update(studentId, student.copy(yearOfStudy = yearOfStudy))
        } catch (e: Exception) {
            throw FirebaseException("Error updating student year of study", e)
        }
    }

    /**
     * Get student with their enrolled courses
     */
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

    /**
     * Get student course enrollments by student ID
     */
    suspend fun getEnrollmentsByStudent(studentId: String): List<FirebaseStudentCourse> {
        return try {
            Log.d("FirebaseStudentRepo", "Fetching enrollments for studentId: $studentId")
            val result = studentCourseCollection.whereEqualTo("studentId", studentId).get().await()
            Log.d("FirebaseStudentRepo", "Enrollment query result size: ${result.documents.size}")
            result.documents.mapNotNull { doc ->
                doc.toObject(FirebaseStudentCourse::class.java)
            }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student enrollments", e)
        }
    }

    /**
     * Enroll student in a course
     */
    suspend fun enrollStudentInCourse(studentCourse: FirebaseStudentCourse) {
        try {
            studentCourseCollection.add(studentCourse).await()
        } catch (e: Exception) {
            throw FirebaseException("Error enrolling student in course", e)
        }
    }

    /**
     * Delete student and associated data
     */
    suspend fun deleteStudent(studentId: String) {
        try {
            val student = getById(studentId) ?: throw FirebaseException("Student not found")
            
            // Delete student's course enrollments
            deleteEnrollmentsByStudent(studentId)
            
            // Delete student document
            delete(studentId)
            
            // Delete associated user account
            userRepository.deleteAccount()
        } catch (e: Exception) {
            throw FirebaseException("Error deleting student", e)
        }
    }

    /**
     * Delete student course enrollments by student ID
     */
    private suspend fun deleteEnrollmentsByStudent(studentId: String) {
        try {
            val enrollments = studentCourseCollection.whereEqualTo("studentId", studentId).get().await()
            for (doc in enrollments.documents) {
                studentCourseCollection.document(doc.id).delete().await()
            }
        } catch (e: Exception) {
            throw FirebaseException("Error deleting student enrollments", e)
        }
    }

    /**
     * Delete all course enrollments for a student
     */
    suspend fun deleteAllStudentCourseEnrollments(studentId: String) {
        try {
            val enrollments = studentCourseCollection.whereEqualTo("studentId", studentId).get().await()
            for (doc in enrollments.documents) {
                studentCourseCollection.document(doc.id).delete().await()
            }
        } catch (e: Exception) {
            throw FirebaseException("Error deleting all student enrollments", e)
        }
    }
} 