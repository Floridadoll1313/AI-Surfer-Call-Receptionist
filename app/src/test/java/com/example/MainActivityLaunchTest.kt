package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.integrations.ChatGptConfig
import com.example.integrations.IntegrationPrefsStore
import com.example.ai.SurferAiReceptionist
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityLaunchTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_launchesAndNavigatesAllTabsWithoutCrashing() {
    composeTestRule.onNodeWithTag("nav_receptionist").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_calls").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_client_hub").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_products").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_integrations").assertIsDisplayed()

    // Navigate to Dashboard (Wave Summary + Search & Filters)
    composeTestRule.onNodeWithTag("nav_calls").performClick()
    composeTestRule.onNodeWithTag("call_summary_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_search_filters_button").performClick()
    composeTestRule.onNodeWithTag("call_search_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("STATUS FILTERS").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("date_filter_chip_today").performScrollTo().assertIsDisplayed()

    // Navigate to Client AI, Products, and Settings
    composeTestRule.onNodeWithTag("nav_client_hub").performClick()
    composeTestRule.onNodeWithTag("nav_products").performClick()
    composeTestRule.onNodeWithTag("add_item_fab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_integrations").performClick()
    composeTestRule.onNodeWithTag("chatgpt_integration_card").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun releaseOpenAiKey_clearsSavedKeyAndPersistsDisabledState() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    IntegrationPrefsStore.saveChatGptConfig(
      context,
      ChatGptConfig(apiKey = "sk-test-custom-override", model = "gpt-4o", isEnabled = true)
    )

    composeTestRule.onNodeWithTag("nav_integrations").performClick()
    composeTestRule.onNodeWithTag("clear_chatgpt_key_button").performScrollTo().performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      val loaded = IntegrationPrefsStore.loadChatGptConfig(context)
      loaded.apiKey.isEmpty() && !loaded.isEnabled
    }

    val updated = IntegrationPrefsStore.loadChatGptConfig(context)
    assertEquals("", updated.apiKey)
    assertFalse(updated.isEnabled)

    // Verify SurferAiReceptionist falls back cleanly without attempting OpenAI when released
    val result = runBlocking {
      SurferAiReceptionist.respondToCall(
        callerName = "Makena",
        callerNumber = "+1 (808) 555-9999",
        callerQuestion = "What is the price of Wave Starter?",
        products = com.example.data.local.AppDatabase.initialProducts,
        faqs = com.example.data.local.AppDatabase.initialFaqs,
        config = updated
      )
    }
    assertTrue(result.responseText.contains("497") || result.responseText.contains("Wave Starter"))
  }
}
