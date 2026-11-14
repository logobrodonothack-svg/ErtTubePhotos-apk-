package com.example.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem

enum class GalleryFilter(val label: String) {
  ALL("All"),
  VIDEOS("Videos"),
  FAVORITES("Favorites"),
  REELS("Reels"),
  PORTRAITS("Portraits"),
  STUDIO("Studio"),
  TRAVEL("Travel"),
}

@Composable
fun PhotosTab(
  photosUiState: UiState<List<PhotoItem>>,
  activeFilter: GalleryFilter,
  backupEnabled: Boolean,
  backupWifiOnly: Boolean,
  onFilterSelected: (GalleryFilter) -> Unit,
  onPhotoClick: (PhotoItem) -> Unit,
  onFavoriteToggle: (PhotoItem) -> Unit,
  onAddPhotoClick: () -> Unit,
  onImportGooglePhotosClick: () -> Unit,
  onOpenBackupSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    BackupStatusBanner(
      backupEnabled = backupEnabled,
      backupWifiOnly = backupWifiOnly,
      onOpenSettings = onOpenBackupSettings,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilledTonalButton(
        onClick = onImportGooglePhotosClick,
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .testTag("photos_import_google_photos_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.CloudDownload,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.import_google_photos_btn))
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(GalleryFilter.entries) { filter ->
        val selected = activeFilter == filter
        FilterChip(
          selected = selected,
          onClick = { onFilterSelected(filter) },
          label = { Text(filter.label) },
          leadingIcon =
            when (filter) {
              GalleryFilter.FAVORITES -> {
                {
                  Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              GalleryFilter.VIDEOS -> {
                {
                  Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              else -> null
            },
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("album_filter_${filter.name.lowercase()}"),
        )
      }
    }

    when (photosUiState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = stringResource(R.string.gallery_loading),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = photosUiState.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
      is UiState.Success -> {
        val filteredPhotos =
          remember(photosUiState.data, activeFilter) {
            val active = photosUiState.data.filter { !it.trashed }
            when (activeFilter) {
              GalleryFilter.ALL -> active
              GalleryFilter.VIDEOS -> active.filter { it.isVideoItem }
              GalleryFilter.FAVORITES -> active.filter { it.favorite }
              GalleryFilter.REELS -> active.filter { it.album == PhotoAlbum.REELS.label }
              GalleryFilter.PORTRAITS ->
                active.filter { it.album == PhotoAlbum.PORTRAITS.label }
              GalleryFilter.STUDIO ->
                active.filter { it.album == PhotoAlbum.STUDIO.label }
              GalleryFilter.TRAVEL ->
                active.filter { it.album == PhotoAlbum.TRAVEL.label }
            }
          }

        if (filteredPhotos.isEmpty()) {
          EmptyGalleryState(
            onAddClick = onAddPhotoClick,
            onImportGooglePhotosClick = onImportGooglePhotosClick,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(filteredPhotos, key = { it.id }) { photo ->
              PhotoCard(
                photo = photo,
                onClick = { onPhotoClick(photo) },
                onFavoriteToggle = { onFavoriteToggle(photo) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BackupStatusBanner(
  backupEnabled: Boolean,
  backupWifiOnly: Boolean,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val wifiSuffix = if (backupWifiOnly) stringResource(R.string.backup_wifi_suffix) else ""
  Surface(
    shape = RoundedCornerShape(18.dp),
    color =
      if (backupEnabled) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      },
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (backupEnabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          } else {
            MaterialTheme.colorScheme.outlineVariant
          },
      ),
    modifier = modifier.fillMaxWidth().testTag("photos_backup_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color =
          if (backupEnabled) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
          },
        modifier = Modifier.size(36.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector =
              if (backupEnabled) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
            contentDescription = null,
            tint =
              if (backupEnabled) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text =
            if (backupEnabled) {
              stringResource(R.string.backup_banner_on_title)
            } else {
              stringResource(R.string.backup_banner_off_title)
            },
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            if (backupEnabled) {
              stringResource(R.string.backup_banner_on_subtitle, wifiSuffix)
            } else {
              stringResource(R.string.backup_banner_off_subtitle)
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      FilledTonalButton(
        onClick = onOpenSettings,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("photos_backup_settings_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Settings,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.backup_open_settings))
      }
    }
  }
}

@Composable
internal fun EmptyGalleryState(
  onAddClick: () -> Unit,
  onImportGooglePhotosClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
    Column(
      modifier = Modifier.widthIn(max = 360.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(80.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(38.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = stringResource(R.string.gallery_empty_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.gallery_empty_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(22.dp))
      Button(
        onClick = onAddClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("empty_add_photo_button"),
      ) {
        Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.gallery_empty_cta))
      }
      Spacer(modifier = Modifier.height(10.dp))
      FilledTonalButton(
        onClick = onImportGooglePhotosClick,
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .testTag("empty_import_google_photos_button"),
      ) {
        Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.import_google_photos_btn))
      }
    }
  }
}
