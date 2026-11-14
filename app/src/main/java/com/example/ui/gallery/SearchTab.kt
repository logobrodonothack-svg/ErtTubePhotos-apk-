package com.example.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PhotoAlbum
import com.example.data.model.PhotoItem

private val SearchSuggestions =
  listOf(
    "Videos",
    "Favorites",
    PhotoAlbum.REELS.label,
    PhotoAlbum.PORTRAITS.label,
    PhotoAlbum.STUDIO.label,
    PhotoAlbum.TRAVEL.label,
  )

@Composable
fun SearchTab(
  photosUiState: UiState<List<PhotoItem>>,
  query: String,
  onQueryChange: (String) -> Unit,
  onPhotoClick: (PhotoItem) -> Unit,
  onFavoriteToggle: (PhotoItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val trimmed = query.trim()

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text(
        text = stringResource(R.string.search_header_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_input_placeholder)) },
        leadingIcon = {
          Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
        },
        trailingIcon =
          if (query.isNotEmpty()) {
            {
              IconButton(
                onClick = { onQueryChange("") },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("search_clear_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = stringResource(R.string.search_clear),
                )
              }
            }
          } else {
            null
          },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("search_query_input"),
      )
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(SearchSuggestions) { suggestion ->
        val selected = trimmed.equals(suggestion, ignoreCase = true)
        FilterChip(
          selected = selected,
          onClick = {
            if (selected) onQueryChange("") else onQueryChange(suggestion)
          },
          label = { Text(suggestion) },
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("search_chip_${suggestion.lowercase()}"),
        )
      }
    }

    when (photosUiState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(
            text = photosUiState.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
          )
        }
      }
      is UiState.Success -> {
        val matches =
          remember(photosUiState.data, trimmed) {
            val active = photosUiState.data.filter { !it.trashed }
            if (trimmed.isEmpty()) {
              active
            } else if (trimmed.equals("favorites", ignoreCase = true)) {
              active.filter { it.favorite }
            } else if (trimmed.equals("videos", ignoreCase = true) || trimmed.equals("video", ignoreCase = true)) {
              active.filter { it.isVideoItem }
            } else {
              active.filter { photo ->
                val typeWord = if (photo.isVideoItem) "video" else "photo"
                photo.title.contains(trimmed, ignoreCase = true) ||
                  (photo.caption?.contains(trimmed, ignoreCase = true) == true) ||
                  photo.album.contains(trimmed, ignoreCase = true) ||
                  typeWord.contains(trimmed, ignoreCase = true)
              }
            }
          }

        Text(
          text =
            if (trimmed.isEmpty()) {
              stringResource(R.string.search_all_frames_count, matches.size)
            } else {
              stringResource(R.string.search_results_count, matches.size)
            },
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        if (matches.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              modifier = Modifier.widthIn(max = 340.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text =
                  if (trimmed.isNotEmpty()) {
                    stringResource(R.string.search_empty_title, trimmed)
                  } else {
                    stringResource(R.string.gallery_empty_title)
                  },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = stringResource(R.string.search_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              if (trimmed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                  onClick = { onQueryChange("") },
                  modifier = Modifier.height(48.dp),
                ) {
                  Text(text = stringResource(R.string.search_clear))
                }
              }
            }
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(matches, key = { it.id }) { photo ->
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
