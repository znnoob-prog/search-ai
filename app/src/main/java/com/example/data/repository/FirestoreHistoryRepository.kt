package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreHistoryRepo"

@Keep
@IgnoreExtraProperties
data class CloudSearchHistoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val query: String = "",
    val isDeepThinking: Boolean = false,
    val modelUsed: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

class FirestoreHistoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun observeCloudHistory(): Flow<List<CloudSearchHistoryItem>> = flow {
        val user = auth.currentUser
        if (user == null || user.uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = user.uid
        emitAll(
            db.collection("users").document(uid).collection("search_history")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudSearchHistoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun saveQueryToCloud(query: String, isDeepThinking: Boolean = false, modelUsed: String = "gemini-4-hydrogen"): Result<Unit> {
        val user = auth.currentUser ?: return Result.success(Unit) // Offline / unauthenticated
        val uid = user.uid
        return try {
            val docRef = db.collection("users").document(uid).collection("search_history").document()
            val payload = mapOf(
                "userId" to uid,
                "query" to query.trim(),
                "isDeepThinking" to isDeepThinking,
                "modelUsed" to modelUsed,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save search history to cloud", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudQuery(historyId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.success(Unit)
        return try {
            db.collection("users").document(user.uid).collection("search_history")
                .document(historyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cloud history item", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllCloudHistory(): Result<Unit> {
        val user = auth.currentUser ?: return Result.success(Unit)
        return try {
            val snapshot = db.collection("users").document(user.uid).collection("search_history").get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cloud search history", e)
            Result.failure(e)
        }
    }
}
