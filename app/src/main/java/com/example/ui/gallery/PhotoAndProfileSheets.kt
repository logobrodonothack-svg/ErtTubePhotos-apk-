package com.example.ui.gallery

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.GalleryMode
import com.example.data.model.MediaType
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem
import com.example.data.model.UserProfile
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoEditorSheet(
  initialPhoto: PhotoItem?,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (String, String?, PhotoAlbum, String?, Boolean, MediaType, Long?) -> Unit,
  onDelete: (() -> Unit)?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var title by remember { mutableStateOf(initialPhoto?.title ?: "") }
  var caption by remember { mutableStateOf(initialPhoto?.caption ?: "") }
  var album by remember { mutableStateOf(PhotoAlbum.fromLabel(initialPhoto?.album)) }
  var isFavorite by remember { mutableStateOf(initialPhoto?.favorite ?: false) }
  var localPath by remember { mutableStateOf(initialPhoto?.localPath) }
  var mediaType by remember { mutableStateOf(MediaType.fromWireValue(initialPhoto?.mediaType)) }
  var durationMs by remember { mutableStateOf(initialPhoto?.durationMs) }
  var isCopyingMedia by remember { mutableStateOf(false) }

  val mediaPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isCopyingMedia = true
          val copied = copyPickedMediaToAppStorage(context, uri)
          if (copied != null) {
            localPath = copied.localPath
            mediaType = copied.mediaType
            durationMs = copied.durationMs
            if (title.isBlank()) {
              title = copied.title
            }
          }
          isCopyingMedia = false
        }
      }
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            if (initialPhoto == null) {
              stringResource(R.string.sheet_add_photo_title)
            } else {
              stringResource(R.string.sheet_edit_photo_title)
            },
          style = MaterialTheme.typography.headlineSmall,
        )
        IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_close_dialog),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (initialPhoto == null) {
        val previewFile =
          if (mediaType == MediaType.VIDEO) {
            videoThumbnailFile(localPath)
          } else {
            localPath?.let { File(it) }?.takeIf { it.exists() }
          }
        if (previewFile != null && previewFile.exists()) {
          Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp))) {
            AsyncImage(
              model = previewFile,
              contentDescription = title.ifBlank { stringResource(R.string.sheet_photo_selected) },
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            if (mediaType == MediaType.VIDEO) {
              Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center).size(48.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_video),
                    tint = Color.White,
                  )
                }
              }
              formatVideoDuration(durationMs)?.let { formatted ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color.Black.copy(alpha = 0.72f),
                  modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                ) {
                  Text(
                    text = formatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        } else if (mediaType == MediaType.VIDEO && !localPath.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().height(72.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text =
                  formatVideoDuration(durationMs)?.let {
                    "${stringResource(R.string.videos_media_type_video)} · $it"
                  } ?: stringResource(R.string.videos_media_type_video),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            enabled = !isCopyingMedia,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("pick_photo_button"),
          ) {
            if (isCopyingMedia) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.videos_pick_photo_button))
            }
          }

          OutlinedButton(
            onClick = {
              mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
              )
            },
            enabled = !isCopyingMedia,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("pick_video_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Videocam,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.videos_pick_video_button))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = stringResource(R.string.videos_media_type_label),
          style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = mediaType == MediaType.PHOTO,
            onClick = { mediaType = MediaType.PHOTO },
            label = { Text(stringResource(R.string.videos_media_type_photo)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("media_type_photo"),
          )
          FilterChip(
            selected = mediaType == MediaType.VIDEO,
            onClick = { mediaType = MediaType.VIDEO },
            label = { Text(stringResource(R.string.videos_media_type_video)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("media_type_video"),
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it.take(100) },
        label = { Text(stringResource(R.string.sheet_title_label)) },
        placeholder = { Text(stringResource(R.string.sheet_title_placeholder)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("photo_title_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = caption,
        onValueChange = { caption = it.take(400) },
        label = { Text(stringResource(R.string.sheet_caption_label)) },
        placeholder = { Text(stringResource(R.string.sheet_caption_placeholder)) },
        maxLines = 3,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("photo_caption_input"),
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.sheet_album_label),
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PhotoAlbum.entries.forEach { candidate ->
          FilterChip(
            selected = album == candidate,
            onClick = { album = candidate },
            label = { Text(candidate.label) },
            leadingIcon =
              if (album == candidate) {
                {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
              } else {
                null
              },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.sheet_favorite_label),
          style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = {
          onSave(
            title,
            caption.ifBlank { null },
            album,
            localPath,
            isFavorite,
            mediaType,
            durationMs,
          )
        },
        enabled = !isSaving && !isCopyingMedia,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_photo_button"),
      ) {
        Text(
          text =
            if (initialPhoto == null) {
              stringResource(R.string.sheet_save_photo)
            } else {
              stringResource(R.string.sheet_update_photo)
            }
        )
      }

      if (onDelete != null) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onDelete,
          enabled = !isSaving,
          shape = RoundedCornerShape(16.dp),
          colors =
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_photo_button"),
        ) {
          Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.bin_move_to_bin))
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSheet(
  accountDisplayName: String?,
  accountEmail: String?,
  profile: UserProfile?,
  totalPhotos: Int,
  favoritePhotos: Int,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSaveProfile: (String, String, String, GalleryMode) -> Unit,
  onSignOut: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var displayName by remember(profile, accountDisplayName) {
    mutableStateOf(profile?.displayName ?: accountDisplayName ?: "ErtTube Creator")
  }
  var handle by remember(profile) { mutableStateOf(profile?.handle ?: "") }
  var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
  var galleryMode by remember(profile) {
    mutableStateOf(GalleryMode.fromWireValue(profile?.preferredGalleryMode))
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.profile_sheet_title),
          style = MaterialTheme.typography.headlineSmall,
        )
        IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_close_dialog),
          )
        }
      }

      accountEmail?.takeIf { it.isNotBlank() }?.let { email ->
        Text(
          text = email,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = stringResource(R.string.profile_photos_count, totalPhotos),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = stringResource(R.string.profile_favorites_count, favoritePhotos),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it.take(80) },
        label = { Text(stringResource(R.string.auth_display_name_label)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = handle,
        onValueChange = { handle = it.take(40) },
        label = { Text(stringResource(R.string.auth_handle_label)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it.take(200) },
        label = { Text(stringResource(R.string.profile_bio_label)) },
        placeholder = { Text(stringResource(R.string.profile_bio_placeholder)) },
        maxLines = 2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.auth_vibe_label),
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GalleryMode.entries.forEach { mode ->
          FilterChip(
            selected = galleryMode == mode,
            onClick = { galleryMode = mode },
            label = { Text(mode.label) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = { onSaveProfile(displayName, handle, bio, galleryMode) },
        enabled = !isSaving,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text(text = stringResource(R.string.profile_save))
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_sign_out_button"),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.profile_sign_out), fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
