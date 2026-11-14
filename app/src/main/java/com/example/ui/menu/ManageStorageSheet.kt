package com.example.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PhotoItem
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.gallery.StorageStats
import com.example.ui.gallery.videoThumbnailFile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStorageSheet(
  storageStats: StorageStats,
  photos: List<PhotoItem>,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onBackToMenu: () -> Unit,
  onClearLocalCache: () -> Unit,
  onMoveSelectedToBin: (List<String>) -> Unit,
  onPermanentlyDeleteSelected: (List<PhotoItem>) -> Unit,
  onOpenBackupSettings: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isSelectMultiple by remember { mutableStateOf(false) }
  var selectedIds by remember(photos) { mutableStateOf(emptySet<String>()) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  val itemSizeBytesById =
    remember(photos) {
      photos.associate { it.id to GalleryViewModel.computeItemStorageBytes(it) }
    }
  val selectedItems =
    remember(photos, selectedIds) { photos.filter { it.id in selectedIds } }
  val selectedTotalBytes =
    remember(selectedItems, itemSizeBytesById) {
      selectedItems.sumOf { itemSizeBytesById[it.id] ?: 4_096L }
    }
  val selectedActiveIds =
    remember(selectedItems) { selectedItems.filter { !it.trashed }.map { it.id } }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .testTag("manage_storage_sheet"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBackToMenu,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.menu_title),
          )
        }
        Text(
          text = stringResource(R.string.storage_sheet_title),
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_close_dialog),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = stringResource(R.string.storage_quota_headline),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = storageStats.formattedRatio,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("storage_detail_ratio"),
          )
          Spacer(modifier = Modifier.height(10.dp))
          LinearProgressIndicator(
            progress = { storageStats.progressFraction },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = stringResource(R.string.storage_used_detail, storageStats.usedMb.toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.storage_cloud_frames_label),
              style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              text = storageStats.cloudFrameCount.toString(),
              style = MaterialTheme.typography.titleMedium,
            )
          }
          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.storage_local_copies_label),
              style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              text = storageStats.localFileCount.toString(),
              style = MaterialTheme.typography.titleMedium,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Select Multiple Vault Items section
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.storage_vault_items_title, photos.size),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (photos.isNotEmpty()) {
          FilledTonalButton(
            onClick = {
              isSelectMultiple = !isSelectMultiple
              if (!isSelectMultiple) selectedIds = emptySet()
            },
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("storage_select_multiple_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Checklist,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                if (isSelectMultiple) {
                  stringResource(R.string.bin_done_mode)
                } else {
                  stringResource(R.string.storage_select_multiple)
                }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (isSelectMultiple && photos.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth().testTag("storage_multi_select_bar"),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text =
                  stringResource(
                    R.string.storage_selection_summary_fmt,
                    selectedIds.size,
                    GalleryViewModel.formatByteSize(selectedTotalBytes),
                  ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("storage_selection_summary"),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                  onClick = { selectedIds = photos.map { it.id }.toSet() },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("storage_select_all_button"),
                ) {
                  Text(text = stringResource(R.string.bin_select_all))
                }
                TextButton(
                  onClick = { selectedIds = emptySet() },
                  enabled = selectedIds.isNotEmpty(),
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("storage_clear_selection_button"),
                ) {
                  Text(text = stringResource(R.string.bin_clear_selection))
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = {
                  onMoveSelectedToBin(selectedActiveIds)
                  selectedIds = emptySet()
                  isSelectMultiple = false
                },
                enabled = !isSaving && selectedActiveIds.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                modifier =
                  Modifier.weight(1f).height(48.dp).testTag("storage_move_to_bin_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.bin_move_to_bin),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Button(
                onClick = { showConfirmDeleteDialog = true },
                enabled = !isSaving && selectedItems.isNotEmpty(),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                  ),
                shape = RoundedCornerShape(14.dp),
                modifier =
                  Modifier.weight(1f).height(48.dp).testTag("storage_delete_forever_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteForever,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.bin_delete_forever),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      if (photos.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.storage_vault_items_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      } else {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("storage_items_list"),
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            photos.forEachIndexed { index, item ->
              if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
              }
              val isSelected = item.id in selectedIds
              val itemBytes = itemSizeBytesById[item.id] ?: 4_096L
              StorageItemRow(
                item = item,
                sizeLabel = GalleryViewModel.formatByteSize(itemBytes),
                isSelectMultiple = isSelectMultiple,
                isSelected = isSelected,
                onToggleSelect = {
                  if (!isSelectMultiple) isSelectMultiple = true
                  selectedIds =
                    if (isSelected) selectedIds - item.id else selectedIds + item.id
                },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedButton(
        onClick = onClearLocalCache,
        enabled = !isSaving && storageStats.localFileCount > 0,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear_local_cache_button"),
      ) {
        Icon(imageVector = Icons.Rounded.CleaningServices, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.storage_clear_cache_cta))
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = onOpenBackupSettings,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("storage_open_backup_button"),
      ) {
        Icon(imageVector = Icons.Rounded.Backup, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.storage_open_backup_cta))
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showConfirmDeleteDialog && selectedItems.isNotEmpty()) {
    AlertDialog(
      onDismissRequest = { showConfirmDeleteDialog = false },
      title = { Text(stringResource(R.string.bin_confirm_delete_title)) },
      text = {
        Text(stringResource(R.string.bin_confirm_delete_message, selectedItems.size))
      },
      confirmButton = {
        Button(
          onClick = {
            onPermanentlyDeleteSelected(selectedItems)
            showConfirmDeleteDialog = false
            selectedIds = emptySet()
            isSelectMultiple = false
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.testTag("storage_confirm_delete_button"),
        ) {
          Text(stringResource(R.string.bin_delete_forever))
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDeleteDialog = false }) {
          Text(stringResource(R.string.cancel_action))
        }
      },
    )
  }
}

@Composable
private fun StorageItemRow(
  item: PhotoItem,
  sizeLabel: String,
  isSelectMultiple: Boolean,
  isSelected: Boolean,
  onToggleSelect: () -> Unit,
) {
  val thumbFile =
    remember(item.localPath, item.mediaType) {
      if (item.isVideoItem) {
        videoThumbnailFile(item.localPath)
      } else {
        item.localPath?.let { File(it) }?.takeIf { it.exists() }
      }
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(
          if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.surfaceContainerLow
        )
        .clickable(onClick = onToggleSelect)
        .padding(horizontal = 8.dp, vertical = 8.dp)
        .testTag("storage_item_${item.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.size(44.dp),
    ) {
      if (thumbFile != null) {
        AsyncImage(
          model = thumbFile,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
        )
      } else {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector =
              if (item.isVideoItem) Icons.Rounded.Videocam else Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            "${if (item.isVideoItem) stringResource(R.string.videos_media_type_video) else stringResource(R.string.videos_media_type_photo)} · ${item.album}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        if (item.trashed) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
          ) {
            Text(
              text = stringResource(R.string.storage_item_in_bin_badge),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }
      }
    }

    Text(
      text = sizeLabel,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 6.dp),
    )

    if (isSelectMultiple) {
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onToggleSelect() },
        modifier = Modifier.testTag("storage_checkbox_${item.id}"),
      )
    }
  }
}
