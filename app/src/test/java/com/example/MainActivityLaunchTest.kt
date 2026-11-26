package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.example.data.local.CallEntity
import org.robolectric.RuntimeEnvironment
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

    // Verify real 2-phone card on Receptionist screen and no fake test alert button
    composeTestRule.onNodeWithTag("two_phone_status_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_ai_instruments_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("test_call_alert_button").assertDoesNotExist()

    // Navigate to Dashboard (Wave Summary + Search & Filters)
    composeTestRule.onNodeWithTag("nav_calls").performClick()
    composeTestRule.onNodeWithTag("call_summary_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_search_filters_button").performClick()
    composeTestRule.onNodeWithTag("call_search_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("BUSINESS PHONE LINE").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("date_filter_chip_today").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("STATUS FILTERS").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_line_filter_chip_all").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_line_filter_chip_phone_1").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_line_filter_chip_phone_2").performScrollTo().assertIsDisplayed()

    // Navigate to Client AI and verify all 7 AI instruments selector + switching
    composeTestRule.onNodeWithTag("nav_client_hub").performClick()
    composeTestRule.onNodeWithTag("ai_instruments_selector").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_tool_chip_copilot_chat").performClick()
    composeTestRule.onNodeWithTag("copilot_chat_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_tool_chip_wave_advisor").performClick()
    composeTestRule.onNodeWithTag("client_ai_advisor_card").assertIsDisplayed()

    // Products and Settings (2-Phone Setup + Phone Verification + Publishing)
    composeTestRule.onNodeWithTag("nav_products").performClick()
    composeTestRule.onNodeWithTag("add_item_fab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_integrations").performClick()
    composeTestRule.onNodeWithTag("google_account_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("two_phone_setup_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_verification_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("permissions_and_publishing_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("chatgpt_integration_card").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h640dp")
  fun receptionistConsole_isVisibleBeforeSetupOnCompactPhone() {
    // The question field and submit control must be reachable before scrolling.
    composeTestRule.onNodeWithTag("receptionist_test_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("receptionist_test_submit").assertIsDisplayed()
    composeTestRule.onNodeWithTag("console_voice_mic_button").assertIsDisplayed()

    // Setup remains available below the console.
    composeTestRule.onNodeWithTag("two_phone_status_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_setup_card").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h640dp")
  fun expandedAlerts_leaveProductsAndReceptionistUsableWithLargeText() {
    RuntimeEnvironment.setFontScale(1.6f)
    composeTestRule.activityRule.scenario.recreate()
    val app = ApplicationProvider.getApplicationContext<OceanSurferApp>()
    runBlocking {
      app.database.callDao().insertCalls((1..44).map { index ->
        CallEntity(
          callerNumber = "test-$index", callerName = "Test caller",
          status = "ESCALATED", isEscalated = true, summary = "",
          transcription = "", customerQuestion = "", aiResponse = ""
        )
      })
    }

    composeTestRule.onNodeWithTag("nav_products").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("status_bar_alert_pill").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("status_bar_alert_pill").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Expand Status Details").performClick()
    composeTestRule.onNodeWithText("44 Alerts").performScrollTo().assertIsDisplayed()
    composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
    composeTestRule.onNodeWithTag("product_item_1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_receptionist").performClick()
    composeTestRule.onNodeWithTag("receptionist_test_input")
      .performScrollTo().assertIsDisplayed().performTextInput("What services are available?")
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
