package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.components.OceanWaveHeader
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_header_displays_title_and_subtitle() {
    composeTestRule.setContent {
      MyApplicationTheme {
        OceanWaveHeader(
          title = "Ocean Tide Drop AI Surfer",
          subtitle = "AI Receptionist & Swell Hub"
        )
      }
    }

    composeTestRule.onNodeWithText("Ocean Tide Drop AI Surfer").assertIsDisplayed()
    composeTestRule.onNodeWithText("AI Receptionist & Swell Hub").assertIsDisplayed()
  }
}
