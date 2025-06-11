package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.firestore.FieldPath

class FirebaseCourseRepository(
    private val teacherRepository: FirebaseTeacherRepository // Inject FirebaseTeacherRepository
) : BaseFirebaseRepository<FirebaseCourse>("courses") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseCourse {
        return FirebaseCourse(
            courseId = documentId,
            name = document["name"] as? String ?: "",
            title = document["title"] as? String ?: "",
            description = document["description"] as? String ?: "",
            objectives = document["objectives"] as? String ?: "",
            location = document["location"] as? String,
            teacherId = document["teacherId"] as? String ?: "",
            departmentId = document["departmentId"] as? String ?: ""
        )
    }

    override fun objectToDocument(obj: FirebaseCourse): Map<String, Any?> {
        return mapOf(
            "name" to obj.name,
            "title" to obj.title,
            "description" to obj.description,
            "objectives" to obj.objectives,
            "location" to obj.location,
            "teacherId" to obj.teacherId,
            "departmentId" to obj.departmentId
        )
    }

    /**
     * Get course by course ID
     */
    suspend fun getCourseByCourseId(courseId: String): FirebaseCourse? {
        return try {
            val document = collection.document(courseId).get().await()
            if (document.exists()) {
                document.data?.let { documentToObject(document.id, it) }
            } else {
                Log.w("FirebaseCourseRepo", "Course not found for ID: $courseId")
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseCourseRepo", "Error getting course by ID: $courseId", e)
            null
        }
    }

    /**
     * Get all courses taught by a teacher
     */
    suspend fun getCoursesByTeacher(teacherId: String): List<FirebaseCourse> {
        return try {
            // Fetch the teacher document to get the list of course IDs
            val teacher = teacherRepository.getById(teacherId)
            if (teacher == null || teacher.courses.isEmpty()) {
                Log.d("FirebaseCourseRepo", "Teacher not found or has no courses for teacherId: $teacherId")
                return emptyList()
            }
            val courseIds = teacher.courses
            Log.d("FirebaseCourseRepo", "Course IDs for teacher $teacherId: $courseIds")

            // Fetch courses by ID
            if (courseIds.isEmpty()) {
                Log.d("FirebaseCourseRepo", "Course ID list is empty, returning empty list.")
                return emptyList()
            }
            // Firestore limits whereIn to 10 items, so split into chunks if necessary
            val chunkedCourseIds = courseIds.chunked(10)
            val courses = mutableListOf<FirebaseCourse>()
            for (chunk in chunkedCourseIds) {
                val result = collection.whereIn(FieldPath.documentId(), chunk).get().await()
                Log.d("FirebaseCourseRepo", "Fetched ${result.documents.size} courses for chunk: $chunk")
                courses.addAll(result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } })
            }
            Log.d("FirebaseCourseRepo", "Total courses fetched for teacher $teacherId: ${courses.size}")
            courses
        } catch (e: Exception) {
            Log.e("FirebaseCourseRepo", "Error getting courses by teacher $teacherId", e)
            emptyList()
        }
    }

    /**
     * Update course description
     */
    suspend fun updateCourseDescription(courseId: String, title: String, description: String, objectives: String) {
        try {
            val course = getById(courseId) ?: throw FirebaseException("Course not found")
            course.updateCourseDescription(title, description, objectives)
            update(courseId, course)
        } catch (e: Exception) {
            Log.e("FirebaseCourseRepo", "Error updating course description", e)
            throw FirebaseException("Error updating course description", e)
        }
    }

    /**
     * Update course location
     */
    suspend fun updateCourseLocation(courseId: String, location: String?) {
        try {
            val course = getById(courseId) ?: throw FirebaseException("Course not found")
            update(courseId, course.copy(location = location))
        } catch (e: Exception) {
            Log.e("FirebaseCourseRepo", "Error updating course location", e)
            throw FirebaseException("Error updating course location", e)
        }
    }

    /**
     * Get courses by a list of course IDs
     */
    suspend fun getCoursesByIds(courseIds: List<String>): List<FirebaseCourse> {
        Log.d("FirebaseCourseRepo", "Fetching courses for IDs: $courseIds")
        if (courseIds.isEmpty()) {
            Log.d("FirebaseCourseRepo", "Course ID list is empty, returning empty list.")
            return emptyList()
        }
        return try {
            // Handle Firestore whereIn limit of 10 items
            val chunkedCourseIds = courseIds.chunked(10)
            val courses = mutableListOf<FirebaseCourse>()
            for (chunk in chunkedCourseIds) {
                val result = collection.whereIn(FieldPath.documentId(), chunk).get().await()
                Log.d("FirebaseCourseRepo", "Course query result size: ${result.documents.size} for chunk: $chunk")
                courses.addAll(result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } })
            }
            courses
        } catch (e: Exception) {
            Log.e("FirebaseCourseRepo", "Error getting courses by IDs", e)
            emptyList()
        }
    }

    /**
     * Delete course and associated data
     */
    suspend fun deleteCourse(courseId: String) {
        try {
            val course = getById(courseId) ?: throw FirebaseException("Course not found")
            // Delete course document
            delete(courseId)
            // Optionally remove course from teacher's course list
            val teacherId = course.teacherId
            if (teacherId.isNotEmpty()) {
                teacherRepository.removeCourse(teacherId, courseId)
                Log.d("FirebaseCourseRepo", "Removed course $courseId from teacher $teacherId")
            }
        } catch (e: Exception) {
            Log.e("FirebaseCourseRepo", "Error deleting course", e)
            throw FirebaseException("Error deleting course", e)
        }
    }
}