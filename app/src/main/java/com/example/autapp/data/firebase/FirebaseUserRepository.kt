package com.example.autapp.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import android.util.Log

class FirebaseUserRepository : BaseFirebaseRepository<FirebaseUser>("users") {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseUser {
        return FirebaseUser(
            id = documentId,
            firstName = document["firstName"] as? String ?: "",
            lastName = document["lastName"] as? String ?: "",
            role = document["role"] as? String ?: "",
            username = document["username"] as? String ?: "",
            password = document["password"] as? String ?: "",
            isFirstLogin = document["isFirstLogin"] as? Boolean ?: true
        )
    }

    override fun objectToDocument(obj: FirebaseUser): Map<String, Any?> {
        return mapOf(
            "firstName" to obj.firstName,
            "lastName" to obj.lastName,
            "role" to obj.role,
            "username" to obj.username,
            "password" to obj.password,
            "isFirstLogin" to obj.isFirstLogin
        )
    }

    /**
     * Register a new user with email and password
     */
    suspend fun registerUser(email: String, password: String, user: FirebaseUser): String {
        return try {
            // Create authentication user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw FirebaseException("Failed to create user")

            // Add logging for successful auth user creation
            Log.d("FirebaseUserRepository", "Firebase Auth user created successfully with UID: $userId for email: $email")

            // Update user profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("${user.firstName} ${user.lastName}")
                .build()
            authResult.user?.updateProfile(profileUpdates)?.await()

            // Create user document in Firestore
            val userWithId = user.copy(id = userId)
            createWithId(userId, userWithId)

            userId
        } catch (e: Exception) {
            // Add logging for auth user creation failure
            Log.e("FirebaseUserRepository", "Error creating Firebase Auth user for email: $email", e)
            throw FirebaseException("Error registering user", e)
        }
    }

    /**
     * Sign in with username and password
     */
    suspend fun signIn(username: String, password: String): FirebaseUser? {
        return try {
            // First check if the user exists in Firestore
            val userDoc = getUserByUsername(username)
            if (userDoc == null) {
                Log.e("FirebaseUserRepository", "User not found in Firestore: $username")
                throw FirebaseException("User not found")
            }

            // Sign in with email and password using Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(username, password).await()
            val userId = authResult.user?.uid ?: throw FirebaseException("Failed to get user ID after sign in")

            // Verify the user ID matches
            if (userId != userDoc.id) {
                Log.e("FirebaseUserRepository", "User ID mismatch: Auth ID=$userId, Firestore ID=${userDoc.id}")
                throw FirebaseException("User ID mismatch")
            }

            userDoc
        } catch (e: Exception) {
            // Add logging for sign-in failure
            Log.e("FirebaseUserRepository", "Error signing in with email: $username", e)
            throw FirebaseException("Error signing in", e)
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            throw FirebaseException("Error signing out", e)
        }
    }

    /**
     * Get the current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            FirebaseUser(
                id = firebaseUser.uid,
                firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "",
                lastName = firebaseUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                username = firebaseUser.email ?: "",
                password = "", // Password is not stored in the client
                role = "" // Role will be fetched from Firestore
            )
        } else {
            null
        }
    }

    /**
     * Update user password
     */
    suspend fun updatePassword(newPassword: String) {
        try {
            auth.currentUser?.updatePassword(newPassword)?.await()
        } catch (e: Exception) {
            throw FirebaseException("Error updating password", e)
        }
    }

    /**
     * Reset password for a user
     */
    suspend fun resetPassword(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw FirebaseException("Error sending password reset email", e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(user: FirebaseUser) {
        try {
            // Update Firestore document
            update(user.id, user)

            // Update auth profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("${user.firstName} ${user.lastName}")
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
        } catch (e: Exception) {
            throw FirebaseException("Error updating profile", e)
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount() {
        try {
            val user = auth.currentUser ?: throw FirebaseException("No user logged in")
            
            // Delete Firestore document
            delete(user.uid)
            
            // Delete auth user
            user.delete().await()
        } catch (e: Exception) {
            throw FirebaseException("Error deleting account", e)
        }
    }

    /**
     * Check if a username is available
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val result = collection.whereEqualTo("username", username).get().await()
            result.isEmpty
        } catch (e: Exception) {
            throw FirebaseException("Error checking username availability", e)
        }
    }

    /**
     * Get user by username
     */
    suspend fun getUserByUsername(username: String): FirebaseUser? {
        return try {
            Log.d("FirebaseUserRepository", "Searching for user with username: $username")
            val result = collection.whereEqualTo("username", username).get().await()
            
            // Add detailed logging for the query result
            Log.d("FirebaseUserRepository", "Query returned ${result.documents.size} documents")
            result.documents.forEachIndexed { index, doc ->
                Log.d("FirebaseUserRepository", "Document $index: id=${doc.id}, data=${doc.data}")
            }

            result.documents.firstOrNull()?.data?.let { documentToObject(result.documents.first().id, it) }
        } catch (e: Exception) {
            Log.e("FirebaseUserRepository", "Error getting user by username: $username", e)
            throw FirebaseException("Error getting user by username", e)
        }
    }
} 