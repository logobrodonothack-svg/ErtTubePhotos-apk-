package com.example.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PhotoItem
import java.io.File

@Composable
internal fun PhotoCard(
  photo: PhotoItem,
  onClick: () -> Unit,
  onFavoriteToggle: () -> Unit,
) {
  val thumbnailFile =
    remember(photo.localPath, photo.mediaType) {
      if (photo.isVideoItem) {
        videoThumbnailFile(photo.localPath)
      } else {
        photo.localPath?.let { File(it) }?.takeIf { it.exists() }
      }
    }
  val formattedDuration =
    remember(photo.durationMs, photo.mediaType) {
      if (photo.isVideoItem) formatVideoDuration(photo.durationMs) else null
    }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("photo_card_${photo.id}"),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.05f)) {
        if (thumbnailFile != null) {
          AsyncImage(
            model = thumbnailFile,
            contentDescription = photo.title,
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
                      if (photo.isVideoItem) {
                        listOf(
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                          MaterialTheme.colorScheme.secondaryContainer,
                        )
                      } else {
                        listOf(
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                          MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                        )
                      }
                  )
                ),
            contentAlignment = Alignment.Center,
          ) {
            if (!photo.isVideoItem) {
              Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp),
              )
            }
          }
        }

        if (photo.isVideoItem) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.62f),
            modifier =
              Modifier.align(Alignment.Center)
                .size(46.dp)
                .testTag("video_play_badge_${photo.id}"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_video),
                tint = Color.White,
                modifier = Modifier.size(28.dp),
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.72f),
            modifier =
              Modifier.align(Alignment.BottomEnd)
                .padding(8.dp)
                .testTag("video_duration_badge_${photo.id}"),
          ) {
            Text(
              text = formattedDuration ?: stringResource(R.string.videos_badge_fallback),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color.Black.copy(alpha = 0.55f),
          modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        ) {
          Text(
            text = photo.album,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        IconButton(
          onClick = onFavoriteToggle,
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(4.dp)
              .minimumInteractiveComponentSize()
              .testTag("favorite_toggle_${photo.id}"),
        ) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector =
                  if (photo.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(R.string.cd_favorite_toggle),
                tint = if (photo.favorite) MaterialTheme.colorScheme.tertiary else Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        if (photo.isVideoItem && thumbnailFile == null) {
          Icon(
            imageVector = Icons.Rounded.Videocam,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).size(18.dp),
          )
        }
      }

      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
          text = photo.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        photo.caption?.takeIf { it.isNotBlank() }?.let { caption ->
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
