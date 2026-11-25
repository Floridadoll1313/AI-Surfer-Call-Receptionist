package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.CallEntity
import com.example.data.local.ProductEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiInstrumentsEngine"

enum class AiInstrumentTool(
  val title: String,
  val badge: String,
  val subtitle: String
) {
  WAVE_ADVISOR("Wave Advisor", "Stream", "Real-time Gemini 3.8 Flash strategy stream"),
  COPILOT_CHAT("Copilot Chat", "Multi-Turn", "Stateful conversation with Kai (startChat)"),
  VISION_SCANNER("Vision & Doc Scan", "Multimodal", "Analyze photos, flyers & business cards"),
  IMAGE_STUDIO("Image Studio", "Nano Banana", "Create & edit visuals (gemini-nano-banana-2.1)"),
  PITCH_BUILDER("Pitch & Email", "JSON Schema", "Structured sales pitch & follow-up email"),
  CALL_ANALYST("Call Analyst", "Queue Brief", "Analyze real Phone 1 & Phone 2 calls"),
  TRANSLATOR_VOICE("Translate & Voice", "Flash-Lite", "Instant translation, tone rewrite & Kai TTS")
}

data class CopilotChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val isFromUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

sealed interface InstrumentTextUiState {
  data object Idle : InstrumentTextUiState
  data object Loading : InstrumentTextUiState
  data class Success(val text: String, val headline: String = "") : InstrumentTextUiState
  data class Error(val message: String) : InstrumentTextUiState
}

sealed interface ImageGenUiState {
  data object Idle : ImageGenUiState
  data object Loading : ImageGenUiState
  data class Success(val bitmap: Bitmap, val prompt: String) : ImageGenUiState
  data class Error(val message: String) : ImageGenUiState
}

data class SalesPitchBrief(
  val headline: String,
  val recommendedOffer: String,
  val talkingPoints: List<String>,
  val objectionReply: String,
  val emailSubject: String,
  val emailBody: String
)

sealed interface PitchBriefUiState {
  data object Idle : PitchBriefUiState
  data object Loading : PitchBriefUiState
  data class Success(val brief: SalesPitchBrief) : PitchBriefUiState
  data class Error(val message: String) : PitchBriefUiState
}

data class CallQueueBrief(
  val executiveSummary: String,
  val urgentCallers: List<String>,
  val nextActions: List<String>,
  val draftFollowUpEmail: String
)

sealed interface CallQueueBriefUiState {
  data object Idle : CallQueueBriefUiState
  data object Loading : CallQueueBriefUiState
  data class Success(val brief: CallQueueBrief) : CallQueueBriefUiState
  data class Error(val message: String) : CallQueueBriefUiState
}

object AiInstrumentsEngine {

