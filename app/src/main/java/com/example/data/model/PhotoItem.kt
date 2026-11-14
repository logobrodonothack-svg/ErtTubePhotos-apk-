package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PhotoAlbum(val label: String) {
  REELS("Reels"),
  PORTRAITS("Portraits"),
  STUDIO("Studio"),
  TRAVEL("Travel");

  companion object {
    fun fromLabel(label: String?): PhotoAlbum {
      return entries.firstOrNull { it.label == label } ?: REELS
    }
  }
}

enum class MediaType(val wireValue: String) {
  PHOTO("photo"),
  VIDEO("video");

  companion object {
    fun fromWireValue(value: String?): MediaType {
      return entries.firstOrNull { it.wireValue == value } ?: PHOTO
    }
  }
}

@IgnoreExtraProperties
data class PhotoItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val caption: String? = null,
  val album: String = PhotoAlbum.REELS.label,
  val localPath: String? = null,
  val favorite: Boolean = false,
  val mediaType: String = MediaType.PHOTO.wireValue,
  val durationMs: Long? = null,
  val trashed: Boolean = false,
  val trashedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isVideoItem: Boolean
    get() = mediaType == MediaType.VIDEO.wireValue
}
