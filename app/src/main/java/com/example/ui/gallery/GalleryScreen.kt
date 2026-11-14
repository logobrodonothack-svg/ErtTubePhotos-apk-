package com.example.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.AccountHistoryStore
import com.example.data.local.SavedAccount
import com.example.data.model.MediaType
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.UserProfile
import com.example.data.repository.ErtTubeRepository
import com.example.ui.auth.PendingSignUpProfile
import com.example.ui.auth.signOutUser
import com.example.ui.auth.switchOrAddGoogleAccount
import com.example.ui.menu.ManageStorageSheet
import com.example.ui.menu.MenuSheet
import com.example.ui.menu.PhotosSettingsSheet
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

enum class GalleryDestination(
  val titleRes: Int,
  val icon: ImageVector,
  val testTag: String,
) {
  PHOTOS(R.string.nav_photos, Icons.Rounded.PhotoLibrary, "nav_photos"),
  COLLECTION(R.string.nav_collection, Icons.Rounded.CollectionsBookmark, "nav_collection"),
  SEARCH(R.string.nav_search, Icons.Rounded.Search, "nav_search"),
}

private enum class ActiveMenuSheet {
  NONE,
  MENU,
  PHOTOS_SETTINGS,
  MANAGE_STORAGE,
  IMPORT_GOOGLE_PHOTOS,
  PROFILE,
}