  private val copilotModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content {
        text(
          "You are Kai, Senior AI Copilot for Ocean Tide Drop AI Surfer (https://otdaisurfer.surf). " +
            "Help business owners with AI automation, call handling, sales scripting, AEO visibility, and client delivery. " +
            "Keep answers concise, actionable, and tailored to Ocean Tide Drop products."
        )
      }
    )
  }

  private val visionModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content {
        text(
          "You are Kai's Multimodal Business Scanner for Ocean Tide Drop AI Surfer. " +
            "Extract key contact info, offers, bottlenecks, pricing opportunities, or visual improvements clearly in bullet points."
        )
      }
    )
  }

  private val fastLanguageModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      systemInstruction = content {
        text(
          "You are a fast, accurate multilingual business translator and tone editor for Ocean Tide Drop AI Surfer. " +
            "Return only the polished translated or rewritten message without preamble."
        )
      }
    )
  }

  private val imageGenModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-nano-banana-2.1",
      generationConfig = generationConfig {
        responseModalities = listOf(ResponseModality.IMAGE)
      }
    )
  }

  private val pitchSchema = Schema.obj(
    properties = mapOf(
      "headline" to Schema.string(description = "Punchy value-proposition hook"),
      "recommendedOffer" to Schema.string(description = "Best matching Ocean Tide Drop package"),
      "talkingPoints" to Schema.array(items = Schema.string(), description = "3 sharp sales talking points"),
      "objectionReply" to Schema.string(description = "Concise reply to price/timing objections"),
      "emailSubject" to Schema.string(description = "High-open-rate follow-up email subject"),
      "emailBody" to Schema.string(description = "Ready-to-send client follow-up email")
    )
  )

  private val pitchModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = pitchSchema
      }
    )
  }

  private val callQueueSchema = Schema.obj(
    properties = mapOf(
      "executiveSummary" to Schema.string(description = "Overview of real call queue health across Phone 1 and Phone 2"),
      "urgentCallers" to Schema.array(items = Schema.string(), description = "Priority callers needing immediate callback"),
      "nextActions" to Schema.array(items = Schema.string(), description = "Ordered crew action items"),
      "draftFollowUpEmail" to Schema.string(description = "Ready-to-send summary email for the team")
    )
  )

  private val callQueueModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = callQueueSchema
      }
    )
  }

  private var activeChat: Chat? = null

  private fun ensureFirebaseReady(context: Context): Boolean {
    return try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context) != null
      } else {
        true
      }
    } catch (_: Exception) {
      false
    }
  }

  fun resetCopilotChat() {
    activeChat = null
  }

  suspend fun sendCopilotMessage(context: Context, userMessage: String): Result<String> {
    if (!ensureFirebaseReady(context)) {
      return Result.failure(IllegalStateException("Firebase AI Logic is not initialized yet."))
    }
    return try {
      val chat = activeChat ?: copilotModel.startChat(
        history = listOf(
          content("user") { text("Introduce yourself briefly as Kai Copilot for Ocean Tide Drop AI Surfer.") },
          content("model") { text("Aloha! I'm Kai Copilot. Ask me anything about client AI workflows, sales calls, or automating your business.") }
        )
      ).also { activeChat = it }

      val response = withTimeout(60_000L) {
        chat.sendMessage(userMessage)
      }
      val text = response.text?.trim()
      if (!text.isNullOrBlank()) {
        Result.success(text)
      } else {
        Result.failure(IllegalStateException("Gemini returned an empty reply."))
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Copilot chat timed out", e)
      Result.failure(Exception("Copilot timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Copilot chat failed", e)
      Result.failure(e)
    }
  }

  /**
   * Scales down large Bitmaps cleanly so inline multimodal uploads stay well under 20MB.
   */
  fun scaleBitmapForInference(bitmap: Bitmap, maxDim: Int = 1280): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = minOf(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat())
    val targetW = (w * ratio).toInt().coerceAtLeast(1)
    val targetH = (h * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  suspend fun analyzeImage(
    context: Context,
    bitmap: Bitmap,
    instruction: String
  ): InstrumentTextUiState {
    if (!ensureFirebaseReady(context)) {
      return InstrumentTextUiState.Error("Firebase AI Logic is not initialized yet.")
    }
    val prepared = scaleBitmapForInference(bitmap)
    val query = instruction.trim().ifBlank {
      "Analyze this image for business info, lead details, marketing takeaways, and recommended Ocean Tide Drop AI actions."
    }
    return try {
      val response = withTimeout(60_000L) {
        visionModel.generateContent(
          content {
            image(prepared)
            text(query)
          }
        )
      }
      val text = response.text?.trim()
      if (!text.isNullOrBlank()) {
        InstrumentTextUiState.Success(text = text, headline = "Multimodal Vision Analysis")
      } else {
        InstrumentTextUiState.Error("Gemini Vision returned an empty response.")
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Vision analysis timed out", e)
      InstrumentTextUiState.Error("Vision analysis timed out after 60 seconds.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Vision analysis failed", e)
      InstrumentTextUiState.Error(e.localizedMessage ?: "Vision analysis failed.")
    }
  }

  suspend fun generateOrEditImage(
    context: Context,
    prompt: String,
    sourceBitmap: Bitmap? = null
  ): ImageGenUiState {
    if (!ensureFirebaseReady(context)) {
      return ImageGenUiState.Error("Firebase AI Logic is not initialized yet.")
    }
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank()) {
      return ImageGenUiState.Error("Enter an image prompt first.")
    }
    return try {
      val response = withTimeout(60_000L) {
        if (sourceBitmap != null) {
          val prepared = scaleBitmapForInference(sourceBitmap, maxDim = 1024)
          imageGenModel.generateContent(
            content {
              image(prepared)
              text(cleanPrompt)
            }
          )
        } else {
          imageGenModel.generateContent(cleanPrompt)
        }
      }
      val imagePart = response.candidates.firstOrNull()
        ?.content?.parts
        ?.filterIsInstance<com.google.firebase.ai.type.ImagePart>()
        ?.firstOrNull()

      val generatedBitmap = imagePart?.image
      if (generatedBitmap != null) {
        ImageGenUiState.Success(bitmap = generatedBitmap, prompt = cleanPrompt)
      } else {
        ImageGenUiState.Error("No image was returned by Nano Banana 2.1.")
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Image generation timed out", e)
      ImageGenUiState.Error("Image generation timed out after 60 seconds.")
    } catch (e: QuotaExceededException) {
      Log.e(TAG, "Image generation quota exceeded", e)
      ImageGenUiState.Error("Rate limit or Firebase Blaze quota exceeded for image generation.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Image generation failed", e)
      ImageGenUiState.Error(e.localizedMessage ?: "Image generation failed.")
    }
  }

  suspend fun generateSalesPitchBrief(
    context: Context,
    prospectBusiness: String,
    painPoint: String,
    products: List<ProductEntity>
  ): PitchBriefUiState {
    if (!ensureFirebaseReady(context)) {
      return PitchBriefUiState.Error("Firebase AI Logic is not initialized yet.")
    }
    val catalog = products.take(10).joinToString("\n") {
      "- ${it.name} ($${it.price.toInt()}): ${it.description}"
    }
    val prompt = """
      Build a structured Ocean Tide Drop AI Surfer sales pitch & follow-up email.
      Prospect / Industry: "$prospectBusiness"
      Key Bottleneck: "$painPoint"
      Available Packages:
      $catalog
    """.trimIndent()
    return try {
      val response = withTimeout(60_000L) {
        pitchModel.generateContent(prompt)
      }
      val rawJson = response.text ?: return PitchBriefUiState.Error("Model returned empty JSON.")
      val json = JSONObject(rawJson)
      val pointsArray = json.optJSONArray("talkingPoints")
      val points = buildList {
        if (pointsArray != null) {
          for (i in 0 until pointsArray.length()) {
            val item = pointsArray.optString(i).trim()
            if (item.isNotBlank()) add(item)
          }
        }
      }
      PitchBriefUiState.Success(
        SalesPitchBrief(
          headline = json.optString("headline", "Ride the AI Wave"),
          recommendedOffer = json.optString("recommendedOffer", "Wave Starter™ Sprint ($497)"),
          talkingPoints = points,
          objectionReply = json.optString("objectionReply", ""),
          emailSubject = json.optString("emailSubject", "AI Gameplan for $prospectBusiness"),
          emailBody = json.optString("emailBody", "")
        )
      )
    } catch (e: TimeoutCancellationException) {
      PitchBriefUiState.Error("Pitch generator timed out after 60 seconds.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Pitch brief generation failed", e)
      PitchBriefUiState.Error(e.localizedMessage ?: "Failed to generate sales brief.")
    }
  }

  suspend fun analyzeCallQueue(
    context: Context,
    calls: List<CallEntity>
  ): CallQueueBriefUiState {
    if (!ensureFirebaseReady(context)) {
      return CallQueueBriefUiState.Error("Firebase AI Logic is not initialized yet.")
    }
    if (calls.isEmpty()) {
      return CallQueueBriefUiState.Error("No real calls logged yet on Phone 1 or Phone 2 to analyze.")
    }
    val callDigest = calls.take(15).joinToString("\n") { call ->
      "- [${call.receivingLine}] ${call.callerName} (${call.callerNumber}) | Status: ${call.status} | Urgency: ${call.urgency} | Q: ${call.customerQuestion} | Summary: ${call.summary}"
    }
    val prompt = """
      Analyze these real logged calls across Phone 1 and Phone 2 for Ocean Tide Drop AI Surfer and produce an executive briefing:
      $callDigest
    """.trimIndent()
    return try {
      val response = withTimeout(60_000L) {
        callQueueModel.generateContent(prompt)
      }
      val rawJson = response.text ?: return CallQueueBriefUiState.Error("Model returned empty JSON.")
      val json = JSONObject(rawJson)
      val urgentArr = json.optJSONArray("urgentCallers")
      val urgentList = buildList {
        if (urgentArr != null) {
          for (i in 0 until urgentArr.length()) {
            urgentArr.optString(i).trim().takeIf { it.isNotBlank() }?.let { add(it) }
          }
        }
      }
      val actionsArr = json.optJSONArray("nextActions")
      val actionsList = buildList {
        if (actionsArr != null) {
          for (i in 0 until actionsArr.length()) {
            actionsArr.optString(i).trim().takeIf { it.isNotBlank() }?.let { add(it) }
          }
        }
      }
      CallQueueBriefUiState.Success(
        CallQueueBrief(
          executiveSummary = json.optString("executiveSummary", ""),
          urgentCallers = urgentList,
          nextActions = actionsList,
          draftFollowUpEmail = json.optString("draftFollowUpEmail", "")
        )
      )
    } catch (e: TimeoutCancellationException) {
      CallQueueBriefUiState.Error("Call queue analysis timed out after 60 seconds.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Call queue analysis failed", e)
      CallQueueBriefUiState.Error(e.localizedMessage ?: "Failed to analyze call queue.")
    }
  }

  suspend fun translateOrRewrite(
    context: Context,
    sourceText: String,
    targetMode: String
  ): InstrumentTextUiState {
    if (!ensureFirebaseReady(context)) {
      return InstrumentTextUiState.Error("Firebase AI Logic is not initialized yet.")
    }
    val clean = sourceText.trim()
    if (clean.isBlank()) {
      return InstrumentTextUiState.Error("Enter or dictate a message first.")
    }
    val prompt = "Transform or translate the following business message for target style/language '$targetMode':\n\n$clean"
    return try {
      val response = withTimeout(45_000L) {
        fastLanguageModel.generateContent(prompt)
      }
      val text = response.text?.trim()
      if (!text.isNullOrBlank()) {
        InstrumentTextUiState.Success(text = text, headline = targetMode)
      } else {
        InstrumentTextUiState.Error("Gemini Flash-Lite returned an empty response.")
      }
    } catch (e: TimeoutCancellationException) {
      InstrumentTextUiState.Error("Translation timed out.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Translation failed", e)
      InstrumentTextUiState.Error(e.localizedMessage ?: "Translation failed.")
    }
  }
}
