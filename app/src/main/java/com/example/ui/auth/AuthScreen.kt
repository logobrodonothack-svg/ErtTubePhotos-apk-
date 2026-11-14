package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.GalleryMode
import com.google.firebase.auth.FirebaseUser

enum class AuthMode {
  SIGN_IN,
  SIGN_UP,
}

data class PendingSignUpProfile(
  val displayName: String,
  val handle: String,
  val preferredGalleryMode: GalleryMode,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
  onAuthSuccess: (FirebaseUser, PendingSignUpProfile?) -> Unit,
  modifier: Modifier = Modifier,
  autoSignInEnabled: Boolean = true,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val focusManager = LocalFocusManager.current

  var authMode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
  var displayNameInput by rememberSaveable { mutableStateOf("") }
  var handleInput by rememberSaveable { mutableStateOf("") }
  var selectedMode by rememberSaveable { mutableStateOf(GalleryMode.CINEMA_REELS) }

  var isLoading by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isErrorStatus by remember { mutableStateOf(false) }

  val cancelledNotice = stringResource(R.string.auth_cancelled_notice)

  LaunchedEffect(autoSignInEnabled) {
    if (autoSignInEnabled) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { user -> onAuthSuccess(user, null) },
        onUnauthenticated = {},
        scope = coroutineScope,
      )
    }
  }

  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary
  val backgroundColor = MaterialTheme.colorScheme.background

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(backgroundColor)
        .drawBehind {
          drawRect(
            brush =
              Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.14f),
                radius = size.maxDimension * 0.65f,
              )
          )
          drawRect(
            brush =
              Brush.radialGradient(
                colors = listOf(tertiaryColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.78f),
                radius = size.maxDimension * 0.45f,
              )
          )
        }
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ErtTube Brand Hero
        ErtTubeBrandHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // Segmented Sign In / Sign Up Mode Switcher
        AuthModeSwitcher(
          selectedMode = authMode,
          onModeSelected = { newMode ->
            authMode = newMode
            statusMessage = null
          },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Auth Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          tonalElevation = 4.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.Start,
          ) {
            AnimatedContent(
              targetState = authMode,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "auth_mode_content",
            ) { mode ->
              if (mode == AuthMode.SIGN_IN) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Text(
                    text = stringResource(R.string.auth_sign_in_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = stringResource(R.string.auth_sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(20.dp))

                  FeatureHighlightRow(
                    icon = Icons.Outlined.CloudSync,
                    title = stringResource(R.string.auth_feature_sync_title),
                    subtitle = stringResource(R.string.auth_feature_sync_desc),
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  FeatureHighlightRow(
                    icon = Icons.Outlined.Collections,
                    title = stringResource(R.string.auth_feature_albums_title),
                    subtitle = stringResource(R.string.auth_feature_albums_desc),
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  FeatureHighlightRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = stringResource(R.string.auth_feature_picker_title),
                    subtitle = stringResource(R.string.auth_feature_picker_desc),
                  )
                }
              } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Text(
                    text = stringResource(R.string.auth_sign_up_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = stringResource(R.string.auth_sign_up_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(18.dp))

                  OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it.take(80) },
                    label = { Text(stringResource(R.string.auth_display_name_label)) },
                    placeholder = { Text(stringResource(R.string.auth_display_name_placeholder)) },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Rounded.PersonOutline,
                        contentDescription = null,
                      )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                      OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                      ),
                    modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
                  )

                  Spacer(modifier = Modifier.height(12.dp))

                  OutlinedTextField(
                    value = handleInput,
                    onValueChange = { handleInput = it.take(40) },
                    label = { Text(stringResource(R.string.auth_handle_label)) },
                    placeholder = { Text(stringResource(R.string.auth_handle_placeholder)) },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Rounded.AlternateEmail,
                        contentDescription = null,
                      )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                      OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                      ),
                    modifier = Modifier.fillMaxWidth().testTag("signup_handle_input"),
                  )

                  Spacer(modifier = Modifier.height(16.dp))

                  Text(
                    text = stringResource(R.string.auth_vibe_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(8.dp))

                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    GalleryMode.entries.forEach { galleryMode ->
                      val isSelected = selectedMode == galleryMode
                      FilterChip(
                        selected = isSelected,
                        onClick = { selectedMode = galleryMode },
                        label = { Text(galleryMode.label) },
                        leadingIcon =
                          if (isSelected) {
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
                        colors =
                          FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                          ),
                        modifier =
                          Modifier.minimumInteractiveComponentSize()
                            .testTag("vibe_chip_${galleryMode.wireValue}"),
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Status / Error Banner
            AnimatedVisibility(visible = statusMessage != null) {
              statusMessage?.let { message ->
                Surface(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                  shape = RoundedCornerShape(16.dp),
                  color =
                    if (isErrorStatus) {
                      MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                      MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Info,
                      contentDescription = null,
                      tint =
                        if (isErrorStatus) {
                          MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                          MaterialTheme.colorScheme.primary
                        },
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = message,
                      style = MaterialTheme.typography.bodyMedium,
                      color =
                        if (isErrorStatus) {
                          MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                          MaterialTheme.colorScheme.onSurface
                        },
                      modifier = Modifier.weight(1f),
                    )
                    IconButton(
                      onClick = { statusMessage = null },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_dismiss_error),
                      )
                    }
                  }
                }
              }
            }

            // Primary Google Sign-In / Sign-Up CTA
            val ctaTestTag =
              if (authMode == AuthMode.SIGN_IN) {
                "google_sign_in_button"
              } else {
                "google_sign_up_button"
              }

            Button(
              onClick = {
                focusManager.clearFocus()
                statusMessage = null
                isLoading = true
                val pendingProfile =
                  if (authMode == AuthMode.SIGN_UP) {
                    val normalizedHandle =
                      handleInput.trim().let { raw ->
                        if (raw.isEmpty()) "" else if (raw.startsWith("@")) raw else "@$raw"
                      }
                    PendingSignUpProfile(
                      displayName = displayNameInput.trim(),
                      handle = normalizedHandle,
                      preferredGalleryMode = selectedMode,
                    )
                  } else {
                    null
                  }

                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = { user ->
                    isLoading = false
                    onAuthSuccess(user, pendingProfile)
                  },
                  onAuthError = { errorMsg ->
                    isLoading = false
                    isErrorStatus = true
                    statusMessage = errorMsg
                  },
                  scope = coroutineScope,
                  onAuthCancelled = {
                    isLoading = false
                    isErrorStatus = false
                    statusMessage = cancelledNotice
                  },
                )
              },
              enabled = !isLoading,
              shape = RoundedCornerShape(18.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.fillMaxWidth().height(56.dp).testTag(ctaTestTag),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = stringResource(R.string.auth_signing_in),
                  style = MaterialTheme.typography.labelLarge,
                )
              } else {
                GoogleMarkBadge()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text =
                    if (authMode == AuthMode.SIGN_IN) {
                      stringResource(R.string.auth_google_sign_in)
                    } else {
                      stringResource(R.string.auth_google_sign_up)
                    },
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = stringResource(R.string.auth_google_security_note),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        TextButton(
          onClick = {
            statusMessage = null
            authMode = if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("auth_mode_switch_link"),
        ) {
          Text(
            text =
              if (authMode == AuthMode.SIGN_IN) {
                stringResource(R.string.auth_switch_to_sign_up)
              } else {
                stringResource(R.string.auth_switch_to_sign_in)
              },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun ErtTubeBrandHeader() {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier =
        Modifier.size(86.dp)
          .clip(RoundedCornerShape(24.dp))
          .border(
            width = 1.5.dp,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                  )
              ),
            shape = RoundedCornerShape(24.dp),
          ),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(id = R.drawable.erttube_logo),
        contentDescription = stringResource(R.string.logo_content_description),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.brand_erttube),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
      ) {
        Text(
          text = stringResource(R.string.brand_photos_badge),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = stringResource(R.string.auth_tagline),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun AuthModeSwitcher(
  selectedMode: AuthMode,
  onModeSelected: (AuthMode) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      AuthModeTab(
        label = stringResource(R.string.auth_tab_sign_in),
        selected = selectedMode == AuthMode.SIGN_IN,
        onClick = { onModeSelected(AuthMode.SIGN_IN) },
        modifier = Modifier.weight(1f).testTag("auth_mode_sign_in"),
      )
      AuthModeTab(
        label = stringResource(R.string.auth_tab_sign_up),
        selected = selectedMode == AuthMode.SIGN_UP,
        onClick = { onModeSelected(AuthMode.SIGN_UP) },
        modifier = Modifier.weight(1f).testTag("auth_mode_sign_up"),
      )
    }
  }
}

@Composable
private fun AuthModeTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color =
      if (selected) {
        MaterialTheme.colorScheme.primary
      } else {
        Color.Transparent
      },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color =
          if (selected) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
      )
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(20.dp),
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun GoogleMarkBadge() {
  Box(
    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(16.dp)) {
      val strokeWidth = size.minDimension * 0.22f
      drawArc(
        color = Color(0xFF4285F4),
        startAngle = -35f,
        sweepAngle = 115f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
      )
      drawArc(
        color = Color(0xFF34A853),
        startAngle = 80f,
        sweepAngle = 95f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
      )
      drawArc(
        color = Color(0xFFFBBC05),
        startAngle = 175f,
        sweepAngle = 60f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
      )
      drawArc(
        color = Color(0xFFEA4335),
        startAngle = 235f,
        sweepAngle = 85f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
      )
      drawLine(
        color = Color(0xFF4285F4),
        start = Offset(size.width * 0.5f, size.height * 0.5f),
        end = Offset(size.width * 0.96f, size.height * 0.5f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Butt,
      )
    }
  }
}
