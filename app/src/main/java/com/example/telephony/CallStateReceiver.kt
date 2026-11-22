package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.OceanSurferApp
import com.example.ai.GeminiAiService
import com.example.ai.ReceptionistResult
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.data.local.CallEntity
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.example.integrations.IntegrationPrefsStore
import com.example.integrations.SupabaseCallSyncWorker
import com.example.integrations.SupabaseClient
import com.example.integrations.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
    @Suppress("DEPRECATION")
    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
      ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
      ?: ""
    val extraCallerName = intent.getStringExtra(EXTRA_CALLER_NAME)
    val extraTranscript = intent.getStringExtra(EXTRA_CALL_TRANSCRIPT)

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        processPhoneStateChange(
          context = context,
          state = state,
          incomingNumber = incomingNumber,
          callerNameOverride = extraCallerName,
          transcriptOverride = extraTranscript
        )
      } catch (e: Exception) {
        Log.e(TAG, "Error processing incoming call broadcast", e)
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    private const val TAG = "CallStateReceiver"
    private const val PREFS_CALL_TRACKER = "ocean_surfer_call_tracker"
    private const val KEY_ACTIVE_NUMBER = "active_number"
    private const val KEY_ACTIVE_NAME = "active_name"
    private const val KEY_RING_TIME = "ring_time_ms"
    private const val KEY_OFFHOOK_TIME = "offhook_time_ms"
    private const val KEY_WAS_RINGING = "was_ringing"

    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_CALL_TRANSCRIPT = "extra_call_transcript"

    suspend fun processPhoneStateChange(
      context: Context,
      state: String,
      incomingNumber: String,
      callerNameOverride: String? = null,
      transcriptOverride: String? = null,
      geminiSummarizer: suspend (
        ctx: Context,
        callerName: String,
        callerNumber: String,
        questionOrTranscript: String,
        products: List<ProductEntity>,
        faqs: List<FaqEntity>
      ) -> ReceptionistResult = { ctx, name, number, query, products, faqs ->
        GeminiAiService.summarizeIncomingCallWithGeminiFlash(
          context = ctx,
          callerName = name,
          callerNumber = number,
          callNotesOrQuestion = query,
          products = products,
          faqs = faqs
        )
      },
      supabaseSaver: suspend (SupabaseConfig, CallEntity) -> Result<Unit> = { cfg, call ->
        SupabaseClient.syncCallRecord(cfg, call)
      }
    ): CallEntity? {
      val prefs = context.getSharedPreferences(PREFS_CALL_TRACKER, Context.MODE_PRIVATE)
      val app = context.applicationContext as? OceanSurferApp
      val database = app?.database ?: AppDatabase.getDatabase(context)
      val repository = AppRepository(database)

      when (state) {
        TelephonyManager.EXTRA_STATE_RINGING -> {
          val resolvedNumber = incomingNumber.ifBlank {
            prefs.getString(KEY_ACTIVE_NUMBER, null) ?: "Unknown Caller"
          }
          val resolvedName = callerNameOverride?.takeIf { it.isNotBlank() } ?: "Cell Phone Caller"
          val now = System.currentTimeMillis()

          // Avoid duplicate RINGING broadcasts fired back-to-back by Android telephony without a new number
          val lastNumber = prefs.getString(KEY_ACTIVE_NUMBER, null)
          val lastRing = prefs.getLong(KEY_RING_TIME, 0L)
          if (incomingNumber.isBlank() && lastNumber != null && (now - lastRing) < 2_500L) {
            return null
          }

          prefs.edit()
            .putString(KEY_ACTIVE_NUMBER, resolvedNumber)
            .putString(KEY_ACTIVE_NAME, resolvedName)
            .putLong(KEY_RING_TIME, now)
            .putLong(KEY_OFFHOOK_TIME, 0L)
            .putBoolean(KEY_WAS_RINGING, true)
            .commit()

          val questionOrTranscript = transcriptOverride?.takeIf { it.isNotBlank() }
            ?: "Incoming phone call detected from $resolvedNumber. Summarize intent, action items, and triage urgency."

          return summarizeWithGeminiAndSaveToSupabase(
            context = context,
            repository = repository,
            callerName = resolvedName,
            callerNumber = resolvedNumber,
            questionOrTranscript = questionOrTranscript,
            durationSeconds = 0,
            geminiSummarizer = geminiSummarizer,
            supabaseSaver = supabaseSaver
          )
        }

        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
          if (prefs.getBoolean(KEY_WAS_RINGING, false)) {
            prefs.edit()
              .putLong(KEY_OFFHOOK_TIME, System.currentTimeMillis())
              .commit()
          }
          return null
        }

        TelephonyManager.EXTRA_STATE_IDLE -> {
          val wasRinging = prefs.getBoolean(KEY_WAS_RINGING, false)
          val activeNumber = prefs.getString(KEY_ACTIVE_NUMBER, null)
          val activeName = prefs.getString(KEY_ACTIVE_NAME, "Cell Phone Caller") ?: "Cell Phone Caller"
          val offhookMs = prefs.getLong(KEY_OFFHOOK_TIME, 0L)
          val ringMs = prefs.getLong(KEY_RING_TIME, 0L)

          prefs.edit()
            .remove(KEY_WAS_RINGING)
            .remove(KEY_OFFHOOK_TIME)
            .commit()

          if (!wasRinging || activeNumber.isNullOrBlank()) return null

          val endMs = System.currentTimeMillis()
          val durationSec = if (offhookMs > 0L) {
            ((endMs - offhookMs) / 1000L).toInt().coerceAtLeast(1)
          } else {
            ((endMs - ringMs) / 1000L).toInt().coerceAtLeast(0)
          }

          if (durationSec < 2 && transcriptOverride.isNullOrBlank()) return null

          val completedNotes = transcriptOverride?.takeIf { it.isNotBlank() }
            ?: "Completed incoming call with $activeNumber (duration ${durationSec}s). Generate final post-call Gemini Flash summary."

          return summarizeWithGeminiAndSaveToSupabase(
            context = context,
            repository = repository,
            callerName = activeName,
            callerNumber = activeNumber,
            questionOrTranscript = completedNotes,
            durationSeconds = durationSec,
            geminiSummarizer = geminiSummarizer,
            supabaseSaver = supabaseSaver
          )
        }

        else -> return null
      }
    }

    private suspend fun summarizeWithGeminiAndSaveToSupabase(
      context: Context,
      repository: AppRepository,
      callerName: String,
      callerNumber: String,
      questionOrTranscript: String,
      durationSeconds: Int,
      geminiSummarizer: suspend (
        ctx: Context,
        callerName: String,
        callerNumber: String,
        questionOrTranscript: String,
        products: List<ProductEntity>,
        faqs: List<FaqEntity>
      ) -> ReceptionistResult,
      supabaseSaver: suspend (SupabaseConfig, CallEntity) -> Result<Unit>
    ): CallEntity {
      val products = repository.getProductsSnapshot()
      val faqs = repository.getFaqsSnapshot()
      val returningCtx = repository.findReturningCallerContext(callerNumber)
      val resolvedCallerName = if ((callerName == "Cell Phone Caller" || callerName.isBlank()) && !returningCtx.matchedName.isNullOrBlank()) {
        returningCtx.matchedName
      } else {
        callerName
      }
      val promptInput = if (returningCtx.isVipOrReturning) {
        "${returningCtx.toPromptContext()}\nCaller Message: $questionOrTranscript"
      } else {
        questionOrTranscript
      }

      // Step 1: Run AI Summarization Workflow via Gemini Flash BEFORE saving to Supabase
      val aiResult = geminiSummarizer(
        context,
        resolvedCallerName,
        callerNumber,
        promptInput,
        products,
        faqs
      )

      val escalationConfig = IntegrationPrefsStore.loadEscalationConfig(context)
      val shouldEscalate = aiResult.requiresImmediateEscalation ||
        (escalationConfig.escalateOnHighUrgency && aiResult.urgency == "HIGH")
      val alertMinutes = if (shouldEscalate) 3 else 5
      val now = System.currentTimeMillis()
      val deadline = now + (alertMinutes * 60_000L)

      val unsavedCall = CallEntity(
        callerNumber = callerNumber,
        callerName = resolvedCallerName,
        timestamp = now,
        durationSeconds = durationSeconds,
        status = if (shouldEscalate) "ESCALATED" else "PENDING_REVIEW",
        urgency = aiResult.urgency,
        topic = aiResult.topic,
        summary = aiResult.summary,
        transcription = "Gemini Flash Call Workflow\nQuestion/Transcript: $questionOrTranscript\nAI Summary: ${aiResult.summary}",
        customerQuestion = questionOrTranscript,
        aiResponse = aiResult.responseText,
        escalationAlertMinutes = alertMinutes,
        escalationDeadline = deadline,
        isEscalated = shouldEscalate,
        escalatedAt = if (shouldEscalate) now else 0L,
        escalationNotes = if (shouldEscalate) {
          aiResult.escalationReason.ifBlank { "Gemini Flash flagged incoming call for immediate escalation." }
        } else ""
      )

      // Step 2: Save the Gemini-summarized call to Supabase BEFORE/WITH local persistence
      val supabaseConfig = IntegrationPrefsStore.loadSupabaseConfig(context)
      val supabaseResult = supabaseSaver(supabaseConfig, unsavedCall)
      val syncNote = if (supabaseResult.isSuccess) {
        "Synced to Supabase public.calls"
      } else {
        "Queued locally (Supabase sync note: ${supabaseResult.exceptionOrNull()?.localizedMessage ?: "offline"})"
      }

      val persistedEntity = unsavedCall.copy(
        transcription = "${unsavedCall.transcription}\n[$syncNote]",
        supabaseSynced = supabaseResult.isSuccess
      )

      // Step 3: Persist locally in Room so Dashboard updates immediately
      val insertedId = repository.insertCall(persistedEntity)
      val savedCall = persistedEntity.copy(id = insertedId)

      if (!supabaseResult.isSuccess && supabaseConfig.autoSyncCalls) {
        SupabaseCallSyncWorker.enqueueImmediateSync(context)
      }

      if (shouldEscalate && escalationConfig.isEnabled) {
        val sendNotification = escalationConfig.alertChannel != com.example.integrations.EscalationAlertChannel.EMAIL
        repository.escalateCallNow(
          context = context,
          call = savedCall,
          reason = savedCall.escalationNotes,
          targetEmail = escalationConfig.targetEmail,
          sendNotification = sendNotification,
          webhookUrl = escalationConfig.webhookUrl
        )
      }

      return savedCall
    }
  }
}
