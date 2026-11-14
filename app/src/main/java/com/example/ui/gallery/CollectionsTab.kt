package com.example.ui.gallery

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Portrait
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import java.io.File

enum class CollectionCategory(
  val key: String,
  val icon: ImageVector,
) {
  VIDEOS("videos", Icons.Rounded.Videocam),
  FAVORITES("favorites", Icons.Rounded.Favorite),
  BIN("bin", Icons.Rounded.DeleteOutline),
  REELS("reels", Icons.Rounded.Movie),
  PORTRAITS("portraits", Icons.Rounded.Portrait),
  STUDIO("studio", Icons.Rounded.Camera),
  TRAVEL("travel", Icons.Rounded.Explore),
  DEVICE_IMPORTS("device_imports", Icons.Rounded.PhoneAndroid),
}

@Composable
fun CollectionsTab(
  photosUiState: UiState<List<PhotoItem>>,
  selectedCategory: CollectionCategory?,
  isMutating: Boolean = false,
  onSelectCategory: (CollectionCategory?) -> Unit,
  onPhotoClick: (PhotoItem) -> Unit,
  onFavoriteToggle: (PhotoItem) -> Unit,
  onRestoreFromBin: (List<String>) -> Unit = {},
  onPermanentlyDelete: (List<PhotoItem>) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (selectedCategory != null) {
    BackHandler { onSelectCategory(null) }
  }

  val allPhotos = (photosUiState as? UiState.Success)?.data.orEmpty()

  if (photosUiState is UiState.Loading) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
    return
  }

  if (selectedCategory == CollectionCategory.BIN) {
    val binnedPhotos = remember(allPhotos) { allPhotos.filter { it.trashed } }
    BinView(
      binnedPhotos = binnedPhotos,
      isMutating = isMutating,
      onBack = { onSelectCategory(null) },
      onRestoreItems = onRestoreFromBin,
      onPermanentlyDeleteItems = onPermanentlyDelete,
      modifier = modifier,
    )
    return
  }

  if (selectedCategory != null) {
    val categoryTitle = categoryTitle(selectedCategory)
    val matchingPhotos =
      remember(allPhotos, selectedCategory) {
        filterByCategory(allPhotos, selectedCategory)
      }

    Column(modifier = modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { onSelectCategory(null) },
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("collection_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.collection_back),
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = categoryTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text =
              if (matchingPhotos.size == 1) {
                stringResource(R.string.collection_single_item)
              } else {
                stringResource(R.string.collection_item_count, matchingPhotos.size)
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (matchingPhotos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(R.string.collection_empty_detail),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 164.dp),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(matchingPhotos, key = { it.id }) { photo ->
            PhotoCard(
              photo = photo,
              onClick = { onPhotoClick(photo) },
              onFavoriteToggle = { onFavoriteToggle(photo) },
            )
          }
        }
      }
    }
  } else {
    Column(modifier = modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
          text = stringResource(R.string.collection_header_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = stringResource(R.string.collection_header_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag("collections_grid"),
      ) {
        items(CollectionCategory.entries, key = { it.key }) { category ->
          val itemsInCategory =
            remember(allPhotos, category) { filterByCategory(allPhotos, category) }
          CollectionCard(
            category = category,
            title = categoryTitle(category),
            photos = itemsInCategory,
            onClick = { onSelectCategory(category) },
          )
        }
      }
    }
  }
}

@Composable
private fun CollectionCard(
  category: CollectionCategory,
  title: String,
  photos: List<PhotoItem>,
  onClick: () -> Unit,
) {
  val coverFile =
    remember(photos) {
      photos.firstNotNullOfOrNull { item ->
        if (item.isVideoItem) {
          videoThumbnailFile(item.localPath)
        } else {
          item.localPath?.let { File(it) }?.takeIf { it.exists() }
        }
      }
    }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onClick)
        .testTag("collection_card_${category.key}"),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column {
      Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.25f),
        contentAlignment = Alignment.Center,
      ) {
        if (coverFile != null) {
          AsyncImage(
            model = coverFile,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          if (category == CollectionCategory.VIDEOS) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.PlayArrow,
                  contentDescription = null,
                  tint = Color.White,
                )
              }
            }
          }
        } else {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.linearGradient(
                    colors =
                      listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                      )
                  )
                ),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              modifier = Modifier.size(52.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = category.icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(26.dp),
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = category.icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              if (photos.size == 1) {
                stringResource(R.string.collection_single_item)
              } else {
                stringResource(R.string.collection_item_count, photos.size)
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun categoryTitle(category: CollectionCategory): String {
  return when (category) {
    CollectionCategory.VIDEOS -> stringResource(R.string.videos_filter_label)
    CollectionCategory.FAVORITES -> stringResource(R.string.collection_favorites)
    CollectionCategory.BIN -> stringResource(R.string.bin_title)
    CollectionCategory.REELS -> PhotoAlbum.REELS.label
    CollectionCategory.PORTRAITS -> PhotoAlbum.PORTRAITS.label
    CollectionCategory.STUDIO -> PhotoAlbum.STUDIO.label
    CollectionCategory.TRAVEL -> PhotoAlbum.TRAVEL.label
    CollectionCategory.DEVICE_IMPORTS -> stringResource(R.string.collection_device_imports)
  }
}

private fun filterByCategory(
  photos: List<PhotoItem>,
  category: CollectionCategory,
): List<PhotoItem> {
  if (category == CollectionCategory.BIN) {
    return photos.filter { it.trashed }
  }
  val active = photos.filter { !it.trashed }
  return when (category) {
    CollectionCategory.VIDEOS -> active.filter { it.isVideoItem }
    CollectionCategory.FAVORITES -> active.filter { it.favorite }
    CollectionCategory.BIN -> photos.filter { it.trashed }
    CollectionCategory.REELS -> active.filter { it.album == PhotoAlbum.REELS.label }
    CollectionCategory.PORTRAITS -> active.filter { it.album == PhotoAlbum.PORTRAITS.label }
    CollectionCategory.STUDIO -> active.filter { it.album == PhotoAlbum.STUDIO.label }
    CollectionCategory.TRAVEL -> active.filter { it.album == PhotoAlbum.TRAVEL.label }
    CollectionCategory.DEVICE_IMPORTS -> active.filter { !it.localPath.isNullOrBlank() }
  }
}
