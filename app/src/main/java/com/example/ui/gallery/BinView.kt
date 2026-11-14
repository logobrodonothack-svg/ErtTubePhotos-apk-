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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PhotoItem
import java.io.File

@Composable
fun BinView(
  binnedPhotos: List<PhotoItem>,
  isMutating: Boolean,
  onBack: () -> Unit,
  onRestoreItems: (List<String>) -> Unit,
  onPermanentlyDeleteItems: (List<PhotoItem>) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  var selectMode by remember { mutableStateOf(false) }
  var selectedIds by remember(binnedPhotos) { mutableStateOf(emptySet<String>()) }
  var confirmEmptyBin by remember { mutableStateOf(false) }
  var itemsPendingPermanentDelete by remember { mutableStateOf<List<PhotoItem>?>(null) }

  Column(modifier = modifier.fillMaxSize().testTag("bin_page")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("collection_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.collection_back),
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.bin_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            if (binnedPhotos.size == 1) {
              stringResource(R.string.bin_subtitle_single)
            } else {
              stringResource(R.string.bin_subtitle_count, binnedPhotos.size)
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (binnedPhotos.isNotEmpty()) {
        TextButton(
          onClick = {
            selectMode = !selectMode
            if (!selectMode) selectedIds = emptySet()
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("bin_select_mode_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Checklist,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text =
              if (selectMode) {
                stringResource(R.string.bin_done_mode)
              } else {
                stringResource(R.string.bin_select_mode)
              }
          )
        }

        TextButton(
          onClick = { confirmEmptyBin = true },
          enabled = !isMutating,
          colors =
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("bin_empty_button"),
        ) {
          Text(text = stringResource(R.string.bin_empty_action))
        }
      }
    }

    if (selectMode && binnedPhotos.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.bin_selected_count, selectedIds.size),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(
                onClick = { selectedIds = binnedPhotos.map { it.id }.toSet() },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("bin_select_all_button"),
              ) {
                Text(text = stringResource(R.string.bin_select_all))
              }
              TextButton(
                onClick = { selectedIds = emptySet() },
                enabled = selectedIds.isNotEmpty(),
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("bin_clear_selection_button"),
              ) {
                Text(text = stringResource(R.string.bin_clear_selection))
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = {
                val ids = selectedIds.toList()
                onRestoreItems(ids)
                selectedIds = emptySet()
                selectMode = false
              },
              enabled = !isMutating && selectedIds.isNotEmpty(),
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.weight(1f).height(48.dp).testTag("bin_restore_selected_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.RestoreFromTrash,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.bin_restore))
            }

            Button(
              onClick = {
                itemsPendingPermanentDelete = binnedPhotos.filter { it.id in selectedIds }
              },
              enabled = !isMutating && selectedIds.isNotEmpty(),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                ),
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.weight(1f).height(48.dp).testTag("bin_delete_selected_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.bin_delete_short))
            }
          }
        }
      }
    }

    if (binnedPhotos.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
          modifier = Modifier.widthIn(max = 340.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(76.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = stringResource(R.string.bin_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.bin_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag("bin_items_grid"),
      ) {
        items(binnedPhotos, key = { it.id }) { item ->
          val isSelected = item.id in selectedIds
          BinItemCard(
            item = item,
            selectMode = selectMode,
            isSelected = isSelected,
            isMutating = isMutating,
            onToggleSelect = {
              if (!selectMode) selectMode = true
              selectedIds =
                if (isSelected) selectedIds - item.id else selectedIds + item.id
            },
            onRestore = { onRestoreItems(listOf(item.id)) },
            onDeleteForever = { itemsPendingPermanentDelete = listOf(item) },
          )
        }
      }
    }
  }

  if (confirmEmptyBin) {
    AlertDialog(
      onDismissRequest = { confirmEmptyBin = false },
      title = { Text(stringResource(R.string.bin_confirm_empty_title)) },
      text = {
        Text(stringResource(R.string.bin_confirm_empty_message, binnedPhotos.size))
      },
      confirmButton = {
        Button(
          onClick = {
            onPermanentlyDeleteItems(binnedPhotos)
            confirmEmptyBin = false
            selectMode = false
            selectedIds = emptySet()
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.testTag("bin_confirm_empty_button"),
        ) {
          Text(stringResource(R.string.bin_delete_forever))
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmEmptyBin = false }) {
          Text(stringResource(R.string.cancel_action))
        }
      },
    )
  }

  itemsPendingPermanentDelete?.let { pending ->
    AlertDialog(
      onDismissRequest = { itemsPendingPermanentDelete = null },
      title = { Text(stringResource(R.string.bin_confirm_delete_title)) },
      text = {
        Text(stringResource(R.string.bin_confirm_delete_message, pending.size))
      },
      confirmButton = {
        Button(
          onClick = {
            onPermanentlyDeleteItems(pending)
            itemsPendingPermanentDelete = null
            selectedIds = selectedIds - pending.map { it.id }.toSet()
            if (selectedIds.isEmpty()) selectMode = false
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.testTag("bin_confirm_delete_button"),
        ) {
          Text(stringResource(R.string.bin_delete_forever))
        }
      },
      dismissButton = {
        TextButton(onClick = { itemsPendingPermanentDelete = null }) {
          Text(stringResource(R.string.cancel_action))
        }
      },
    )
  }
}

@Composable
private fun BinItemCard(
  item: PhotoItem,
  selectMode: Boolean,
  isSelected: Boolean,
  isMutating: Boolean,
  onToggleSelect: () -> Unit,
  onRestore: () -> Unit,
  onDeleteForever: () -> Unit,
) {
  val thumbnailFile =
    remember(item.localPath, item.mediaType) {
      if (item.isVideoItem) {
        videoThumbnailFile(item.localPath)
      } else {
        item.localPath?.let { File(it) }?.takeIf { it.exists() }
      }
    }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onToggleSelect)
        .testTag("bin_item_card_${item.id}"),
    shape = RoundedCornerShape(20.dp),
    color =
      if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      },
    border =
      BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color =
          if (isSelected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.outlineVariant
          },
      ),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)) {
        if (thumbnailFile != null) {
          AsyncImage(
            model = thumbnailFile,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.linearGradient(
                    colors =
                      listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.primaryContainer,
                      )
                  )
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector =
                if (item.isVideoItem) Icons.Rounded.Videocam else Icons.Rounded.PhotoLibrary,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
          }
        }

        if (item.isVideoItem && thumbnailFile != null) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.align(Alignment.Center).size(38.dp),
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

        if (selectMode) {
          Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(4.dp)
                .testTag("bin_checkbox_${item.id}"),
          )
        }
      }

      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = item.album,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilledTonalButton(
            onClick = onRestore,
            enabled = !isMutating,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("bin_restore_${item.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.RestoreFromTrash,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.bin_restore),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = onDeleteForever,
            enabled = !isMutating,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("bin_delete_${item.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteForever,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.bin_delete_short),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
