package com.example

import com.example.ai.SurferAiReceptionist
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.example.integrations.ChatGptConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SurferAiReceptionistTest {

  private val receptionist = SurferAiReceptionist
  private val testProducts = listOf(
    ProductEntity(
      id = 1,
      name = "Free AI Wave Check™",
      price = 0.0,
      category = "Diagnostic",
      description = "Free 5-question audit diagnosing business AI opportunities.",
      popularBadge = "FREE AUDIT",
      surfLevel = "Stage: Discover",
      ctaText = "Start Free Wave Check",
      directUrl = "https://otdaisurfer.surf/wave-check",
      slug = "wave-check"
    ),
    ProductEntity(
      id = 2,
      name = "Wave Starter™ Sprint",
      price = 497.0,
      category = "AI Implementation Sprint",
      description = "Single-agent sprint delivered in 3-5 days.",
      popularBadge = "POPULAR $497",
      surfLevel = "Stage: Implement",
      ctaText = "Buy Wave Starter ($497)",
      directUrl = "https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b",
      slug = "wave-starter"
    ),
    ProductEntity(
      id = 3,
      name = "Big Kahuna™ / Tsunami Growth",
      price = 3997.0,
      category = "Enterprise AI Transformation",
      description = "Multi-agent transformation across marketing, sales, and operations.",
      popularBadge = "COMPLETE SUITE",
      surfLevel = "Stage: Transform",
      ctaText = "Apply for Transformation",
      directUrl = "https://otdaisurfer.surf/big-kahuna",
      slug = "big-kahuna"
    )
  )

  private val testFaqs = listOf(
    FaqEntity(
      id = 1,
      category = "Wave Starter",
      question = "What is included in the $497 Wave Starter™?",
      answer = "A high-impact implementation sprint (Wave Scout, Sales Rider, Content Creator, or Customer Care Cove) configured and delivered in 3-5 days.",
      tags = "wave starter, 497, sprint, implementation"
    )
  )

  @Before
  fun setup() {
    // SurferAiReceptionist is an object singleton
  }

  @Test
  fun testFreeWaveCheckInquiry() = runBlocking {
    val result = receptionist.respondToCall(
      callerName = "John",
      callerNumber = "+1 (555) 123-4567",
      callerQuestion = "Can I get a free AI Wave Check audit?",
      products = testProducts,
      faqs = testFaqs,
      config = ChatGptConfig()
    )

    assertTrue(result.responseText.contains("Wave Check") || result.responseText.contains("Free"))
    assertFalse(result.requiresImmediateEscalation)
  }

  @Test
  fun testWaveStarterPricingInquiry() = runBlocking {
    val result = receptionist.respondToCall(
      callerName = "Sarah",
      callerNumber = "+1 (555) 987-6543",
      callerQuestion = "How much does the Wave Starter sprint cost?",
      products = testProducts,
      faqs = testFaqs,
      config = ChatGptConfig()
    )

    assertTrue(result.responseText.contains("497") || result.responseText.contains("Wave Starter"))
  }

  @Test
  fun testUrgentEscalationDetection() = runBlocking {
    val result = receptionist.respondToCall(
      callerName = "Executive Client",
      callerNumber = "+1 (555) 000-9999",
      callerQuestion = "This is urgent! Our server is down, please escalate immediately!",
      products = testProducts,
      faqs = testFaqs,
      config = ChatGptConfig()
    )

    assertTrue(result.requiresImmediateEscalation)
    assertTrue(result.urgency == "URGENT_VIP")
    assertTrue(result.escalationReason.isNotBlank())
  }

  @Test
  fun testGitHubRepositoryInquiry() = runBlocking {
    val result = receptionist.respondToCall(
      callerName = "Developer",
      callerNumber = "+1 (555) 333-4444",
      callerQuestion = "Where can I find the official GitHub repository for AISurfer?",
      products = testProducts,
      faqs = testFaqs,
      config = ChatGptConfig()
    )

    assertTrue(result.responseText.contains("otdaisurfer/AISurfer-"))
    assertTrue(result.summary.contains("Topic:") && result.summary.contains("Action Items:") && result.summary.contains("Sentiment:"))
  }
}
