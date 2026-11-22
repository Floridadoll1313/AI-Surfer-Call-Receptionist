package com.example

import android.content.Context
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ReceptionistResult
import com.example.telephony.CallStateReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallStateReceiverTest {

  @Test
  fun incomingCallRunsGeminiFlashSummarizationBeforeSavingToSupabase() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val executionOrder = mutableListOf<String>()
    var savedSummaryInSupabase = ""

    suspend fun event(state: String) = CallStateReceiver.processPhoneStateChange(
      context = context,
      state = state,
      incomingNumber = "+1 (808) 555-7744",
      callerNameOverride = "Koa Martinez",
      transcriptOverride = "Urgent VIP: Looking for Big Kahuna $3,997 multi-agent setup ASAP.",
      geminiSummarizer = { _, callerName, _, _, _, _ ->
        executionOrder.add("gemini_flash_summarize")
        ReceptionistResult(
          responseText = "Aloha $callerName! Flagged for VIP onboarding.",
          summary = "Topic: Big Kahuna VIP | Action Items: Call $callerName back ASAP | Sentiment: Positive (High Intent)",
          urgency = "URGENT_VIP",
          topic = "ENTERPRISE_AI",
          requiresImmediateEscalation = true,
          escalationReason = "Gemini Flash detected VIP enterprise request."
        )
      },
      supabaseSaver = { _, call ->
        executionOrder.add("supabase_save")
        savedSummaryInSupabase = call.summary
        Result.success(Unit)
      }
    )

    assertEquals(null, event(TelephonyManager.EXTRA_STATE_RINGING))
    assertEquals(null, event(TelephonyManager.EXTRA_STATE_RINGING))
    assertEquals(null, event(TelephonyManager.EXTRA_STATE_OFFHOOK))
    val resultCall = event(TelephonyManager.EXTRA_STATE_IDLE)
    assertEquals(null, event(TelephonyManager.EXTRA_STATE_IDLE))
    assertNotNull(resultCall)
    assertEquals(listOf("gemini_flash_summarize", "supabase_save"), executionOrder)
    assertTrue(savedSummaryInSupabase.contains("Topic: Big Kahuna VIP"))
    assertEquals("ESCALATED", resultCall!!.status)
    assertTrue(resultCall.isEscalated)
    assertEquals("+1 (808) 555-7744", resultCall.callerNumber)
  }
}
