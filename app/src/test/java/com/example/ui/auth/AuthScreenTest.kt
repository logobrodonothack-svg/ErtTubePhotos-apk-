package com.example.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.ui.theme.ErtTubePhotosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun authScreen_togglesBetweenSignInAndSignUpModes() {
    composeTestRule.setContent {
      ErtTubePhotosTheme {
        AuthScreen(
          onAuthSuccess = { _, _ -> },
          autoSignInEnabled = false,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_mode_sign_in").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_mode_sign_up").assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("auth_mode_sign_up").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("signup_name_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("signup_handle_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("vibe_chip_cinema_reels").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_up_button").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("signup_name_input").performScrollTo().performTextInput("Ert Creator")
    composeTestRule.onNodeWithTag("signup_handle_input").performScrollTo().performTextInput("ert_studio")
    composeTestRule.onNodeWithTag("vibe_chip_studio_shots").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("auth_mode_switch_link").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
  }
}
