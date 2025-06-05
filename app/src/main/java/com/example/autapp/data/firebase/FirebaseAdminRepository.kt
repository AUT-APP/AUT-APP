package com.example.autapp.data.firebase

import kotlinx.coroutines.tasks.await

class FirebaseAdminRepository : BaseFirebaseRepository<FirebaseAdmin>("admins") {
    private val userRepository = FirebaseUserRepository()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseAdmin {
        return FirebaseAdmin(
            adminId = documentId, // Use the documentId provided
            firstName = document["firstName"] as? String ?: "",
            lastName = document["lastName"] as? String ?: "",
            username = document["username"] as? String ?: "",
            password = document["password"] as? String ?: "",
            role = document["role"] as? String ?: "Admin",
            department = document["department"] as? String ?: "",
            accessLevel = (document["accessLevel"] as? Number)?.toInt() ?: 0
        )
    }

    override fun objectToDocument(obj: FirebaseAdmin): Map<String, Any?> {
        return mapOf(
            "firstName" to obj.firstName,
            "lastName" to obj.lastName,
            "username" to obj.username,
            "password" to obj.password,
            "role" to obj.role,
            "department" to obj.department,
            "accessLevel" to obj.accessLevel
        )
    }

    /**
     * Create a new admin with user account
     */
    suspend fun createAdminWithUser(
        email: String,
        password: String,
        admin: FirebaseAdmin,
        user: FirebaseUser
    ): String {
        return try {
            // Create user account first
            val userId = userRepository.registerUser(email, password, user.copy(role = "Admin"))
            
            // Create admin document
            val adminWithId = admin.copy(adminId = userId)
            val adminId = create(adminWithId)
            
            adminId
        } catch (e: Exception) {
            // If admin creation fails, delete the user account
            try {
                userRepository.deleteAccount()
            } catch (deleteError: Exception) {
                // Log the error but throw the original exception
                e.addSuppressed(deleteError)
            }
            throw FirebaseException("Error creating admin", e)
        }
    }

    /**
     * Get admin by admin ID
     */
    suspend fun getAdminByAdminId(adminId: String): FirebaseAdmin? {
        return try {
            val result = collection.whereEqualTo("adminId", adminId).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting admin by ID", e)
        }
    }

    /**
     * Get all admins in a department
     */
    suspend fun getAdminsByDepartment(department: String): List<FirebaseAdmin> {
        return try {
            val result = collection.whereEqualTo("department", department).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting admins by department", e)
        }
    }

    /**
     * Get admins by access level
     */
    suspend fun getAdminsByAccessLevel(accessLevel: Int): List<FirebaseAdmin> {
        return try {
            val result = collection.whereEqualTo("accessLevel", accessLevel).get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting admins by access level", e)
        }
    }

    /**
     * Update admin's access level
     */
    suspend fun updateAccessLevel(adminId: String, accessLevel: Int) {
        try {
            val admin = getById(adminId) ?: throw FirebaseException("Admin not found")
            update(adminId, admin.copy(accessLevel = accessLevel))
        } catch (e: Exception) {
            throw FirebaseException("Error updating access level", e)
        }
    }

    /**
     * Update admin's department
     */
    suspend fun updateDepartment(adminId: String, department: String) {
        try {
            val admin = getById(adminId) ?: throw FirebaseException("Admin not found")
            update(adminId, admin.copy(department = department))
        } catch (e: Exception) {
            throw FirebaseException("Error updating department", e)
        }
    }

    /**
     * Delete admin and associated data
     */
    suspend fun deleteAdmin(adminId: String) {
        try {
            val admin = getById(adminId) ?: throw FirebaseException("Admin not found")
            
            // Delete admin document
            delete(adminId)
            
            // Delete associated user account
            userRepository.deleteAccount()
        } catch (e: Exception) {
            throw FirebaseException("Error deleting admin", e)
        }
    }
} 