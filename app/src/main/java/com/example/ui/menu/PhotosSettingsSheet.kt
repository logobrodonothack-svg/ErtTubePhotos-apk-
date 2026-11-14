package com.example.ui.menu

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.gallery.StorageStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosSettingsSheet(
  backupEnabled: Boolean,
  backupWifiOnly: Boolean,
  storageStats: StorageStats,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onBackToMenu: () -> Unit,
  onBackupChanged: (Boolean) -> Unit,
  onWifiOnlyChanged: (Boolean) -> Unit,
  onOpenManageStorage: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .testTag("photos_settings_sheet"),
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
          text = stringResource(R.string.photos_settings_title),
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

      // Backup Off / On Toggle Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = CircleShape,
            color =
              if (backupEnabled) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
              },
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
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
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = stringResource(R.string.photos_settings_backup_title),
                style = MaterialTheme.typography.titleMedium,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = RoundedCornerShape(8.dp),
                color =
                  if (backupEnabled) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                  },
              ) {
                Text(
                  text =
                    if (backupEnabled) {
                      stringResource(R.string.photos_settings_state_on)
                    } else {
                      stringResource(R.string.photos_settings_state_off)
                    },
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (backupEnabled) {
                      MaterialTheme.colorScheme.onPrimary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  modifier =
                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                      .testTag("backup_status_text"),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text =
                if (backupEnabled) {
                  stringResource(R.string.photos_settings_backup_desc_on)
                } else {
                  stringResource(R.string.photos_settings_backup_desc_off)
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = backupEnabled,
            enabled = !isSaving,
            onCheckedChange = onBackupChanged,
            modifier = Modifier.testTag("backup_toggle_switch"),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Wi-Fi Only Toggle
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.photos_settings_wifi_title),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = stringResource(R.string.photos_settings_wifi_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = backupWifiOnly,
            enabled = backupEnabled && !isSaving,
            onCheckedChange = onWifiOnlyChanged,
            modifier = Modifier.testTag("backup_wifi_only_switch"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedButton(
        onClick = onOpenManageStorage,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_manage_storage_button"),
      ) {
        Icon(imageVector = Icons.Rounded.CloudQueue, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            "${stringResource(R.string.menu_manage_storage)} · ${storageStats.formattedRatio}",
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
