package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseDepartmentRepository : BaseFirebaseRepository<FirebaseDepartment>("departments") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseDepartment {
        return FirebaseDepartment(
            departmentId = documentId,
            name = document["name"] as? String ?: "",
            type = document["type"] as? String ?: "",
            description = document["description"] as? String
        )
    }

    override fun objectToDocument(obj: FirebaseDepartment): Map<String, Any?> {
        return mapOf(
            "name" to obj.name,
            "type" to obj.type,
            "description" to obj.description
        )
    }

    /**
     * Get department by department ID
     */
    suspend fun getDepartmentByDepartmentId(departmentId: String): FirebaseDepartment? {
        Log.d("FirebaseDepartmentRepo", "Attempting to fetch department with ID: $departmentId")
        return try {
            val documentSnapshot = collection.document(departmentId).get().await()
            val department = documentSnapshot.data?.let { documentToObject(documentSnapshot.id, it) }
            if (department != null) {
                Log.d("FirebaseDepartmentRepo", "Successfully fetched department: ${department.name}")
            } else {
                Log.d("FirebaseDepartmentRepo", "Department with ID $departmentId not found.")
            }
            department
        } catch (e: Exception) {
            Log.e("FirebaseDepartmentRepo", "Error getting department by ID", e)
            throw FirebaseException("Error getting department by ID", e)
        }
    }

    /**
     * Get departments by type
     */
    suspend fun getDepartmentsByType(type: String): List<FirebaseDepartment> {
        return try {
            val result = collection.whereEqualTo("type", type).get().await()
            result.documents.mapNotNull { doc ->
                doc.data?.let { documentToObject(doc.id, it) }
            }
        } catch (e: Exception) {
            throw FirebaseException("Error getting departments by type", e)
        }
    }

    /**
     * Get all major departments
     */
    suspend fun getMajorDepartments(): List<FirebaseDepartment> {
        return getDepartmentsByType("Major")
    }

    /**
     * Get all minor departments
     */
    suspend fun getMinorDepartments(): List<FirebaseDepartment> {
        return getDepartmentsByType("Minor")
    }

    /**
     * Get all regular departments
     */
    suspend fun getRegularDepartments(): List<FirebaseDepartment> {
        return getDepartmentsByType("Department")
    }

    /**
     * Update department description
     */
    suspend fun updateDepartmentDescription(departmentId: String, description: String?) {
        try {
            val department = getById(departmentId) ?: throw FirebaseException("Department not found")
            update(departmentId, department.copy(description = description))
        } catch (e: Exception) {
            throw FirebaseException("Error updating department description", e)
        }
    }

    /**
     * Delete department
     * Note: This should only be called if there are no references to this department
     */
    suspend fun deleteDepartment(departmentId: String) {
        try {
            val department = getById(departmentId) ?: throw FirebaseException("Department not found")
            
            // Check if department is in use
            // This would typically be done through a Cloud Function or transaction
            // to ensure data consistency
            
            // Delete department document
            delete(departmentId)
        } catch (e: Exception) {
            throw FirebaseException("Error deleting department", e)
        }
    }
} 