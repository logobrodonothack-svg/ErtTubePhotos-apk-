package com.example.ui.menu

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SavedAccount
import com.example.data.model.UserProfile
import com.example.ui.gallery.StorageStats
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
  accountDisplayName: String?,
  accountEmail: String?,
  profile: UserProfile?,
  storageStats: StorageStats,
  otherAccounts: List<SavedAccount>,
  isAccountBusy: Boolean,
  onDismiss: () -> Unit,
  onOpenManageStorage: () -> Unit,
  onOpenPhotosSettings: () -> Unit,
  onOpenImportGooglePhotos: () -> Unit,
  onOpenEditProfile: () -> Unit,
  onSwitchAccount: () -> Unit,
  onAddAccount: () -> Unit,
  onSwitchToSavedAccount: (SavedAccount) -> Unit,
  onSignOut: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val activeName =
    profile?.displayName?.takeIf { it.isNotBlank() }
      ?: accountDisplayName?.takeIf { it.isNotBlank() }
      ?: "ErtTube Creator"
  val activeInitial = activeName.firstOrNull()?.uppercaseChar()?.toString() ?: "E"
  val backupEnabled = profile?.backupEnabled ?: false
  val wifiOnly = profile?.backupWifiOnly ?: true

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .testTag("menu_sheet"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.menu_title),
          style = MaterialTheme.typography.headlineSmall,
        )
        IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_close_dialog),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Active Account Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("menu_active_account"),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = activeInitial,
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = activeName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              accountEmail?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                  text = email,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              profile?.handle?.takeIf { it.isNotBlank() }?.let { handle ->
                Text(
                  text = handle,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            TextButton(
              onClick = onOpenEditProfile,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("menu_edit_profile_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = stringResource(R.string.menu_edit_profile))
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Switch Account / Add Account Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onSwitchAccount,
              enabled = !isAccountBusy,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("switch_account_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.SwitchAccount,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.menu_switch_account),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            FilledTonalButton(
              onClick = onAddAccount,
              enabled = !isAccountBusy,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("add_account_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PersonAddAlt1,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.menu_add_account),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Other Accounts Section
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("other_accounts_section"),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = stringResource(R.string.menu_other_accounts_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(8.dp))
          if (otherAccounts.isEmpty()) {
            Text(
              text = stringResource(R.string.menu_other_accounts_empty),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            otherAccounts.forEachIndexed { index, account ->
              if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
              }
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isAccountBusy) { onSwitchToSavedAccount(account) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = account.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = account.email ?: account.handle ?: account.uid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                TextButton(
                  onClick = { onSwitchToSavedAccount(account) },
                  enabled = !isAccountBusy,
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("switch_other_account_${account.uid}"),
                ) {
                  Text(text = stringResource(R.string.menu_switch_to_account))
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Manage Storage Card (1 MB / 1 TB)
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenManageStorage)
            .testTag("menu_manage_storage_card"),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.CloudQueue,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(R.string.menu_manage_storage),
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = storageStats.formattedRatio,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                  Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("menu_storage_ratio"),
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          LinearProgressIndicator(
            progress = { storageStats.progressFraction },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text =
              stringResource(
                R.string.menu_storage_summary_fmt,
                storageStats.formattedRatio,
                storageStats.cloudFrameCount,
              ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Photos Settings Row Card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenPhotosSettings)
            .testTag("menu_photos_settings_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.menu_photos_settings),
              style = MaterialTheme.typography.titleMedium,
            )
            val wifiLabel =
              if (wifiOnly) stringResource(R.string.photos_settings_wifi_title) else "Wi-Fi & Mobile"
            Text(
              text =
                if (backupEnabled) {
                  stringResource(R.string.menu_photos_settings_subtitle_on, wifiLabel)
                } else {
                  stringResource(R.string.menu_photos_settings_subtitle_off)
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color =
              if (backupEnabled) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
              },
          ) {
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
              modifier =
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  .testTag("menu_backup_status_badge"),
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Import Google Photos Card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenImportGooglePhotos)
            .testTag("menu_import_google_photos_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.import_google_photos_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.import_google_photos_menu_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.profile_sign_out), fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
