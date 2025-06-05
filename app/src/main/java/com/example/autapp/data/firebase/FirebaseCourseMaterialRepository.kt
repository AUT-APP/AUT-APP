package com.example.autapp.data.firebase

import com.example.autapp.data.models.CourseMaterial
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class FirebaseCourseMaterialRepository : BaseFirebaseRepository<CourseMaterial>("course_materials") {

     override fun documentToObject(documentId: String, document: Map<String, Any?>): CourseMaterial {
         return CourseMaterial(
             materialId = documentId,
             courseId = document["courseId"] as? String ?: "",
             title = document["title"] as? String ?: "",
             description = document["description"] as? String ?: "",
             type = document["type"] as? String ?: "",
             contentUrl = document["contentUrl"] as? String
         )
}
    override fun objectToDocument(obj: CourseMaterial): Map<String, Any?> {
        return mapOf(
            "courseId" to obj.courseId,
            "title" to obj.title,
            "description" to obj.description,
            "type" to obj.type,
            "contentUrl" to obj.contentUrl
        )
    }
    suspend fun getMaterialsByCourse(courseId: String): List<CourseMaterial> {
        return try {
            val snapshot = collection
                .whereEqualTo("courseId", courseId)
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let {
                    val material = documentToObject(doc.id, it)
                    println("DEBUG: Material = $material")
                    material
                }
            }
        } catch (e: Exception) {
            // Optionally log this for debugging
            emptyList()
        }
    }
}