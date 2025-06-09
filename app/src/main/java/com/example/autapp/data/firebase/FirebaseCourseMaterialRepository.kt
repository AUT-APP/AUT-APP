package com.example.autapp.data.firebase

import android.content.Context
import android.util.Log
import com.example.autapp.data.models.CourseMaterial
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.autapp.data.models.Notification
import com.example.autapp.util.NotificationHelper
import androidx.core.app.NotificationCompat
import com.example.autapp.R
import com.example.autapp.data.firebase.FirebaseCourseRepository

class FirebaseCourseMaterialRepository(
    private val notificationRepository: FirebaseNotificationRepository,
    private val courseRepository: FirebaseCourseRepository
) : BaseFirebaseRepository<CourseMaterial>("course_materials") {

    companion object {
        const val MATERIAL_CHANNEL_ID = "material_channel"
    }

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
            Log.e("MaterialRepo", "Error loading materials: ${e.message}", e)
            emptyList()
        }
    }

    private fun buildMaterialNotification(title: String, text: String, material: CourseMaterial): Notification {
        return Notification(
            notificationId = (material.courseId + material.materialId).hashCode(),
            iconResId = R.drawable.ic_notification,
            title = title,
            text = text,
            channelId = MATERIAL_CHANNEL_ID,
            priority = NotificationCompat.PRIORITY_HIGH,
            deepLinkUri = "myapp://materials/${material.courseId}",
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun addMaterialAndNotify(material: CourseMaterial, context: Context): Boolean {
        return try {
            val savedId = create(material)
            val savedMaterial = material.copy(materialId = savedId)
            val course = courseRepository.getById(savedMaterial.courseId)
            val courseTitle = course?.title ?: "Unknown Course"
            val courseCode = course?.name ?: savedMaterial.courseId
            val notification = buildMaterialNotification(
                title = "New ${savedMaterial.type} material added",
                text = "New ${savedMaterial.type.lowercase()} material \"${savedMaterial.title}\" was uploaded for $courseTitle ($courseCode)",
                material = savedMaterial
            )
            notificationRepository.create(notification.toFirebaseNotification())
            NotificationHelper.pushNotification(context, notification)
            true
        } catch (e: Exception) {
            Log.e("MaterialRepo", "Error adding material: ${e.message}", e)
            false
        }
    }

    suspend fun updateMaterialAndNotify(material: CourseMaterial, context: Context): Boolean {
        return try {
            update(material.materialId, material)
            val course = courseRepository.getById(material.courseId)
            val courseTitle = course?.title ?: "Unknown Course"
            val courseCode = course?.name ?: material.courseId
            val notification = buildMaterialNotification(
                title = "${material.type} material updated",
                text = "${material.type} material \"${material.title}\" was updated for $courseTitle ($courseCode)",
                material = material
            )
            notificationRepository.create(notification.toFirebaseNotification())
            NotificationHelper.pushNotification(context, notification)
            true
        } catch (e: Exception) {
            Log.e("MaterialRepo", "Error updating material: ${e.message}", e)
            false
        }
    }

    suspend fun deleteMaterialAndNotify(material: CourseMaterial, context: Context): Boolean {
        return try {
            delete(material.materialId)
            val course = courseRepository.getById(material.courseId)
            val courseTitle = course?.title ?: "Unknown Course"
            val courseCode = course?.name ?: material.courseId
            val notification = buildMaterialNotification(
                title = "${material.type} material deleted",
                text = "${material.type} material \"${material.title}\" was deleted from $courseTitle ($courseCode)",
                material = material
            )
            notificationRepository.create(notification.toFirebaseNotification())
            NotificationHelper.pushNotification(context, notification)
            true
        } catch (e: Exception) {
            Log.e("MaterialRepo", "Error deleting material: ${e.message}", e)
            false
        }
    }
}