package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GalleryMode(val wireValue: String, val label: String, val description: String) {
  CINEMA_REELS("cinema_reels", "Cinema Reels", "Widescreen frames & motion stills"),
  DAILY_SNAPS("daily_snaps", "Daily Snaps", "Fast candid grids & moments"),
  STUDIO_SHOTS("studio_shots", "Studio Shots", "High-contrast curated collections");

  companion object {
    fun fromWireValue(value: String?): GalleryMode {
      return entries.firstOrNull { it.wireValue == value } ?: CINEMA_REELS
    }
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val handle: String? = null,
  val bio: String? = null,
  val email: String? = null,
  val preferredGalleryMode: String? = null,
  val backupEnabled: Boolean = false,
  val backupWifiOnly: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
