package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GalleryMode
import com.example.data.model.PhotoAlbum
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErtTubeRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureOrUpdateUserProfile_authenticatedUser_createsAndReadsProfile() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ErtTubeRepository(firestore, auth)

    val result =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.ensureOrUpdateUserProfile(
          displayName = "Alice Creator",
          handle = "@alice_ert",
          bio = "Visual diary",
          email = ALICE_EMAIL,
          preferredGalleryMode = GalleryMode.CINEMA_REELS.wireValue,
          overwriteExistingPreferences = true,
        )
      }
    assertTrue(result.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile(uid).first { it != null } }
    assertNotNull(profile)
    assertEquals("Alice Creator", profile?.displayName)
    assertEquals("@alice_ert", profile?.handle)
    assertEquals(false, profile?.backupEnabled)

    val backupResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateBackupSettings(backupEnabled = true, backupWifiOnly = false)
      }
    assertTrue(
      "backupResult failed: ${backupResult.exceptionOrNull()?.stackTraceToString()}",
      backupResult.isSuccess,
    )
    val updatedProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeUserProfile(uid).first { it?.backupEnabled == true }
      }
    assertEquals(true, updatedProfile?.backupEnabled)
    assertEquals(false, updatedProfile?.backupWifiOnly)
  }

  @Test
  fun addAndObservePhotos_authenticatedOwner_emitsPhotoItem() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ErtTubeRepository(firestore, auth)

    val addResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addPhoto(
          title = "Neon Studio",
          caption = "Violet rim lighting",
          album = PhotoAlbum.STUDIO,
          localPath = null,
          isFavorite = true,
        )
      }
    assertTrue(addResult.isSuccess)
    val photoId = addResult.getOrThrow()
    assertTrue(photoId.isNotEmpty())

    val photos =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePhotos(uid).first { list -> list.any { it.id == photoId } }
      }
    assertTrue(photos.any { it.id == photoId && it.title == "Neon Studio" && it.favorite })

    val videoResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addPhoto(
          title = "ErtTube Reel Clip",
          caption = "60fps B-roll",
          album = PhotoAlbum.REELS,
          localPath = null,
          isFavorite = false,
          mediaType = com.example.data.model.MediaType.VIDEO,
          durationMs = 18500L,
        )
      }
    assertTrue("addPhoto video failed: ${videoResult.exceptionOrNull()}", videoResult.isSuccess)
    val videoId = videoResult.getOrThrow()
    val updatedWithVideo =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePhotos(uid).first { list -> list.any { it.id == videoId } }
      }
    assertTrue(updatedWithVideo.any { it.id == videoId && it.isVideoItem && it.durationMs == 18500L })

    val toggleResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.toggleFavorite(photoId, false) }
    assertTrue(toggleResult.isSuccess)

    val binResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.moveToBin(photoId) }
    assertTrue("moveToBin failed: ${binResult.exceptionOrNull()}", binResult.isSuccess)
    val binnedList =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePhotos(uid).first { list -> list.any { it.id == photoId && it.trashed } }
      }
    assertTrue(binnedList.any { it.id == photoId && it.trashed })

    val restoreResult =
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.restoreMultipleFromBin(listOf(photoId)) }
    assertTrue("restore failed: ${restoreResult.exceptionOrNull()}", restoreResult.isSuccess)
    val restoredList =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePhotos(uid).first { list -> list.any { it.id == photoId && !it.trashed } }
      }
    assertTrue(restoredList.any { it.id == photoId && !it.trashed })

    val batchDeleteResult =
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.permanentlyDeletePhotos(listOf(photoId, videoId)) }
    assertTrue(
      "permanentlyDeletePhotos failed: ${batchDeleteResult.exceptionOrNull()}",
      batchDeleteResult.isSuccess,
    )
  }

  @Test
  fun observePhotos_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = ErtTubeRepository(firestore, auth)
    val addPhotoResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.addPhoto("Alice Private Frame", null, PhotoAlbum.PORTRAITS, null)
      }
    assertTrue(
      "Alice addPhoto failed for uid=$aliceUid authUid=${auth.currentUser?.uid}: ${addPhotoResult.exceptionOrNull()}",
      addPhotoResult.isSuccess,
    )
    val photoId = addPhotoResult.getOrThrow()

    signInTestUser(BOB_EMAIL)
    val bobError =
      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          firestore
            .collection("users")
            .document(aliceUid)
            .collection("photos")
            .document(photoId)
            .get()
            .await()
        }
        null
      } catch (e: Exception) {
        e.unwrapFirestoreException()
      }

    assertNotNull(bobError)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobError?.code)
  }

  @Test
  fun observePhotos_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = ErtTubeRepository(firestore, auth)

    val exception =
      try {
        withTimeout(FLOW_TIMEOUT_MS) { repo.observePhotos("alice_uid_placeholder").first() }
        null
      } catch (e: Exception) {
        e
      }

    assertNotNull(exception)
    val firestoreException = exception?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $exception", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_erttube@test.com"
    const val BOB_EMAIL = "bob_erttube@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
