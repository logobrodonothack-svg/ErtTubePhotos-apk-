package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ErtTubeRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val docRef = db.collection("users").document(userId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
          }
          throw error
        }
    )
  }

  suspend fun ensureOrUpdateUserProfile(
    displayName: String,
    handle: String? = null,
    bio: String? = null,
    email: String? = null,
    preferredGalleryMode: String? = null,
    overwriteExistingPreferences: Boolean = false,
  ): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid)
    val safeName = displayName.trim().ifBlank { "ErtTube Creator" }.take(80)
    val safeHandle = handle?.trim()?.takeIf { it.isNotEmpty() }?.take(40)
    val safeBio = bio?.trim()?.takeIf { it.isNotEmpty() }?.take(200)
    val safeEmail = email?.trim()?.takeIf { it.isNotEmpty() }?.take(160)

    return try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val createPayload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", safeName)
            safeHandle?.let { put("handle", it) }
            safeBio?.let { put("bio", it) }
            safeEmail?.let { put("email", it) }
            preferredGalleryMode?.let { put("preferredGalleryMode", it) }
            put("backupEnabled", false)
            put("backupWifiOnly", true)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(createPayload).await()
      } else if (overwriteExistingPreferences) {
        val updatePayload =
          buildMap<String, Any> {
            put("displayName", safeName)
            safeHandle?.let { put("handle", it) }
            safeBio?.let { put("bio", it) }
            safeEmail?.let { put("email", it) }
            preferredGalleryMode?.let { put("preferredGalleryMode", it) }
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.update(updatePayload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateBackupSettings(
    backupEnabled: Boolean,
    backupWifiOnly: Boolean,
  ): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid)
    val payload =
      mapOf<String, Any>(
        "backupEnabled" to backupEnabled,
        "backupWifiOnly" to backupWifiOnly,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val displayName = auth.currentUser?.displayName?.ifBlank { null } ?: "ErtTube Creator"
        val createPayload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", displayName.take(80))
            auth.currentUser?.email?.takeIf { it.isNotBlank() }?.let { put("email", it.take(160)) }
            put("backupEnabled", backupEnabled)
            put("backupWifiOnly", backupWifiOnly)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(createPayload).await()
      } else {
        docRef.update(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observePhotos(userId: String): Flow<List<PhotoItem>> = flow {
    val colRef = db.collection("users").document(userId).collection("photos")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(PhotoItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
          }
          throw error
        }
    )
  }

  suspend fun getPhotoById(photoId: String): Result<PhotoItem?> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid).collection("photos").document(photoId)
    return try {
      val snapshot = docRef.get().await()
      val item =
        if (snapshot.exists()) {
          snapshot.toObject(
            PhotoItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      Result.success(item)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun addPhoto(
    title: String,
    caption: String?,
    album: PhotoAlbum,
    localPath: String?,
    isFavorite: Boolean = false,
    mediaType: com.example.data.model.MediaType = com.example.data.model.MediaType.PHOTO,
    durationMs: Long? = null,
  ): Result<String> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid).collection("photos").document()
    val safeTitle = title.trim().ifBlank { "Untitled Frame" }.take(100)
    val safeCaption = caption?.trim()?.takeIf { it.isNotEmpty() }?.take(400)
    val safePath = localPath?.trim()?.takeIf { it.isNotEmpty() }?.take(500)
    val safeDuration = durationMs?.takeIf { it >= 0L }

    val payload =
      buildMap<String, Any> {
        put("userId", uid)
        put("title", safeTitle)
        safeCaption?.let { put("caption", it) }
        put("album", album.label)
        safePath?.let { put("localPath", it) }
        put("favorite", isFavorite)
        put("mediaType", mediaType.wireValue)
        safeDuration?.let { put("durationMs", it) }
        put("trashed", false)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }

    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updatePhoto(
    photoId: String,
    title: String,
    caption: String,
    album: PhotoAlbum,
    isFavorite: Boolean,
  ): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid).collection("photos").document(photoId)
    val payload =
      mapOf<String, Any>(
        "title" to title.trim().ifBlank { "Untitled Frame" }.take(100),
        "caption" to caption.trim().take(400),
        "album" to album.label,
        "favorite" to isFavorite,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun toggleFavorite(photoId: String, isFavorite: Boolean): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid).collection("photos").document(photoId)
    val payload =
      mapOf<String, Any>(
        "favorite" to isFavorite,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun moveToBin(photoId: String): Result<Unit> =
    moveMultipleToBin(listOf(photoId)).map { }

  suspend fun moveMultipleToBin(photoIds: List<String>): Result<Int> {
    val cleanIds = photoIds.filter { it.isNotBlank() }.distinct()
    if (cleanIds.isEmpty()) return Result.success(0)
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val photosCol = db.collection("users").document(uid).collection("photos")
    return try {
      cleanIds.chunked(BATCH_LIMIT).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { id ->
          batch.update(
            photosCol.document(id),
            mapOf<String, Any?>(
              "trashed" to true,
              "trashedAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
      }
      Result.success(cleanIds.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, photosCol.path, auth)
      Result.failure(e)
    }
  }

  suspend fun restoreMultipleFromBin(photoIds: List<String>): Result<Int> {
    val cleanIds = photoIds.filter { it.isNotBlank() }.distinct()
    if (cleanIds.isEmpty()) return Result.success(0)
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val photosCol = db.collection("users").document(uid).collection("photos")
    return try {
      cleanIds.chunked(BATCH_LIMIT).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { id ->
          batch.update(
            photosCol.document(id),
            mapOf<String, Any?>(
              "trashed" to false,
              "trashedAt" to null,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
      }
      Result.success(cleanIds.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, photosCol.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deletePhoto(photoId: String): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection("users").document(uid).collection("photos").document(photoId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun permanentlyDeletePhotos(photoIds: List<String>): Result<Int> {
    val cleanIds = photoIds.filter { it.isNotBlank() }.distinct()
    if (cleanIds.isEmpty()) return Result.success(0)
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val photosCol = db.collection("users").document(uid).collection("photos")
    return try {
      cleanIds.chunked(BATCH_LIMIT).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { id ->
          batch.delete(photosCol.document(id))
        }
        batch.commit().await()
      }
      Result.success(cleanIds.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, photosCol.path, auth)
      Result.failure(e)
    }
  }

  private companion object {
    const val BATCH_LIMIT = 450
  }
}