@Composable
fun GalleryScreen(
  currentUser: FirebaseUser,
  pendingSignUpProfile: PendingSignUpProfile?,
  onConsumePendingProfile: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val accountHistoryStore = remember(context) { AccountHistoryStore(context) }

  val viewModel: GalleryViewModel =
    viewModel(
      key = currentUser.uid,
      factory =
        viewModelFactory {
          initializer {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            GalleryViewModel(ErtTubeRepository(db), currentUser.uid)
          }
        },
    )

  LaunchedEffect(currentUser.uid, pendingSignUpProfile) {
    val fallbackName =
      pendingSignUpProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser.displayName?.takeIf { it.isNotBlank() }
        ?: "ErtTube Creator"
    accountHistoryStore.recordAccount(
      uid = currentUser.uid,
      displayName = fallbackName,
      email = currentUser.email,
      handle = pendingSignUpProfile?.handle,
    )
    viewModel.syncInitialProfile(
      displayName = fallbackName,
      handle = pendingSignUpProfile?.handle?.takeIf { it.isNotBlank() },
      email = currentUser.email,
      preferredGalleryMode = pendingSignUpProfile?.preferredGalleryMode,
      overwriteExistingPreferences = pendingSignUpProfile != null,
    )
    if (pendingSignUpProfile != null) {
      onConsumePendingProfile()
    }
  }

  val profile by viewModel.profileState.collectAsStateWithLifecycle()
  val photosUiState by viewModel.photosState.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

  LaunchedEffect(currentUser.uid, profile?.displayName, profile?.handle) {
    profile?.let { loaded ->
      accountHistoryStore.recordAccount(
        uid = currentUser.uid,
        displayName = loaded.displayName,
        email = loaded.email ?: currentUser.email,
        handle = loaded.handle,
      )
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(actionMessage) {
    actionMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearActionMessage()
    }
  }

  val cancelledNotice = stringResource(R.string.menu_account_picker_cancelled)
  var isAccountSwitching by remember { mutableStateOf(false) }

  val triggerAccountPicker: () -> Unit = {
    isAccountSwitching = true
    switchOrAddGoogleAccount(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { newUser ->
        isAccountSwitching = false
        accountHistoryStore.recordAccount(
          uid = newUser.uid,
          displayName = newUser.displayName,
          email = newUser.email,
        )
      },
      onAuthError = { err ->
        isAccountSwitching = false
        viewModel.notifyMessage(err)
      },
      scope = coroutineScope,
      onAuthCancelled = {
        isAccountSwitching = false
        viewModel.notifyMessage(cancelledNotice)
      },
    )
  }

  GalleryScreenContent(
    currentUserUid = currentUser.uid,
    currentUserDisplayName = currentUser.displayName,
    currentUserEmail = currentUser.email,
    profile = profile,
    photosUiState = photosUiState,
    searchQuery = searchQuery,
    isMutating = isMutating,
    isAccountSwitching = isAccountSwitching,
    otherAccounts =
      remember(currentUser.uid, profile) {
        accountHistoryStore.getOtherAccounts(currentUser.uid)
      },
    snackbarHostState = snackbarHostState,
    onSearchQueryChange = viewModel::onSearchQueryChange,
    onAddPhoto = viewModel::addPhoto,
    onImportGooglePhotos = viewModel::importGooglePhotos,
    onUpdatePhoto = viewModel::updatePhoto,
    onDeletePhoto = viewModel::moveToBin,
    onMoveMultipleToBin = viewModel::moveMultipleToBin,
    onRestoreFromBin = viewModel::restoreFromBin,
    onPermanentlyDelete = viewModel::permanentlyDelete,
    onToggleFavorite = viewModel::toggleFavorite,
    onSaveProfile = viewModel::updateProfile,
    onUpdateBackupSettings = viewModel::updateBackupSettings,
    onClearLocalCache = {
      viewModel.clearLocalPhotoCache(context.filesDir)
    },
    onSwitchAccount = triggerAccountPicker,
    onAddAccount = triggerAccountPicker,
    onSwitchToSavedAccount = { _ -> triggerAccountPicker() },
    onNotifyMessage = viewModel::notifyMessage,
    onSignOut = {
      signOutUser(credentialManager = credentialManager, scope = coroutineScope)
    },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreenContent(
  currentUserUid: String,
  currentUserDisplayName: String?,
  currentUserEmail: String?,
  profile: UserProfile?,
  photosUiState: UiState<List<PhotoItem>>,
  searchQuery: String,
  isMutating: Boolean,
  isAccountSwitching: Boolean,
  otherAccounts: List<SavedAccount>,
  snackbarHostState: SnackbarHostState,
  onSearchQueryChange: (String) -> Unit,
  onAddPhoto: (String, String?, PhotoAlbum, String?, Boolean, MediaType, Long?) -> Unit,
  onImportGooglePhotos: (List<ImportedPhotoInput>, PhotoAlbum, String?, Boolean) -> Unit = { _, _, _, _ -> },
  onUpdatePhoto: (String, String, String, PhotoAlbum, Boolean) -> Unit,
  onDeletePhoto: (String) -> Unit,
  onMoveMultipleToBin: (List<String>) -> Unit = {},
  onRestoreFromBin: (List<String>) -> Unit = {},
  onPermanentlyDelete: (List<PhotoItem>) -> Unit = {},
  onToggleFavorite: (PhotoItem) -> Unit,
  onSaveProfile: (String, String, String, com.example.data.model.GalleryMode) -> Unit,
  onUpdateBackupSettings: (Boolean, Boolean) -> Unit,
  onClearLocalCache: () -> Unit,
  onSwitchAccount: () -> Unit,
  onAddAccount: () -> Unit,
  onSwitchToSavedAccount: (SavedAccount) -> Unit,
  onNotifyMessage: (String) -> Unit = {},
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedDestination by rememberSaveable { mutableStateOf(GalleryDestination.PHOTOS) }
  var activeFilter by rememberSaveable { mutableStateOf(GalleryFilter.ALL) }
  var selectedCollection by rememberSaveable { mutableStateOf<CollectionCategory?>(null) }
  var showAddSheet by remember { mutableStateOf(false) }
  var selectedPhoto by remember { mutableStateOf<PhotoItem?>(null) }
  var playingVideo by remember { mutableStateOf<PhotoItem?>(null) }
  var activeMenuSheet by remember { mutableStateOf(ActiveMenuSheet.NONE) }
  var cacheRefreshTick by remember { mutableIntStateOf(0) }

  val handleMediaClick: (PhotoItem) -> Unit = { item ->
    if (item.isVideoItem) {
      playingVideo = item
    } else {
      selectedPhoto = item
    }
  }

  if (selectedDestination != GalleryDestination.PHOTOS &&
    (selectedDestination != GalleryDestination.COLLECTION || selectedCollection == null)
  ) {
    BackHandler { selectedDestination = GalleryDestination.PHOTOS }
  }

  val allPhotos = (photosUiState as? UiState.Success)?.data.orEmpty()
  val storageStats =
    remember(allPhotos, cacheRefreshTick) {
      GalleryViewModel.computeStorageStats(context.filesDir, allPhotos)
    }

  val backupEnabled = profile?.backupEnabled ?: false
  val backupWifiOnly = profile?.backupWifiOnly ?: true

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 840.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          modifier = Modifier.fillMaxHeight().testTag("gallery_navigation_rail"),
          header = {
            FloatingActionButton(
              onClick = { showAddSheet = true },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(top = 8.dp).testTag("rail_add_photo_fab"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.gallery_add_photo),
              )
            }
          },
        ) {
          GalleryDestination.entries.forEach { destination ->
            NavigationRailItem(
              selected = selectedDestination == destination,
              onClick = { selectedDestination = destination },
              icon = {
                Icon(
                  imageVector = destination.icon,
                  contentDescription = stringResource(destination.titleRes),
                )
              },
              label = { Text(stringResource(destination.titleRes)) },
              modifier = Modifier.testTag(destination.testTag),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          TopAppBar(
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
            title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                  painter = painterResource(id = R.drawable.erttube_logo),
                  contentDescription = stringResource(R.string.logo_content_description),
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      profile?.handle?.takeIf { it.isNotBlank() }
                        ?: profile?.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUserDisplayName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.gallery_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            },
            actions = {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color =
                  if (backupEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  },
                modifier =
                  Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable { activeMenuSheet = ActiveMenuSheet.PHOTOS_SETTINGS }
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_backup_chip"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector =
                      if (backupEnabled) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint =
                      if (backupEnabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                      } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      },
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text =
                      if (backupEnabled) {
                        stringResource(R.string.backup_status_on)
                      } else {
                        stringResource(R.string.backup_status_off)
                      },
                    style = MaterialTheme.typography.labelMedium,
                    color =
                      if (backupEnabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                      } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      },
                  )
                }
              }

              Spacer(modifier = Modifier.width(4.dp))

              IconButton(
                onClick = { activeMenuSheet = ActiveMenuSheet.MENU },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("menu_button"),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(36.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.Menu,
                      contentDescription = stringResource(R.string.cd_open_menu),
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            },
          )
        },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar(modifier = Modifier.testTag("gallery_bottom_nav")) {
              GalleryDestination.entries.forEach { destination ->
                NavigationBarItem(
                  selected = selectedDestination == destination,
                  onClick = { selectedDestination = destination },
                  icon = {
                    Icon(
                      imageVector = destination.icon,
                      contentDescription = stringResource(destination.titleRes),
                    )
                  },
                  label = { Text(stringResource(destination.titleRes)) },
                  modifier = Modifier.testTag(destination.testTag),
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (!useNavigationRail && selectedDestination == GalleryDestination.PHOTOS) {
            FloatingActionButton(
              onClick = { showAddSheet = true },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("add_photo_fab"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.gallery_add_photo),
              )
            }
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          when (selectedDestination) {
            GalleryDestination.PHOTOS ->
              PhotosTab(
                photosUiState = photosUiState,
                activeFilter = activeFilter,
                backupEnabled = backupEnabled,
                backupWifiOnly = backupWifiOnly,
                onFilterSelected = { activeFilter = it },
                onPhotoClick = handleMediaClick,
                onFavoriteToggle = onToggleFavorite,
                onAddPhotoClick = { showAddSheet = true },
                onImportGooglePhotosClick = {
                  activeMenuSheet = ActiveMenuSheet.IMPORT_GOOGLE_PHOTOS
                },
                onOpenBackupSettings = { activeMenuSheet = ActiveMenuSheet.PHOTOS_SETTINGS },
              )
            GalleryDestination.COLLECTION ->
              CollectionsTab(
                photosUiState = photosUiState,
                selectedCategory = selectedCollection,
                isMutating = isMutating,
                onSelectCategory = { selectedCollection = it },
                onPhotoClick = handleMediaClick,
                onFavoriteToggle = onToggleFavorite,
                onRestoreFromBin = onRestoreFromBin,
                onPermanentlyDelete = onPermanentlyDelete,
              )
            GalleryDestination.SEARCH ->
              SearchTab(
                photosUiState = photosUiState,
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onPhotoClick = handleMediaClick,
                onFavoriteToggle = onToggleFavorite,
              )
          }
        }
      }
    }
  }

  if (showAddSheet) {
    PhotoEditorSheet(
      initialPhoto = null,
      isSaving = isMutating,
      onDismiss = { showAddSheet = false },
      onSave = { title, caption, album, localPath, favorite, mediaType, durationMs ->
        onAddPhoto(title, caption, album, localPath, favorite, mediaType, durationMs)
        showAddSheet = false
      },
      onDelete = null,
    )
  }

  playingVideo?.let { video ->
    val liveVideo = allPhotos.firstOrNull { it.id == video.id } ?: video
    VideoPlayerSheet(
      video = liveVideo,
      onDismiss = { playingVideo = null },
      onToggleFavorite = { onToggleFavorite(liveVideo) },
      onEditDetails = {
        selectedPhoto = liveVideo
        playingVideo = null
      },
      onDelete = {
        onDeletePhoto(liveVideo.id)
        playingVideo = null
      },
    )
  }

  selectedPhoto?.let { editing ->
    PhotoEditorSheet(
      initialPhoto = editing,
      isSaving = isMutating,
      onDismiss = { selectedPhoto = null },
      onSave = { title, caption, album, _, favorite, _, _ ->
        onUpdatePhoto(editing.id, title, caption ?: "", album, favorite)
        selectedPhoto = null
      },
      onDelete = {
        onDeletePhoto(editing.id)
        selectedPhoto = null
      },
    )
  }

  when (activeMenuSheet) {
    ActiveMenuSheet.NONE -> Unit
    ActiveMenuSheet.MENU -> {
      MenuSheet(
        accountDisplayName = currentUserDisplayName,
        accountEmail = currentUserEmail,
        profile = profile,
        storageStats = storageStats,
        otherAccounts = otherAccounts,
        isAccountBusy = isAccountSwitching,
        onDismiss = { activeMenuSheet = ActiveMenuSheet.NONE },
        onOpenManageStorage = { activeMenuSheet = ActiveMenuSheet.MANAGE_STORAGE },
        onOpenPhotosSettings = { activeMenuSheet = ActiveMenuSheet.PHOTOS_SETTINGS },
        onOpenImportGooglePhotos = { activeMenuSheet = ActiveMenuSheet.IMPORT_GOOGLE_PHOTOS },
        onOpenEditProfile = { activeMenuSheet = ActiveMenuSheet.PROFILE },
        onSwitchAccount = {
          activeMenuSheet = ActiveMenuSheet.NONE
          onSwitchAccount()
        },
        onAddAccount = {
          activeMenuSheet = ActiveMenuSheet.NONE
          onAddAccount()
        },
        onSwitchToSavedAccount = { saved ->
          activeMenuSheet = ActiveMenuSheet.NONE
          onSwitchToSavedAccount(saved)
        },
        onSignOut = {
          activeMenuSheet = ActiveMenuSheet.NONE
          onSignOut()
        },
      )
    }
    ActiveMenuSheet.PHOTOS_SETTINGS -> {
      PhotosSettingsSheet(
        backupEnabled = backupEnabled,
        backupWifiOnly = backupWifiOnly,
        storageStats = storageStats,
        isSaving = isMutating,
        onDismiss = { activeMenuSheet = ActiveMenuSheet.NONE },
        onBackToMenu = { activeMenuSheet = ActiveMenuSheet.MENU },
        onBackupChanged = { enabled -> onUpdateBackupSettings(enabled, backupWifiOnly) },
        onWifiOnlyChanged = { wifiOnly -> onUpdateBackupSettings(backupEnabled, wifiOnly) },
        onOpenManageStorage = { activeMenuSheet = ActiveMenuSheet.MANAGE_STORAGE },
      )
    }
    ActiveMenuSheet.MANAGE_STORAGE -> {
      ManageStorageSheet(
        storageStats = storageStats,
        photos = allPhotos,
        isSaving = isMutating,
        onDismiss = { activeMenuSheet = ActiveMenuSheet.NONE },
        onBackToMenu = { activeMenuSheet = ActiveMenuSheet.MENU },
        onClearLocalCache = {
          onClearLocalCache()
          cacheRefreshTick++
        },
        onMoveSelectedToBin = onMoveMultipleToBin,
        onPermanentlyDeleteSelected = { items ->
          onPermanentlyDelete(items)
          cacheRefreshTick++
        },
        onOpenBackupSettings = { activeMenuSheet = ActiveMenuSheet.PHOTOS_SETTINGS },
      )
    }
    ActiveMenuSheet.IMPORT_GOOGLE_PHOTOS -> {
      val importCancelledNotice = stringResource(R.string.import_google_photos_cancelled)
      ImportGooglePhotosSheet(
        isSaving = isMutating,
        onDismiss = { activeMenuSheet = ActiveMenuSheet.NONE },
        onImportPhotos = { items, album, note, markFavorite ->
          onImportGooglePhotos(items, album, note, markFavorite)
          activeMenuSheet = ActiveMenuSheet.NONE
        },
        onCancelled = {
          onNotifyMessage(importCancelledNotice)
        },
      )
    }
    ActiveMenuSheet.PROFILE -> {
      val activePhotos = allPhotos.filter { !it.trashed }
      val favCount = activePhotos.count { it.favorite }
      ProfileSheet(
        accountDisplayName = currentUserDisplayName,
        accountEmail = currentUserEmail,
        profile = profile,
        totalPhotos = activePhotos.size,
        favoritePhotos = favCount,
        isSaving = isMutating,
        onDismiss = { activeMenuSheet = ActiveMenuSheet.NONE },
        onSaveProfile = { name, handle, bio, mode ->
          onSaveProfile(name, handle, bio, mode)
          activeMenuSheet = ActiveMenuSheet.NONE
        },
        onSignOut = {
          activeMenuSheet = ActiveMenuSheet.NONE
          onSignOut()
        },
      )
    }
  }
}
