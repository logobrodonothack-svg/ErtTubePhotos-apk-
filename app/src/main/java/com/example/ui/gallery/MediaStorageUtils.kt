package com.example.ui.gallery

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.MediaType
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CopiedMediaResult(
  val title: String,
  val localPath: String,
  val mediaType: MediaType,
  val durationMs: Long?,
)

suspend fun copyPickedMediaToAppStorage(
  context: Context,
  uri: Uri,
  indexFallback: Int = 0,
): CopiedMediaResult? =
  withContext(Dispatchers.IO) {
    try {
      val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
      val mimeType = context.contentResolver.getType(uri)
      val rawName = resolveMediaDisplayName(context, uri)
      val isVideo =
        mimeType?.startsWith("video/") == true ||
          rawName?.lowercase(Locale.US)?.let {
            it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".mkv") || it.endsWith(".webm")
          } == true

      val extension = if (isVideo) "mp4" else "jpg"
      val prefix = if (isVideo) "ert_vid_" else "ert_"
      val destFile = File(photosDir, "${prefix}${UUID.randomUUID()}.$extension")

      val copied =
        context.contentResolver.openInputStream(uri)?.use { input ->
          destFile.outputStream().use { output -> input.copyTo(output) }
          true
        } ?: false

      if (!copied || !destFile.exists()) return@withContext null

      val cleanTitle =
        rawName
          ?.substringBeforeLast('.')
          ?.trim()
          ?.takeIf { it.isNotEmpty() }
          ?.take(100)
          ?: if (isVideo) "ErtTube Video ${indexFallback + 1}" else "Google Photo ${indexFallback + 1}"

      var durationMs: Long? = null
      if (isVideo) {
        val retriever = MediaMetadataRetriever()
        try {
          retriever.setDataSource(destFile.absolutePath)
          durationMs =
            retriever
              .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
              ?.toLongOrNull()
              ?.takeIf { it >= 0L }
          val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          if (frame != null) {
            val thumbFile = File("${destFile.absolutePath}.thumb.jpg")
            thumbFile.outputStream().use { out ->
              frame.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            frame.recycle()
          }
        } catch (_: Exception) {
          // Non-fatal: playback still works without extracted thumbnail/duration
        } finally {
          try {
            retriever.release()
          } catch (_: Exception) {
          }
        }
      }

      CopiedMediaResult(
        title = cleanTitle,
        localPath = destFile.absolutePath,
        mediaType = if (isVideo) MediaType.VIDEO else MediaType.PHOTO,
        durationMs = durationMs,
      )
    } catch (_: Exception) {
      null
    }
  }

fun videoThumbnailFile(localPath: String?): File? {
  if (localPath.isNullOrBlank()) return null
  val thumb = File("$localPath.thumb.jpg")
  return thumb.takeIf { it.exists() && it.length() > 0L }
}

fun formatVideoDuration(durationMs: Long?): String? {
  if (durationMs == null || durationMs <= 0L) return null
  val totalSeconds = durationMs / 1000L
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0L) {
    String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.US, "%d:%02d", minutes, seconds)
  }
}

private fun resolveMediaDisplayName(context: Context, uri: Uri): String? {
  return try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
      ?.use { cursor ->
        val colIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (colIdx >= 0 && cursor.moveToFirst()) {
          cursor.getString(colIdx)
        } else {
          null
        }
      }
  } catch (_: Exception) {
    null
  }
}
