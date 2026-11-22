package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiService"

sealed interface GeminiAdvisorUiState {
  data object Idle : GeminiAdvisorUiState
  data object Loading : GeminiAdvisorUiState
  data class Streaming(val text: String) : GeminiAdvisorUiState
  data class Success(val text: String, val recommendedProduct: String = "") : GeminiAdvisorUiState
  data class Error(val message: String) : GeminiAdvisorUiState
}

data class StructuredCallInsight(
  val replyText: String,
  val mainTopic: String,
  val actionItems: List<String>,
  val sentiment: String,
  val summaryFormatted: String,
  val urgency: String,
  val requiresEscalation: Boolean,
  val escalationReason: String
)

object GeminiAiService {

  private val chatModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content {
        text(
          "You are Kai, the Ocean Tide Drop AI Surfer Advisor & Receptionist representing https://otdaisurfer.surf (GitHub: otdaisurfer/AISurfer-). " +
          "Offer practical, crisp business AI guidance covering Free AI Wave Check™ ($0), AI Opportunity Report™, Wave Starter™ ($497 sprints: Wave Scout, Sales Rider, Content Creator, Customer Care Cove), " +
          "Wave Builder ($1,997: AEO Blueprint, Automation Architect), and Big Kahuna™ / Tsunami Growth ($3,997). " +
          "Keep tone warm, sharp, coastal-confident, and concrete."
        )
      }
    )
  }

  private val callInsightSchema = Schema.obj(
    properties = mapOf(
      "replyText" to Schema.string(description = "Warm, concise spoken response from Kai addressing the caller"),
      "mainTopic" to Schema.string(description = "Short main topic label (e.g. Wave Starter Sprint, Urgent Escalation, Strategy Booking)"),
      "actionItems" to Schema.array(
        items = Schema.string(),
        description = "1 to 3 concrete follow-up action items for the team"
      ),
      "sentiment" to Schema.enumeration(
        values = listOf("Positive", "Neutral", "Excited VIP", "Urgent / Frustrated"),
        description = "Overall sentiment of the caller conversation"
      ),
      "urgency" to Schema.enumeration(
        values = listOf("LOW", "MEDIUM", "HIGH", "URGENT_VIP"),
        description = "Priority level for human crew follow-up"
      ),
      "requiresEscalation" to Schema.boolean(description = "True if caller has urgent issue, requests a human/manager, or high-value enterprise inquiry"),
      "escalationReason" to Schema.string(description = "Brief reason when requiresEscalation is true, empty otherwise")
    ),
    optionalProperties = listOf("escalationReason")
  )

  private val structuredModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = callInsightSchema
      },
      systemInstruction = content {
        text(
          "You are Kai, AI Phone Receptionist for Ocean Tide Drop AI Surfer (https://otdaisurfer.surf). " +
            "Respond strictly with valid JSON matching the schema."
        )
      }
    )
  }

  fun isFirebaseAvailable(context: Context? = null): Boolean {
    return try {
      if (context != null) {
        FirebaseApp.getApps(context).isNotEmpty()
      } else {
        FirebaseApp.getInstance()
        true
      }
    } catch (_: Exception) {
      false
    }
  }

  fun streamWaveAdvisor(
    businessContext: String,
    bottleneck: String,
    products: List<ProductEntity>
  ): Flow<GeminiAdvisorUiState> = flow {
    emit(GeminiAdvisorUiState.Loading)
    val catalogSummary = products.take(12).joinToString("\n") {
      "- ${it.name} ($${it.price.toInt()}): ${it.description}"
    }
    val prompt = """
      Analyze this client inquiry for Ocean Tide Drop AI Surfer (https://otdaisurfer.surf):
      Client Business / Question: "$businessContext"
      Primary Bottleneck: "$bottleneck"

      Official Catalog:
      $catalogSummary

      Provide:
      1. Recommended Ocean Tide Drop product/stage and why
      2. 3 concrete first-week AI implementation steps
      3. Expected ROI impact & next CTA (Free Wave Check at otdaisurfer.surf/wave-check, Stripe $497 sprint, or Calendly strategy call).
    """.trimIndent()

    val accumulated = StringBuilder()
    try {
      withTimeout(60_000L) {
        chatModel.generateContentStream(prompt).collect { chunk ->
          val chunkText = chunk.text.orEmpty()
          if (chunkText.isNotEmpty()) {
            accumulated.append(chunkText)
            emit(GeminiAdvisorUiState.Streaming(accumulated.toString()))
          }
        }
      }
      val finalText = accumulated.toString().trim()
      if (finalText.isNotBlank()) {
        val matched = products.firstOrNull { finalText.contains(it.name.removeSuffix("™"), ignoreCase = true) }?.name ?: "Wave Starter™ Sprint"
        emit(GeminiAdvisorUiState.Success(finalText, recommendedProduct = matched))
      } else {
        emit(GeminiAdvisorUiState.Error("Gemini returned an empty response."))
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Gemini streaming timed out", e)
      emit(GeminiAdvisorUiState.Error("AI Advisor timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI generation failed", e)
      emit(GeminiAdvisorUiState.Error(e.localizedMessage ?: "Gemini AI generation failed."))
    }
  }

  suspend fun generateStructuredCallInsight(
    callerName: String,
    callerNumber: String,
    question: String,
    products: List<ProductEntity>,
    faqs: List<FaqEntity>,
    context: Context? = null,
    timeoutMs: Long = 45_000L,
    returningContext: String = ""
  ): StructuredCallInsight? {
    if (context != null) {
      try {
        if (FirebaseApp.getApps(context).isEmpty()) {
          FirebaseApp.initializeApp(context)
        }
      } catch (_: Exception) {
        return null
      }
    }
    if (!isFirebaseAvailable(context)) return null
    return try {
      val catalog = products.take(10).joinToString("\n") { "- ${it.name} ($${it.price.toInt()}): ${it.description}" }
      val faqText = faqs.take(6).joinToString("\n") { "- Q: ${it.question} A: ${it.answer}" }
      val vipBlock = if (returningContext.isNotBlank()) "\n$returningContext\n" else ""

      val prompt = """
        You are Kai, AI Phone Receptionist for Ocean Tide Drop AI Surfer (https://otdaisurfer.surf, repo: otdaisurfer/AISurfer-).
        Caller: $callerName ($callerNumber)$vipBlock
        Question: "$question"

        Catalog:
        $catalog

        FAQs:
        $faqText

        Reply as Kai and return structured JSON capturing mainTopic, actionItems, sentiment, urgency, and requiresEscalation.
      """.trimIndent()

      val response = withTimeout(timeoutMs) {
        structuredModel.generateContent(prompt)
      }
      val jsonText = response.text ?: return null
      val json = JSONObject(jsonText)
      val replyText = json.optString("replyText").ifBlank { return null }
      val mainTopic = json.optString("mainTopic", "General Inquiry")
      val actionsArray = json.optJSONArray("actionItems")
      val actions = buildList {
        if (actionsArray != null) {
          for (i in 0 until actionsArray.length()) {
            val item = actionsArray.optString(i).trim()
            if (item.isNotBlank()) add(item)
          }
        }
      }.ifEmpty { listOf("Follow up with $callerName via email or callback") }
      val sentiment = json.optString("sentiment", "Positive")
      val urgency = json.optString("urgency", "MEDIUM")
      val requiresEscalation = json.optBoolean("requiresEscalation", false)
      val escalationReason = json.optString("escalationReason", "")

      val formattedSummary =
        "Topic: $mainTopic | Action Items: ${actions.joinToString("; ")} | Sentiment: $sentiment"

      StructuredCallInsight(
        replyText = replyText,
        mainTopic = mainTopic,
        actionItems = actions,
        sentiment = sentiment,
        summaryFormatted = formattedSummary,
        urgency = urgency,
        requiresEscalation = requiresEscalation,
        escalationReason = escalationReason
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "Structured Gemini call insight timed out after ${timeoutMs}ms; using fallback engine.")
      null
    } catch (e: Exception) {
      Log.w(TAG, "Structured Gemini call insight unavailable (${e.javaClass.simpleName}: ${e.localizedMessage}); using fallback engine.")
      null
    }
  }

  /**
   * Gemini Flash summarization workflow for BroadcastReceiver goAsync() execution windows.
   */
  suspend fun summarizeIncomingCallWithGeminiFlash(
    context: Context,
    callerName: String,
    callerNumber: String,
    callNotesOrQuestion: String,
    products: List<ProductEntity>,
    faqs: List<FaqEntity>,
    returningContext: String = ""
  ): ReceptionistResult {
    val insight = generateStructuredCallInsight(
      callerName = callerName,
      callerNumber = callerNumber,
      question = callNotesOrQuestion,
      products = products,
      faqs = faqs,
      context = context,
      timeoutMs = 30_000L,
      returningContext = returningContext
    )
    if (insight != null) {
      return ReceptionistResult(
        responseText = insight.replyText,
        summary = insight.summaryFormatted,
        urgency = insight.urgency,
        topic = insight.mainTopic.uppercase().replace(' ', '_').take(40),
        requiresImmediateEscalation = insight.requiresEscalation,
        escalationReason = insight.escalationReason
      )
    }

    // Fallback to deterministic structured summarization if Gemini Flash is unreachable/offline
    return SurferAiReceptionist.respondToCall(
      callerName = callerName,
      callerNumber = callerNumber,
      callerQuestion = callNotesOrQuestion,
      products = products,
      faqs = faqs,
      returningContext = returningContext
    )
  }
}
