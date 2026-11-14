package com.example.ui.gallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GalleryMode
import com.example.data.model.MediaType
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.UserProfile
import com.example.data.repository.ErtTubeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class StorageStats(
  val usedMb: Long = 1L,
  val totalLabel: String = "1 TB",
  val formattedRatio: String = "1 MB / 1 TB",
  val progressFraction: Float = 0.015f,
  val localFileCount: Int = 0,
  val cloudFrameCount: Int = 0,
)

data class ImportedPhotoInput(
  val title: String,
  val localPath: String,
  val mediaType: MediaType = MediaType.PHOTO,
  val durationMs: Long? = null,
)

class GalleryViewModel(
  private val repository: ErtTubeRepository,
  private val currentUserId: String,
) : ViewModel() {

  val profileState: StateFlow<UserProfile?> =
    repository
      .observeUserProfile(currentUserId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing profile", firestoreError ?: error)
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val photosState: StateFlow<UiState<List<PhotoItem>>> =
    repository
      .observePhotos(currentUserId)
      .map<List<PhotoItem>, UiState<List<PhotoItem>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing photos", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load your photos"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isMutating = MutableStateFlow(false)
  val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query.take(80)
  }

  fun syncInitialProfile(
    displayName: String,
    handle: String?,
    email: String?,
    preferredGalleryMode: GalleryMode?,
    overwriteExistingPreferences: Boolean,
  ) {
    viewModelScope.launch {
      val result =
        repository.ensureOrUpdateUserProfile(
          displayName = displayName,
          handle = handle,
          email = email,
          preferredGalleryMode = preferredGalleryMode?.wireValue,
          overwriteExistingPreferences = overwriteExistingPreferences,
        )
      result.onFailure { err ->
        _actionMessage.value = err.localizedMessage ?: "Unable to sync profile"
      }
    }
  }

  fun updateProfile(
    displayName: String,
    handle: String,
    bio: String,
    preferredGalleryMode: GalleryMode,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      val result =
        repository.ensureOrUpdateUserProfile(
          displayName = displayName,
          handle = handle,
          bio = bio,
          preferredGalleryMode = preferredGalleryMode.wireValue,
          overwriteExistingPreferences = true,
        )
      _isMutating.value = false
      result
        .onSuccess { _actionMessage.value = "Profile updated" }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update profile" }
    }
  }

  fun updateBackupSettings(backupEnabled: Boolean, backupWifiOnly: Boolean) {
    viewModelScope.launch {
      _isMutating.value = true
      val result = repository.updateBackupSettings(backupEnabled, backupWifiOnly)
      _isMutating.value = false
      result
        .onSuccess {
          _actionMessage.value =
            if (backupEnabled) "Photos backup turned on" else "Photos backup turned off"
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to update backup settings"
        }
    }
  }

  fun addPhoto(
    title: String,
    caption: String?,
    album: PhotoAlbum,
    localPath: String?,
    isFavorite: Boolean,
    mediaType: MediaType = MediaType.PHOTO,
    durationMs: Long? = null,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      val result =
        repository.addPhoto(title, caption, album, localPath, isFavorite, mediaType, durationMs)
      _isMutating.value = false
      result.onFailure { err ->
        _actionMessage.value = err.localizedMessage ?: "Failed to save item"
      }
    }
  }

  fun importGooglePhotos(
    items: List<ImportedPhotoInput>,
    album: PhotoAlbum,
    note: String?,
    isFavorite: Boolean,
  ) {
    if (items.isEmpty()) return
    viewModelScope.launch {
      _isMutating.value = true
      val resolvedCaption = note?.trim()?.takeIf { it.isNotEmpty() } ?: "Imported from Google Photos"
      var importedCount = 0
      var importedPhotos = 0
      var importedVideos = 0
      var lastError: Throwable? = null
      items.forEach { item ->
        val result =
          repository.addPhoto(
            title = item.title,
            caption = resolvedCaption,
            album = album,
            localPath = item.localPath,
            isFavorite = isFavorite,
            mediaType = item.mediaType,
            durationMs = item.durationMs,
          )
        result
          .onSuccess {
            importedCount++
            if (item.mediaType == MediaType.VIDEO) importedVideos++ else importedPhotos++
          }
          .onFailure { lastError = it }
      }
      _isMutating.value = false
      _actionMessage.value =
        if (importedCount > 0) {
          val noun =
            when {
              importedVideos > 0 && importedPhotos > 0 ->
                if (importedCount == 1) "item" else "photos & videos"
              importedVideos > 0 ->
                if (importedCount == 1) "video" else "videos"
              else ->
                if (importedCount == 1) "photo" else "photos"
            }
          "Imported $importedCount $noun from Google Photos"
        } else {
          lastError?.localizedMessage ?: "Failed to import Google Photos"
        }
    }
  }

  fun updatePhoto(
    photoId: String,
    title: String,
    caption: String,
    album: PhotoAlbum,
    isFavorite: Boolean,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      val result = repository.updatePhoto(photoId, title, caption, album, isFavorite)
      _isMutating.value = false
      result.onFailure { err ->
        _actionMessage.value = err.localizedMessage ?: "Failed to update photo"
      }
    }
  }

  fun toggleFavorite(photo: PhotoItem) {
    if (photo.id.isBlank()) return
    viewModelScope.launch {
      val result = repository.toggleFavorite(photo.id, !photo.favorite)
      result.onFailure { err ->
        _actionMessage.value = err.localizedMessage ?: "Failed to update favorite"
      }
    }
  }

  fun deletePhoto(photoId: String) {
    moveToBin(photoId)
  }

  fun moveToBin(photoId: String) {
    if (photoId.isBlank()) return
    viewModelScope.launch {
      _isMutating.value = true
      val result = repository.moveToBin(photoId)
      _isMutating.value = false
      result
        .onSuccess { _actionMessage.value = "Moved to Bin" }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to move item to Bin"
        }
    }
  }

  fun moveMultipleToBin(photoIds: List<String>) {
    val cleanIds = photoIds.filter { it.isNotBlank() }.distinct()
    if (cleanIds.isEmpty()) return
    viewModelScope.launch {
      _isMutating.value = true
      val result = repository.moveMultipleToBin(cleanIds)
      _isMutating.value = false
      result
        .onSuccess { count ->
          _actionMessage.value =
            "Moved $count item${if (count == 1) "" else "s"} to Bin"
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to move items to Bin"
        }
    }
  }

  fun restoreFromBin(photoIds: List<String>) {
    val cleanIds = photoIds.filter { it.isNotBlank() }.distinct()
    if (cleanIds.isEmpty()) return
    viewModelScope.launch {
      _isMutating.value = true
      val result = repository.restoreMultipleFromBin(cleanIds)
      _isMutating.value = false
      result
        .onSuccess { count ->
          _actionMessage.value =
            "Restored $count item${if (count == 1) "" else "s"} from Bin"
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to restore items"
        }
    }
  }

  fun permanentlyDelete(items: List<PhotoItem>) {
    val cleanIds = items.map { it.id }.filter { it.isNotBlank() }.distinct()
    if (cleanIds.isEmpty()) return
    viewModelScope.launch {
      _isMutating.value = true
      withContext(Dispatchers.IO) {
        items.forEach { item ->
          item.localPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
            runCatching { File("$path.thumb.jpg").delete() }
          }
        }
      }
      val result = repository.permanentlyDeletePhotos(cleanIds)
      _isMutating.value = false
      result
        .onSuccess { count ->
          _actionMessage.value =
            "Permanently deleted $count item${if (count == 1) "" else "s"}"
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to permanently delete items"
        }
    }
  }

  fun clearLocalPhotoCache(filesDir: File, onCleared: () -> Unit = {}) {
    viewModelScope.launch {
      _isMutating.value = true
      val removedCount =
        withContext(Dispatchers.IO) {
          val photosDir = File(filesDir, "photos")
          if (!photosDir.exists()) return@withContext 0
          var deleted = 0
          photosDir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
              deleted++
            }
          }
          deleted
        }
      _isMutating.value = false
      _actionMessage.value =
        if (removedCount > 0) {
          "Cleared $removedCount local cached image${if (removedCount == 1) "" else "s"}"
        } else {
          "No local image cache to clear"
        }
      onCleared()
    }
  }

  fun notifyMessage(message: String) {
    _actionMessage.value = message
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  companion object {
    private const val TAG = "GalleryVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L
    private const val ONE_MB_BYTES = 1024L * 1024L
    private const val ONE_TB_IN_MB = 1_048_576f

    fun computeItemStorageBytes(photo: PhotoItem): Long {
      var bytes = 4_096L
      val path = photo.localPath?.takeIf { it.isNotBlank() }
      if (path != null) {
        val file = File(path)
        if (file.exists() && file.isFile) {
          bytes += file.length()
        }
        val thumb = File("$path.thumb.jpg")
        if (thumb.exists() && thumb.isFile) {
          bytes += thumb.length()
        }
      }
      return bytes
    }

    fun formatByteSize(bytes: Long): String {
      return when {
        bytes >= 1024L * 1024L ->
          String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
        bytes >= 1024L ->
          "${(bytes / 1024L).coerceAtLeast(1L)} KB"
        else -> "$bytes B"
      }
    }

    fun computeStorageStats(filesDir: File, photos: List<PhotoItem>): StorageStats {
      val seenPaths = HashSet<String>()
      var fileBytes = 0L
      var localFileCount = 0

      photos.forEach { photo ->
        val path = photo.localPath?.takeIf { it.isNotBlank() } ?: return@forEach
        val file = File(path)
        if (file.exists() && file.isFile && seenPaths.add(file.absolutePath)) {
          fileBytes += file.length()
          localFileCount++
        }
      }

      val photosDir = File(filesDir, "photos")
      if (photosDir.exists() && photosDir.isDirectory) {
        photosDir.listFiles()?.forEach { file ->
          if (file.isFile && seenPaths.add(file.absolutePath)) {
            fileBytes += file.length()
            localFileCount++
          }
        }
      }

      val metadataBytes = photos.size * 4_096L
      val totalBytes = fileBytes + metadataBytes
      val usedMb = ceil(totalBytes.toDouble() / ONE_MB_BYTES.toDouble()).toLong().coerceAtLeast(1L)
      val fraction = (usedMb.toFloat() / ONE_TB_IN_MB).coerceIn(0.015f, 1f)

      return StorageStats(
        usedMb = usedMb,
        totalLabel = "1 TB",
        formattedRatio = "$usedMb MB / 1 TB",
        progressFraction = fraction,
        localFileCount = localFileCount,
        cloudFrameCount = photos.size,
      )
    }
  }
}
