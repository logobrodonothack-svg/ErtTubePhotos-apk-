package com.example.ui.gallery

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.AddToPhotos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PhotoAlbum
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportGooglePhotosSheet(
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onImportPhotos: (List<ImportedPhotoInput>, PhotoAlbum, String?, Boolean) -> Unit,
  onCancelled: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var targetAlbum by remember { mutableStateOf(PhotoAlbum.REELS) }
  var importVideosOnly by remember { mutableStateOf(false) }
  var customNote by remember { mutableStateOf("") }
  var markFavorite by remember { mutableStateOf(false) }
  var isCopying by remember { mutableStateOf(false) }

  val multiPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_IMPORT_BATCH)
    ) { uris ->
      if (uris.isEmpty()) {
        onCancelled()
        return@rememberLauncherForActivityResult
      }
      scope.launch {
        isCopying = true
        val imported = copyMultiplePickedImagesToAppStorage(context, uris)
        isCopying = false
        if (imported.isNotEmpty()) {
          onImportPhotos(imported, targetAlbum, customNote.ifBlank { null }, markFavorite)
        } else {
          onCancelled()
        }
      }
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 8.dp)
          .testTag("import_google_photos_sheet"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.CloudDownload,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = stringResource(R.string.import_google_photos_title),
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

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = stringResource(R.string.import_google_photos_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(14.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.videos_import_include_videos),
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !importVideosOnly,
          onClick = { importVideosOnly = false },
          label = { Text(stringResource(R.string.videos_import_all_media)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.PhotoLibrary,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("import_mode_all_media"),
        )
        FilterChip(
          selected = importVideosOnly,
          onClick = { importVideosOnly = true },
          label = { Text(stringResource(R.string.videos_import_videos_only)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Videocam,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("import_mode_videos_only"),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.import_google_photos_album_label),
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PhotoAlbum.entries.forEach { candidate ->
          FilterChip(
            selected = targetAlbum == candidate,
            onClick = { targetAlbum = candidate },
            label = { Text(candidate.label) },
            leadingIcon =
              if (targetAlbum == candidate) {
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
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("import_album_${candidate.name.lowercase()}"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = customNote,
        onValueChange = { customNote = it.take(400) },
        label = { Text(stringResource(R.string.import_google_photos_note_label)) },
        placeholder = { Text(stringResource(R.string.import_google_photos_note_placeholder)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("import_note_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.import_google_photos_favorite_label),
          style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
          checked = markFavorite,
          onCheckedChange = { markFavorite = it },
          modifier = Modifier.testTag("import_favorite_switch"),
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = {
          val pickerType =
            if (importVideosOnly) {
              ActivityResultContracts.PickVisualMedia.VideoOnly
            } else {
              ActivityResultContracts.PickVisualMedia.ImageAndVideo
            }
          multiPickerLauncher.launch(PickVisualMediaRequest(pickerType))
        },
        enabled = !isSaving && !isCopying,
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(52.dp)
            .testTag("launch_google_photos_picker_button"),
      ) {
        if (isCopying || isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = stringResource(R.string.import_google_photos_importing))
        } else {
          Icon(imageVector = Icons.Rounded.AddToPhotos, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.import_google_photos_cta))
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

private const val MAX_IMPORT_BATCH = 20

private suspend fun copyMultiplePickedImagesToAppStorage(
  context: Context,
  uris: List<Uri>,
): List<ImportedPhotoInput> {
  return uris.mapIndexedNotNull { index, uri ->
    copyPickedMediaToAppStorage(context, uri, index)?.let { copied ->
      ImportedPhotoInput(
        title = copied.title,
        localPath = copied.localPath,
        mediaType = copied.mediaType,
        durationMs = copied.durationMs,
      )
    }
  }
}
