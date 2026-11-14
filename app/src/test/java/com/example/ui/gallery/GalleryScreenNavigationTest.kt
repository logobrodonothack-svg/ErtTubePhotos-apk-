package com.example.ui.gallery

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.local.SavedAccount
import com.example.data.model.MediaType
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.UserProfile
import com.example.ui.theme.ErtTubePhotosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class GalleryScreenNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun samplePhotos(): List<PhotoItem> =
    listOf(
      PhotoItem(
        id = "p1",
        userId = "u1",
        title = "Sunset Reel 01",
        caption = "Golden hour cinema still",
        album = PhotoAlbum.REELS.label,
        favorite = true,
      ),
      PhotoItem(
        id = "p2",
        userId = "u1",
        title = "Studio Portrait",
        caption = "Violet rim light",
        album = PhotoAlbum.PORTRAITS.label,
        favorite = false,
      ),
      PhotoItem(
        id = "v1",
        userId = "u1",
        title = "Sunset Reel Video",
        caption = "4K B-roll clip",
        album = PhotoAlbum.REELS.label,
        favorite = true,
        mediaType = MediaType.VIDEO.wireValue,
        durationMs = 18_500L,
      ),
    )

  @Test
  fun navigationBar_switchesBetweenPhotosCollectionAndSearch() {
    composeTestRule.setContent {
      ErtTubePhotosTheme {
        var searchQuery by remember { mutableStateOf("") }
        GalleryScreenContent(
          currentUserUid = "u1",
          currentUserDisplayName = "Alex Creator",
          currentUserEmail = "alex@example.com",
          profile = UserProfile(userId = "u1", displayName = "Alex Creator", handle = "@alex_ert"),
          photosUiState = UiState.Success(samplePhotos()),
          searchQuery = searchQuery,
          isMutating = false,
          isAccountSwitching = false,
          otherAccounts = emptyList(),
          snackbarHostState = remember { SnackbarHostState() },
          onSearchQueryChange = { searchQuery = it },
          onAddPhoto = { _, _, _, _, _, _, _ -> },
          onUpdatePhoto = { _, _, _, _, _ -> },
          onDeletePhoto = {},
          onToggleFavorite = {},
          onSaveProfile = { _, _, _, _ -> },
          onUpdateBackupSettings = { _, _ -> },
          onClearLocalCache = {},
          onSwitchAccount = {},
          onAddAccount = {},
          onSwitchToSavedAccount = {},
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gallery_bottom_nav").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_photos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_collection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_search").assertIsDisplayed()
    composeTestRule.onNodeWithTag("photo_card_p1").assertIsDisplayed()

    // Navigate to Collection Page
    composeTestRule.onNodeWithTag("nav_collection").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("collection_card_favorites").assertIsDisplayed()
    composeTestRule.onNodeWithTag("collections_grid").performScrollToNode(hasTestTag("collection_card_reels"))
    composeTestRule.onNodeWithTag("collection_card_reels").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("collection_back_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("photo_card_p1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("collection_back_button").performClick()
    composeTestRule.waitForIdle()

    // Navigate to Search Page and filter
    composeTestRule.onNodeWithTag("nav_search").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_query_input").performTextInput("Violet")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("photo_card_p2").assertIsDisplayed()
  }

  @Test
  fun menuPage_displaysManageStorageAndAccountsAndBackupToggle() {
    var latestBackupState = false
    composeTestRule.setContent {
      ErtTubePhotosTheme {
        var backupEnabled by remember { mutableStateOf(false) }
        GalleryScreenContent(
          currentUserUid = "u1",
          currentUserDisplayName = "Alex Creator",
          currentUserEmail = "alex@example.com",
          profile =
            UserProfile(
              userId = "u1",
              displayName = "Alex Creator",
              handle = "@alex_ert",
              backupEnabled = backupEnabled,
              backupWifiOnly = true,
            ),
          photosUiState = UiState.Success(samplePhotos()),
          searchQuery = "",
          isMutating = false,
          isAccountSwitching = false,
          otherAccounts =
            listOf(
              SavedAccount(
                uid = "u2",
                displayName = "Studio Secondary",
                email = "studio2@example.com",
              )
            ),
          snackbarHostState = remember { SnackbarHostState() },
          onSearchQueryChange = {},
          onAddPhoto = { _, _, _, _, _, _, _ -> },
          onUpdatePhoto = { _, _, _, _, _ -> },
          onDeletePhoto = {},
          onToggleFavorite = {},
          onSaveProfile = { _, _, _, _ -> },
          onUpdateBackupSettings = { enabled, _ ->
            backupEnabled = enabled
            latestBackupState = enabled
          },
          onClearLocalCache = {},
          onSwitchAccount = {},
          onAddAccount = {},
          onSwitchToSavedAccount = {},
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_button").performClick()
    composeTestRule.waitForIdle()

    // Verify Switch Account, Add Account, Other Accounts, and Manage Storage 1 MB / 1 TB
    composeTestRule.onNodeWithTag("switch_account_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("add_account_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("other_accounts_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_other_account_u2").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_manage_storage_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_storage_ratio", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("1 MB / 1 TB", useUnmergedTree = true).assertIsDisplayed()

    // Open Manage Storage sheet
    composeTestRule.onNodeWithTag("menu_manage_storage_card").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("manage_storage_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("storage_detail_ratio").assertIsDisplayed()

    // Open Photos Settings from Manage Storage and toggle Backup Off -> On
    composeTestRule.onNodeWithTag("storage_open_backup_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("photos_settings_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("backup_toggle_switch").performScrollTo().assertIsOff().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("backup_toggle_switch").performScrollTo().assertIsOn()
    assertEquals(true, latestBackupState)
  }

  @Test
  fun importGooglePhotos_opensSheetFromPhotosAndMenu() {
    composeTestRule.setContent {
      ErtTubePhotosTheme {
        GalleryScreenContent(
          currentUserUid = "u1",
          currentUserDisplayName = "Alex Creator",
          currentUserEmail = "alex@example.com",
          profile = UserProfile(userId = "u1", displayName = "Alex Creator"),
          photosUiState = UiState.Success(samplePhotos()),
          searchQuery = "",
          isMutating = false,
          isAccountSwitching = false,
          otherAccounts = emptyList(),
          snackbarHostState = remember { SnackbarHostState() },
          onSearchQueryChange = {},
          onAddPhoto = { _, _, _, _, _, _, _ -> },
          onUpdatePhoto = { _, _, _, _, _ -> },
          onDeletePhoto = {},
          onToggleFavorite = {},
          onSaveProfile = { _, _, _, _ -> },
          onUpdateBackupSettings = { _, _ -> },
          onClearLocalCache = {},
          onSwitchAccount = {},
          onAddAccount = {},
          onSwitchToSavedAccount = {},
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Open Import Google Photos directly from Photos page
    composeTestRule.onNodeWithTag("photos_import_google_photos_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("import_google_photos_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("import_mode_videos_only").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("import_album_portraits").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("import_favorite_switch").performScrollTo().performClick()
    composeTestRule
      .onNodeWithTag("launch_google_photos_picker_button")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun videosFilterAndCollectionAndPlayerSheet_workProperly() {
    composeTestRule.setContent {
      ErtTubePhotosTheme {
        GalleryScreenContent(
          currentUserUid = "u1",
          currentUserDisplayName = "Alex Creator",
          currentUserEmail = "alex@example.com",
          profile = UserProfile(userId = "u1", displayName = "Alex Creator"),
          photosUiState = UiState.Success(samplePhotos()),
          searchQuery = "",
          isMutating = false,
          isAccountSwitching = false,
          otherAccounts = emptyList(),
          snackbarHostState = remember { SnackbarHostState() },
          onSearchQueryChange = {},
          onAddPhoto = { _, _, _, _, _, _, _ -> },
          onUpdatePhoto = { _, _, _, _, _ -> },
          onDeletePhoto = {},
          onToggleFavorite = {},
          onSaveProfile = { _, _, _, _ -> },
          onUpdateBackupSettings = { _, _ -> },
          onClearLocalCache = {},
          onSwitchAccount = {},
          onAddAccount = {},
          onSwitchToSavedAccount = {},
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // 1. Videos filter chip filters down to video items with duration pill and play badge
    composeTestRule.onNodeWithTag("album_filter_videos").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("photo_card_v1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_play_badge_v1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_duration_badge_v1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("0:18", useUnmergedTree = true).assertIsDisplayed()

    // 2. Collection tab shows Videos collection card and opens video item
    composeTestRule.onNodeWithTag("nav_collection").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("collection_card_videos").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("photo_card_v1").assertIsDisplayed()

    // 3. Clicking a video opens VideoPlayerSheet, and Edit Details transitions to PhotoEditorSheet
    composeTestRule.onNodeWithTag("photo_card_v1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("video_player_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_player_edit_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("photo_title_input").assertIsDisplayed()
  }

  @Test
  fun collectionBinPage_displaysBinnedItems_supportsRestoreAndPermanentDelete() {
    val restoredIds = mutableListOf<List<String>>()
    val deletedItems = mutableListOf<List<PhotoItem>>()
    val binnedItem =
      PhotoItem(
        id = "b1",
        userId = "u1",
        title = "Trashed Studio Take",
        caption = "Outtake clip",
        album = PhotoAlbum.STUDIO.label,
        trashed = true,
      )

    composeTestRule.setContent {
      ErtTubePhotosTheme {
        GalleryScreenContent(
          currentUserUid = "u1",
          currentUserDisplayName = "Alex Creator",
          currentUserEmail = "alex@example.com",
          profile = UserProfile(userId = "u1", displayName = "Alex Creator"),
          photosUiState = UiState.Success(samplePhotos() + binnedItem),
          searchQuery = "",
          isMutating = false,
          isAccountSwitching = false,
          otherAccounts = emptyList(),
          snackbarHostState = remember { SnackbarHostState() },
          onSearchQueryChange = {},
          onAddPhoto = { _, _, _, _, _, _, _ -> },
          onUpdatePhoto = { _, _, _, _, _ -> },
          onDeletePhoto = {},
          onRestoreFromBin = { ids -> restoredIds.add(ids) },
          onPermanentlyDelete = { items -> deletedItems.add(items) },
          onToggleFavorite = {},
          onSaveProfile = { _, _, _, _ -> },
          onUpdateBackupSettings = { _, _ -> },
          onClearLocalCache = {},
          onSwitchAccount = {},
          onAddAccount = {},
          onSwitchToSavedAccount = {},
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Open Collection tab -> Bin card -> Bin page
    composeTestRule.onNodeWithTag("nav_collection").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("collections_grid").performScrollToNode(hasTestTag("collection_card_bin"))
    composeTestRule.onNodeWithTag("collection_card_bin").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("bin_page").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bin_item_card_b1").assertIsDisplayed()

    // Test Restore action
    composeTestRule.onNodeWithTag("bin_restore_b1", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    assertEquals(listOf(listOf("b1")), restoredIds)

    // Test Permanent Delete action via confirmation dialog
    composeTestRule.onNodeWithTag("bin_delete_b1", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bin_confirm_delete_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, deletedItems.size)
    assertEquals(listOf("b1"), deletedItems.first().map { it.id })
  }

  @Test
  fun manageStorage_selectMultiple_movesToBinAndDeletesForever() {
    val movedToBinBatches = mutableListOf<List<String>>()
    val permanentlyDeletedBatches = mutableListOf<List<PhotoItem>>()

    composeTestRule.setContent {
      ErtTubePhotosTheme {
        GalleryScreenContent(
          currentUserUid = "u1",
          currentUserDisplayName = "Alex Creator",
          currentUserEmail = "alex@example.com",
          profile = UserProfile(userId = "u1", displayName = "Alex Creator"),
          photosUiState = UiState.Success(samplePhotos()),
          searchQuery = "",
          isMutating = false,
          isAccountSwitching = false,
          otherAccounts = emptyList(),
          snackbarHostState = remember { SnackbarHostState() },
          onSearchQueryChange = {},
          onAddPhoto = { _, _, _, _, _, _, _ -> },
          onUpdatePhoto = { _, _, _, _, _ -> },
          onDeletePhoto = {},
          onMoveMultipleToBin = { ids -> movedToBinBatches.add(ids) },
          onPermanentlyDelete = { items -> permanentlyDeletedBatches.add(items) },
          onToggleFavorite = {},
          onSaveProfile = { _, _, _, _ -> },
          onUpdateBackupSettings = { _, _ -> },
          onClearLocalCache = {},
          onSwitchAccount = {},
          onAddAccount = {},
          onSwitchToSavedAccount = {},
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_manage_storage_card").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Activate Select Multiple and pick two items
    composeTestRule.onNodeWithTag("storage_select_multiple_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("storage_item_p1").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("storage_item_p2").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("storage_move_to_bin_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(listOf(listOf("p1", "p2")), movedToBinBatches)

    // Re-enter Select Multiple, Select All, and Delete Forever
    composeTestRule.onNodeWithTag("storage_select_multiple_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("storage_select_all_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("storage_delete_forever_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("storage_confirm_delete_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, permanentlyDeletedBatches.size)
    assertEquals(listOf("p1", "p2", "v1"), permanentlyDeletedBatches.first().map { it.id })
  }
}
