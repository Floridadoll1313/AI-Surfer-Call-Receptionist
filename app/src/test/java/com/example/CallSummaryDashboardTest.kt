package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.data.local.CallEntity
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.CallSummaryDashboard
import com.example.ui.theme.OceanPalette
import com.example.ui.theme.ThemeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.example.ui.components.CallDateFilter
import com.example.ui.components.CallFilterMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallSummaryDashboardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var repository: AppRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = AppRepository(database)
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun callSummaryDashboard_retrievesAndDisplaysLatestCallsFromRoom() {
    val olderCall = CallEntity(
      id = 101L,
      callerNumber = "+1 (808) 555-0101",
      callerName = "Keoni Hale",
      timestamp = 1_000_000L,
      durationSeconds = 35,
      status = "REVIEWED",
      urgency = "LOW",
      topic = "GENERAL",
      summary = "Asked about Ocean Tide Drop consultation hours.",
      transcription = "Caller: What are your hours?",
      customerQuestion = "What are your hours?",
      aiResponse = "We operate 24/7 with Kai AI Receptionist."
    )
    val latestCall = CallEntity(
      id = 102L,
      callerNumber = "+1 (808) 555-0202",
      callerName = "Nani Kahale",
      timestamp = 2_000_000L,
      durationSeconds = 72,
      status = "PENDING_REVIEW",
      urgency = "URGENT_VIP",
      topic = "ENTERPRISE",
      summary = "Topic: Big Kahuna Onboarding | Action Items: Schedule VIP strategy call | Sentiment: High Intent",
      transcription = "Caller: Can we deploy Big Kahuna this week?",
      customerQuestion = "Can we deploy Big Kahuna this week?",
      aiResponse = "Yes, flagging our VIP crew right away!"
    )

    val daoLatest = runBlocking {
      repository.insertCalls(listOf(olderCall, latestCall))
      repository.observeLatestCalls(limit = 10).first()
    }
    assertEquals(2, daoLatest.size)
    assertEquals("Nani Kahale", daoLatest.first().callerName)

    val viewModel = OceanSurferViewModel(repository, context)

    composeTestRule.setContent {
      ThemeProvider(
        palette = OceanPalette.DEEP_SEA,
        animateTransitions = false
      ) {
        CallSummaryDashboard(viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      runCatching {
        composeTestRule.onNodeWithTag("call_summary_item_102").assertExists()
        true
      }.getOrDefault(false)
    }

    composeTestRule.onNodeWithTag("call_summary_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("call_summary_pull_to_refresh").assertIsDisplayed()
    composeTestRule.onNodeWithTag("immersive_wave_background").assertIsDisplayed()
    composeTestRule.onNodeWithTag("call_summary_list").assertIsDisplayed()
    composeTestRule.onNodeWithText("Call Summary Dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_102"))
    composeTestRule.onNodeWithText("Nani Kahale").assertIsDisplayed()

    // Expand call 102 details
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("summary_expand_button_102"))
    composeTestRule.onNodeWithTag("summary_expand_button_102").performClick()
    composeTestRule.onNodeWithText("\"Can we deploy Big Kahuna this week?\"").assertIsDisplayed()

    // Scroll to review button after expansion and mark call 102 as reviewed in Room
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("summary_review_button_102"))
    composeTestRule.onNodeWithTag("summary_review_button_102").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      runBlocking {
        database.callDao().getCallById(102L)?.status == "REVIEWED"
      }
    }
    val updatedStatus = runBlocking { database.callDao().getCallById(102L)?.status }
    assertEquals("REVIEWED", updatedStatus)

    // Scroll LazyColumn to verify older call from Room is also displayed
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_101"))
    composeTestRule.onNodeWithText("Keoni Hale").assertIsDisplayed()
  }

  @Test
  fun callSummaryDashboard_rendersWaveEmptyStateWhenNoCallsExist() {
    composeTestRule.setContent {
      ThemeProvider(
        palette = OceanPalette.REEF_GLOW,
        animateTransitions = false
      ) {
        CallSummaryDashboard(calls = emptyList())
      }
    }

    composeTestRule.onNodeWithTag("immersive_wave_background").assertIsDisplayed()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_empty_state"))
    composeTestRule.onNodeWithTag("call_summary_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithText("Calm Waters — No Calls Logged").assertIsDisplayed()
  }

  @Test
  fun callSummaryDashboard_pullToRefreshTriggersSupabaseResync() {
    var syncTriggerCount = 0
    val sampleCall = CallEntity(
      id = 201L,
      callerNumber = "+1 (808) 555-0303",
      callerName = "Kaiulani Reef",
      timestamp = 3_000_000L,
      durationSeconds = 48,
      status = "PENDING_REVIEW",
      urgency = "HIGH",
      topic = "ONBOARDING",
      summary = "Requested immediate Supabase call log sync.",
      transcription = "Caller: Please check our synced logs.",
      customerQuestion = "Please check our synced logs.",
      aiResponse = "Checking live Supabase call records now!"
    )

    composeTestRule.setContent {
      ThemeProvider(
        palette = OceanPalette.SUNSET_TIDE,
        animateTransitions = false
      ) {
        CallSummaryDashboard(
          calls = listOf(sampleCall),
          isSyncing = false,
          lastSyncedLabel = "Synced with Supabase",
          onSyncLatest = { syncTriggerCount++ }
        )
      }
    }

    composeTestRule.onNodeWithTag("call_summary_pull_to_refresh").assertIsDisplayed()
    composeTestRule.onNodeWithTag("call_summary_sync_status").assertIsDisplayed()

    composeTestRule.onNodeWithTag("call_summary_list").performTouchInput {
      swipeDown(
        startY = top + 20f,
        endY = bottom - 10f,
        durationMillis = 350
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      syncTriggerCount == 1
    }

    assertEquals(1, syncTriggerCount)
  }

  @Test
  fun callSummaryDashboard_showsReSyncingStatusWhenSyncInProgress() {
    composeTestRule.setContent {
      ThemeProvider(
        palette = OceanPalette.DEEP_SEA,
        animateTransitions = false
      ) {
        CallSummaryDashboard(
          calls = emptyList(),
          isSyncing = true,
          lastSyncedLabel = "Live Supabase sync at 8:50 PM"
        )
      }
    }

    composeTestRule.onNodeWithText("Re-syncing calls from Supabase...").assertIsDisplayed()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_empty_state"))
    composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed()
  }

  @Test
  fun callSummaryDashboard_searchAndDateFilterNarrowCallsAndResetCleanly() {
    val now = System.currentTimeMillis()
    val tenDaysAgo = now - (10L * 24L * 60L * 60L * 1000L)

    val todayCall = CallEntity(
      id = 301L,
      callerNumber = "+1 (808) 555-1111",
      callerName = "Leilani Shore",
      timestamp = now,
      durationSeconds = 60,
      status = "PENDING_REVIEW",
      urgency = "HIGH",
      topic = "BOOKING",
      summary = "Requested a Wave Builder strategy booking today.",
      transcription = "Caller: Book me a strategy call today.",
      customerQuestion = "Can I book a Wave Builder strategy call today?",
      aiResponse = "Yes, Calendly booking slot is ready!"
    )
    val olderCall = CallEntity(
      id = 302L,
      callerNumber = "+1 (808) 555-2222",
      callerName = "Makoa Tide",
      timestamp = tenDaysAgo,
      durationSeconds = 40,
      status = "REVIEWED",
      urgency = "LOW",
      topic = "SUPPORT",
      summary = "Asked about Care Cove maintenance plan.",
      transcription = "Caller: Tell me about Care Cove.",
      customerQuestion = "Tell me about Care Cove.",
      aiResponse = "Care Cove provides monthly tuning."
    )

    composeTestRule.setContent {
      ThemeProvider(
        palette = OceanPalette.DEEP_SEA,
        animateTransitions = false
      ) {
        CallSummaryDashboard(calls = listOf(todayCall, olderCall))
      }
    }

    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_301"))
    composeTestRule.onNodeWithTag("call_summary_item_301").assertExists()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_302"))
    composeTestRule.onNodeWithTag("call_summary_item_302").assertExists()

    // Scroll back up to date filter chip and filter to Today
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_filter_card"))
    composeTestRule.onNodeWithTag("summary_date_filter_chip_today").performClick()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_301"))
    composeTestRule.onNodeWithTag("call_summary_item_301").assertExists()
    composeTestRule.onNodeWithTag("call_summary_item_302").assertDoesNotExist()

    // Switch to Last 30 Days -> both visible again
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_filter_card"))
    composeTestRule.onNodeWithTag("summary_date_filter_chip_last_30_days").performClick()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_302"))
    composeTestRule.onNodeWithTag("call_summary_item_302").assertExists()

    // Search by caller / topic -> only matching call remains
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_filter_card"))
    composeTestRule.onNodeWithTag("summary_call_search_input").performTextInput("Care Cove")
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_302"))
    composeTestRule.onNodeWithTag("call_summary_item_302").assertExists()
    composeTestRule.onNodeWithTag("call_summary_item_301").assertDoesNotExist()

    // Search for non-existent term -> shows empty-filter state with clear button
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_filter_card"))
    composeTestRule.onNodeWithTag("summary_call_search_input").performTextInput("zzznomatch")
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_no_matches_state"))
    composeTestRule.onNodeWithTag("call_summary_no_matches_state").assertIsDisplayed()
    composeTestRule.onNodeWithTag("clear_summary_filters_button").performClick()

    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_301"))
    composeTestRule.onNodeWithTag("call_summary_item_301").assertExists()
    composeTestRule.onNodeWithTag("call_summary_list")
      .performScrollToNode(hasTestTag("call_summary_item_302"))
    composeTestRule.onNodeWithTag("call_summary_item_302").assertExists()
  }

  @Test
  fun callFilterMatcher_evaluatesDatePresetsAndCustomUtcRangeAccurately() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 23, 14, 30, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val nowMillis = cal.timeInMillis
    val threeDaysAgo = nowMillis - (3L * 24L * 60L * 60L * 1000L)
    val fortyDaysAgo = nowMillis - (40L * 24L * 60L * 60L * 1000L)

    assertTrue(CallFilterMatcher.matchesDateFilter(nowMillis, CallDateFilter.TODAY, nowMillis = nowMillis))
    assertFalse(CallFilterMatcher.matchesDateFilter(threeDaysAgo, CallDateFilter.TODAY, nowMillis = nowMillis))
    assertTrue(CallFilterMatcher.matchesDateFilter(threeDaysAgo, CallDateFilter.LAST_7_DAYS, nowMillis = nowMillis))
    assertFalse(CallFilterMatcher.matchesDateFilter(fortyDaysAgo, CallDateFilter.LAST_30_DAYS, nowMillis = nowMillis))

    val utcStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      set(2026, Calendar.NOVEMBER, 19, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val utcEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      set(2026, Calendar.NOVEMBER, 21, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    assertTrue(
      CallFilterMatcher.matchesDateFilter(
        timestampMillis = threeDaysAgo,
        filter = CallDateFilter.CUSTOM,
        customStartUtcMillis = utcStart,
        customEndUtcMillis = utcEnd,
        nowMillis = nowMillis
      )
    )
    assertFalse(
      CallFilterMatcher.matchesDateFilter(
        timestampMillis = nowMillis,
        filter = CallDateFilter.CUSTOM,
        customStartUtcMillis = utcStart,
        customEndUtcMillis = utcEnd,
        nowMillis = nowMillis
      )
    )
  }
}
