package com.example.autapp.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import android.util.Log

/**
 * Base repository class that provides common Firestore operations
 * @param T The data model type
 * @param collectionPath The Firestore collection path
 */
abstract class BaseFirebaseRepository<T>(
    protected val collectionPath: String
) {
    protected val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    protected val collection: CollectionReference = db.collection(collectionPath)

    /**
     * Convert a Firestore document to a model object
     */
    protected abstract fun documentToObject(documentId: String, document: Map<String, Any?>): T

    /**
     * Convert a model object to a Firestore document
     */
    protected abstract fun objectToDocument(obj: T): Map<String, Any?>

    /**
     * Create a new document
     */
    suspend fun create(obj: T): String {
        return try {
            // Add the document first to get the auto-generated ID
            val docRef = collection.add(objectToDocument(obj)).await()
            val documentId = docRef.id
            // Now update the document to include the ID in a field (assuming your model has an ID field)
            // This is a common pattern if you want the document ID to be part of the document data.
            // If your model already includes the ID before calling create, this step might be different or unnecessary.
            collection.document(documentId).update("id", documentId).await() // Assuming your model has an 'id' field
            docRef.id
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error creating document", e)
        }
    }

    /**
     * Create a document with a specific ID
     */
    suspend fun createWithId(id: String, obj: T) {
        try {
            collection.document(id).set(objectToDocument(obj)).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error creating document with ID", e)
        }
    }

    /**
     * Get a document by ID
     */
    suspend fun getById(id: String): T? {
        return try {
            val document = collection.document(id).get().await()
            if (document.exists()) {
                documentToObject(id, document.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error getting document", e)
        }
    }

    /**
     * Update a document
     */
    suspend fun update(id: String, obj: T) {
        try {
            collection.document(id).update(objectToDocument(obj)).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error updating document", e)
        }
    }

    /**
     * Delete a document
     */
    suspend fun delete(id: String) {
        try {
            collection.document(id).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error deleting document", e)
        }
    }

    /**
     * Get all documents in the collection
     */
    suspend fun getAll(): List<T> {
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { documentToObject(doc.id, it) }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("BaseFirebaseRepo", "Error fetching all documents for collection $collectionPath", e)
            throw FirebaseException("Error fetching all documents for collection $collectionPath", e)
        }
    }

    /**
     * Query documents with a specific field value
     */
    suspend fun queryByField(field: String, value: Any, source: Source = Source.CACHE): List<T> {
        return try {
            val snapshot = collection.whereEqualTo(field, value).get(source).await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { documentToObject(doc.id, it) }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error querying documents", e)
        }
    }

    /**
     * Query documents with multiple conditions
     */
    suspend fun query(conditions: List<QueryCondition>): List<T> {
        return try {
            var query: Query = collection
            conditions.forEach { condition ->
                query = when (condition.operator) {
                    QueryOperator.EQUAL_TO -> query.whereEqualTo(condition.field, condition.value)
                    QueryOperator.GREATER_THAN -> query.whereGreaterThan(condition.field, condition.value)
                    QueryOperator.LESS_THAN -> query.whereLessThan(condition.field, condition.value)
                    QueryOperator.GREATER_THAN_OR_EQUAL_TO -> query.whereGreaterThanOrEqualTo(condition.field, condition.value)
                    QueryOperator.LESS_THAN_OR_EQUAL_TO -> query.whereLessThanOrEqualTo(condition.field, condition.value)
                    QueryOperator.ARRAY_CONTAINS -> query.whereArrayContains(condition.field, condition.value)
                }
            }
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { documentToObject(doc.id, it) }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw FirebaseException("Error querying documents", e)
        }
    }

    /**
     * Custom exception for Firebase operations
     */
    class FirebaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
}

/**
 * Query condition for Firestore queries
 */
data class QueryCondition(
    val field: String,
    val operator: QueryOperator,
    val value: Any
)

/**
 * Query operators for Firestore queries
 */
enum class QueryOperator {
    EQUAL_TO,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN_OR_EQUAL_TO,
    ARRAY_CONTAINS
} 