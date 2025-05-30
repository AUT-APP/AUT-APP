package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTeacherRepository : BaseFirebaseRepository<FirebaseTeacher>("teachers") {
    private val userRepository = FirebaseUserRepository()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseTeacher {
        return FirebaseTeacher(
            id = document["id"] as? String ?: "",
            firstName = document["firstName"] as? String ?: "",
            lastName = document["lastName"] as? String ?: "",
            username = document["username"] as? String ?: "",
            password = document["password"] as? String ?: "",
            teacherId = documentId,
            departmentId = document["departmentId"] as? String ?: "",
            role = document["role"] as? String ?: "Teacher",
            title = document["title"] as? String ?: "",
            officeNumber = document["officeNumber"] as? String ?: "",
            email = document["email"] as? String ?: "",
            phoneNumber = document["phoneNumber"] as? String ?: "",
            officeHours = document["officeHours"] as? String ?: "",
            courses = (document["courses"] as? List<String>)?.toMutableList() ?: mutableListOf(),
            dob = document["dob"] as? String ?: ""
        )
    }

    override fun objectToDocument(obj: FirebaseTeacher): Map<String, Any?> {
        return mapOf(
            "id" to obj.id,
            "firstName" to obj.firstName,
            "lastName" to obj.lastName,
            "username" to obj.username,
            "password" to obj.password,
            "departmentId" to obj.departmentId,
            "role" to obj.role,
            "title" to obj.title,
            "officeNumber" to obj.officeNumber,
            "email" to obj.email,
            "phoneNumber" to obj.phoneNumber,
            "officeHours" to obj.officeHours,
            "courses" to obj.courses,
            "dob" to obj.dob
        )
    }

    /**
     * Create a new teacher with user account
     */
    suspend fun createTeacherWithUser(
        email: String,
        password: String,
        teacher: FirebaseTeacher,
        user: FirebaseUser
    ): String {
        return try {
            // Create user account first
            val userId = userRepository.registerUser(email, password, user.copy(role = "Teacher"))
            
            // Create teacher document
            val teacherWithId = teacher.copy(teacherId = userId)
            val teacherId = create(teacherWithId)
            
            teacherId
        } catch (e: Exception) {
            // If teacher creation fails, delete the user account
            try {
                userRepository.deleteAccount()
            } catch (deleteError: Exception) {
                // Log the error but throw the original exception
                e.addSuppressed(deleteError)
            }
            throw FirebaseException("Error creating teacher", e)
        }
    }

    /**
     * Get teacher by teacher ID
     */
    suspend fun getTeacherByTeacherId(teacherId: String): FirebaseTeacher? {
        return try {
            val result = collection.whereEqualTo("teacherId", teacherId).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting teacher by ID", e)
        }
    }

    /**
     * Get all teachers in a department
     */
    suspend fun getTeachersByDepartment(departmentId: String): List<FirebaseTeacher> {
        return try {
            val result = collection.whereEqualTo("departmentId", departmentId).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting teachers by department", e)
        }
    }

    /**
     * Update teacher's office hours
     */
    suspend fun updateOfficeHours(teacherId: String, officeHours: String) {
        try {
            val teacher = getById(teacherId) ?: throw FirebaseException("Teacher not found")
            update(teacherId, teacher.copy(officeHours = officeHours))
        } catch (e: Exception) {
            throw FirebaseException("Error updating office hours", e)
        }
    }

    /**
     * Add a course to teacher's course list
     */
    suspend fun addCourse(teacherId: String, courseId: String) {
        try {
            val teacher = getById(teacherId) ?: throw FirebaseException("Teacher not found")
            val updatedCourses = teacher.courses.toMutableList().apply { add(courseId) }
            update(teacherId, teacher.copy(courses = updatedCourses))
        } catch (e: Exception) {
            throw FirebaseException("Error adding course to teacher", e)
        }
    }

    /**
     * Remove a course from teacher's course list
     */
    suspend fun removeCourse(teacherId: String, courseId: String) {
        try {
            val teacher = getById(teacherId) ?: throw FirebaseException("Teacher not found")
            val updatedCourses = teacher.courses.toMutableList().apply { remove(courseId) }
            update(teacherId, teacher.copy(courses = updatedCourses))
        } catch (e: Exception) {
            throw FirebaseException("Error removing course from teacher", e)
        }
    }

    /**
     * Delete teacher and associated data
     */
    suspend fun deleteTeacher(teacherId: String) {
        try {
            val teacher = getById(teacherId) ?: throw FirebaseException("Teacher not found")
            
            // Delete teacher document
            delete(teacherId)
            
            // Delete associated user account
            userRepository.deleteAccount()
        } catch (e: Exception) {
            throw FirebaseException("Error deleting teacher", e)
        }
    }
} 