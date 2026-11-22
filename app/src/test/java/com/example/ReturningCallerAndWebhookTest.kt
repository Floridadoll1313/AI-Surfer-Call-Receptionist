package com.example

import com.example.ai.SurferAiReceptionist
import com.example.data.ReturningCallerMatcher
import com.example.data.local.CallEntity
import com.example.data.local.WaveAuditLeadEntity
import com.example.integrations.ChatGptConfig
import com.example.telephony.EscalationWebhookDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReturningCallerAndWebhookTest {

  @Test
  fun normalizeAndMatchReturningCallerAcrossCallsAndLeads() {
    assertEquals("8085551234", ReturningCallerMatcher.normalizePhone("+1 (808) 555-1234"))

    val priorCall = CallEntity(
      id = 42L,
      callerNumber = "+1 (808) 555-1234",
      callerName = "Kaiola Brooks",
      summary = "Topic: Big Kahuna | Action Items: Book call | Sentiment: Excited VIP",
      transcription = "Caller asked about Big Kahuna",
      customerQuestion = "Can we schedule Big Kahuna onboarding?",
      aiResponse = "Aloha!",
      urgency = "URGENT_VIP",
      topic = "ENTERPRISE_AI"
    )
    val priorLead = WaveAuditLeadEntity(
      id = "lead-1",
      businessName = "Maui Surf Co",
      contactName = "Kaiola Brooks",
      contactEmail = "kaiola@mauisurf.example",
      contactPhone = "808-555-1234",
      biggestBottleneck = "Multi-Agent Enterprise Ops",
      recommendedProduct = "Big Kahuna™ / Tsunami Growth ($3,997)",
      syncedToSupabase = true
    )

    val matched = ReturningCallerMatcher.match("8085551234", listOf(priorCall), listOf(priorLead))
    assertTrue(matched.isVipOrReturning)
    assertEquals("Kaiola Brooks", matched.matchedName)
    assertEquals("Maui Surf Co", matched.businessName)
    assertEquals(1, matched.priorCallCount)
    assertTrue(matched.toBadgeText().contains("VIP Returning Caller"))
    assertTrue(matched.toPromptContext().contains("Maui Surf Co"))

    val unknown = ReturningCallerMatcher.match("+1 (555) 999-0000", listOf(priorCall), listOf(priorLead))
    assertFalse(unknown.isVipOrReturning)
    assertEquals("", unknown.toBadgeText())
  }

  @Test
  fun webhookPayloadIncludesSlackCompatibleTextAndStructuredFields() {
    val call = CallEntity(
      id = 7L,
      callerNumber = "+1 (808) 555-SURF",
      callerName = "Makena Kai",
      summary = "Topic: VIP | Action Items: Call back | Sentiment: Urgent",
      transcription = "Urgent request",
      customerQuestion = "Need help right now",
      aiResponse = "Flagged for escalation",
      urgency = "URGENT_VIP",
      topic = "HIGH_PRIORITY"
    )

    val json = EscalationWebhookDispatcher.buildPayload(
      call = call,
      reason = "VIP SLA trigger",
      targetEmail = "oceantidedropservice@gmail.com"
    )
    assertEquals("ocean_surfer_escalation", json.getString("event"))
    assertEquals("Makena Kai", json.getString("callerName"))
    assertEquals("URGENT_VIP", json.getString("urgency"))
    assertTrue(json.getString("text").contains("Makena Kai"))
    assertTrue(json.getString("text").contains("VIP SLA trigger"))
  }

  @Test
  fun returningContextEnrichesReceptionistReply() = runBlocking {
    val result = SurferAiReceptionist.respondToCall(
      callerName = "Kaiola",
      callerNumber = "+1 (808) 555-1234",
      callerQuestion = "What are your hours?",
      products = emptyList(),
      faqs = emptyList(),
      config = ChatGptConfig(),
      returningContext = "Returning Caller Profile: Name=Kaiola; Business=Maui Surf Co; PriorCalls=1"
    )

    assertTrue(result.responseText.contains("Welcome back to Ocean Tide Drop, Kaiola!"))
  }
}
