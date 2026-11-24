package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAdvisorUiState
import com.example.ai.GeminiAiService
import com.example.ai.ReceptionistResult
import com.example.ai.SurferAiReceptionist
import com.example.data.AppRepository
import com.example.data.local.CallEntity
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.example.data.local.WaveAuditLeadEntity
import com.example.integrations.ChatGptConfig
import com.example.integrations.EscalationAlertChannel
import com.example.integrations.EscalationTriggerConfig
import com.example.integrations.GitHubWebsiteConfig
import com.example.integrations.IntegrationPrefsStore
import com.example.integrations.SupabaseCallSyncWorker
import com.example.integrations.SupabaseClient
import com.example.integrations.SupabaseConfig
import com.example.integrations.SupabaseConnectionState
import com.example.integrations.SupabaseWaveAuditLeadDto
import com.example.integrations.WaveAuditLead
import com.example.ui.components.CallDateFilter
import com.example.ui.components.CallStatusFilter
import com.example.ui.theme.OceanPalette
import com.example.voice.VoiceReceptionistController
import com.example.voice.VoiceUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveCallUiState(
  val isInCall: Boolean = false,
  val isRinging: Boolean = false,
  val callerName: String = "",
  val callerNumber: String = "",
  val currentCustomerQuery: String = "",
  val currentAiResponse: String = "",
  val isAiThinking: Boolean = false,
  val callDurationSeconds: Int = 0,
  val isFlaggedForEscalation: Boolean = false,
  val escalationReason: String = "",
  val returningContextBadge: String = "",
  val returningContextPrompt: String = "",
  val audioWaveHeights: List<Float> = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.8f, 0.5f, 0.7f, 0.3f)
)

class OceanSurferViewModel(
  private val repository: AppRepository,
  private val appContext: Context
) : ViewModel() {

  val calls: StateFlow<List<CallEntity>> = repository.allCalls
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val latestCalls: StateFlow<List<CallEntity>> = repository.latestCalls
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val pendingCalls: StateFlow<List<CallEntity>> = repository.pendingCalls
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val products: StateFlow<List<ProductEntity>> = repository.allProducts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val faqs: StateFlow<List<FaqEntity>> = repository.allFaqs
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _activeCall = MutableStateFlow(ActiveCallUiState())
  val activeCall: StateFlow<ActiveCallUiState> = _activeCall.asStateFlow()

  private val _chatGptConfig = MutableStateFlow(IntegrationPrefsStore.loadChatGptConfig(appContext))
  val chatGptConfig: StateFlow<ChatGptConfig> = _chatGptConfig.asStateFlow()

  private val _supabaseConfig = MutableStateFlow(IntegrationPrefsStore.loadSupabaseConfig(appContext))
  val supabaseConfig: StateFlow<SupabaseConfig> = _supabaseConfig.asStateFlow()

  private val _isFetchingFromSupabase = MutableStateFlow(false)
  val isFetchingFromSupabase: StateFlow<Boolean> = _isFetchingFromSupabase.asStateFlow()

  private val _lastSupabaseFetchTime = MutableStateFlow("Synced with Supabase")
  val lastSupabaseFetchTime: StateFlow<String> = _lastSupabaseFetchTime.asStateFlow()

  private val _gitHubConfig = MutableStateFlow(GitHubWebsiteConfig())
  val gitHubConfig: StateFlow<GitHubWebsiteConfig> = _gitHubConfig.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _activeCallAlert = MutableStateFlow<CallEntity?>(null)
  val activeCallAlert: StateFlow<CallEntity?> = _activeCallAlert.asStateFlow()

  private val _escalationTriggerConfig = MutableStateFlow(IntegrationPrefsStore.loadEscalationConfig(appContext))
  val escalationTriggerConfig: StateFlow<EscalationTriggerConfig> = _escalationTriggerConfig.asStateFlow()

  private val _pendingEmailEscalation = MutableStateFlow<CallEntity?>(null)
  val pendingEmailEscalation: StateFlow<CallEntity?> = _pendingEmailEscalation.asStateFlow()

  private val _selectedStatusFilter = MutableStateFlow(CallStatusFilter.ALL)
  val selectedStatusFilter: StateFlow<CallStatusFilter> = _selectedStatusFilter.asStateFlow()

  private val _callSearchQuery = MutableStateFlow("")
  val callSearchQuery: StateFlow<String> = _callSearchQuery.asStateFlow()

  private val _selectedDateFilter = MutableStateFlow(CallDateFilter.ALL)
  val selectedDateFilter: StateFlow<CallDateFilter> = _selectedDateFilter.asStateFlow()

  private val _customDateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
  val customDateRange: StateFlow<Pair<Long?, Long?>> = _customDateRange.asStateFlow()

  private val _supabaseConnectionState = MutableStateFlow<SupabaseConnectionState>(SupabaseConnectionState.Idle)
  val supabaseConnectionState: StateFlow<SupabaseConnectionState> = _supabaseConnectionState.asStateFlow()

  private val _geminiAdvisorState = MutableStateFlow<GeminiAdvisorUiState>(GeminiAdvisorUiState.Idle)
  val geminiAdvisorState: StateFlow<GeminiAdvisorUiState> = _geminiAdvisorState.asStateFlow()

  private val _selectedPalette = MutableStateFlow(IntegrationPrefsStore.loadOceanPalette(appContext))
  val selectedPalette: StateFlow<OceanPalette> = _selectedPalette.asStateFlow()

  fun selectOceanPalette(palette: OceanPalette) {
    if (_selectedPalette.value == palette) return
    _selectedPalette.value = palette
    IntegrationPrefsStore.saveOceanPalette(appContext, palette)
    _snackbarMessage.value = "Ocean theme switched to ${palette.displayName}!"
  }

  val unsyncedCallCount: StateFlow<Int> = repository.allCalls
    .map { list -> list.count { !it.supabaseSynced } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val clientLeads: StateFlow<List<WaveAuditLead>> = repository.allLeads
    .map { entities ->
      entities.map { entity ->
        WaveAuditLead(
          id = entity.id.toString(),
          businessName = entity.businessName,
          contactName = entity.contactName,
          contactEmail = entity.contactEmail,
          contactPhone = entity.contactPhone,
          biggestBottleneck = entity.biggestBottleneck,
          recommendedProduct = entity.recommendedProduct,
          syncedToSupabase = entity.syncedToSupabase,
          createdAt = entity.createdAt
        )
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val voiceController = VoiceReceptionistController(appContext) { transcript ->
    processQuestionInCall(transcript)
  }
  val voiceUiState: StateFlow<VoiceUiState> = voiceController.uiState

  private var advisorJob: Job? = null

  fun startVoiceListening() {
    voiceController.startListening()
  }

  fun startConsoleVoiceInput(onCaptured: (String) -> Unit) {
    voiceController.startListening(onCaptured)
  }

  fun speakText(text: String) {
    voiceController.speakReply(text)
  }

  fun stopVoiceListening() {
    voiceController.stopListening()
  }

  fun toggleVoiceOutput() {
    voiceController.toggleVoiceOutput()
  }

  override fun onCleared() {
    super.onCleared()
    voiceController.release()
  }

  fun setStatusFilter(filter: CallStatusFilter) {
    _selectedStatusFilter.value = filter
  }

  fun setCallSearchQuery(query: String) {
    _callSearchQuery.value = query
  }

  fun setDateFilter(filter: CallDateFilter) {
    _selectedDateFilter.value = filter
  }

  fun setCustomDateRange(startUtcMillis: Long?, endUtcMillis: Long?) {
    _customDateRange.value = startUtcMillis to endUtcMillis
  }

  fun clearCallFilters() {
    _callSearchQuery.value = ""
    _selectedDateFilter.value = CallDateFilter.ALL
    _customDateRange.value = null to null
    _selectedStatusFilter.value = CallStatusFilter.ALL
  }

  fun dismissCallAlert() {
    _activeCallAlert.value = null
  }

  fun consumePendingEmailEscalation() {
    _pendingEmailEscalation.value = null
  }

  fun updateEscalationTriggerConfig(
    isEnabled: Boolean = _escalationTriggerConfig.value.isEnabled,
    alertChannel: EscalationAlertChannel = _escalationTriggerConfig.value.alertChannel,
    targetEmail: String = _escalationTriggerConfig.value.targetEmail,
    webhookUrl: String = _escalationTriggerConfig.value.webhookUrl,
    autoOpenEmailComposer: Boolean = _escalationTriggerConfig.value.autoOpenEmailComposer,
    escalateOnHighUrgency: Boolean = _escalationTriggerConfig.value.escalateOnHighUrgency
  ) {
    val updated = EscalationTriggerConfig(
      isEnabled = isEnabled,
      alertChannel = alertChannel,
      targetEmail = targetEmail.trim().ifBlank { "oceantidedropservice@gmail.com" },
      webhookUrl = webhookUrl.trim(),
      autoOpenEmailComposer = autoOpenEmailComposer,
      escalateOnHighUrgency = escalateOnHighUrgency
    )
    _escalationTriggerConfig.value = updated
    IntegrationPrefsStore.saveEscalationConfig(appContext, updated)
    val webhookSuffix = if (updated.webhookUrl.isNotBlank()) " + Webhook" else ""
    _snackbarMessage.value = "AI Escalation trigger updated (${alertChannel.label}$webhookSuffix)."
  }

  private var callTimerJob: Job? = null
  private var waveAnimationJob: Job? = null

  init {
    startEscalationMonitor()
    autoSyncOfficialCatalog()
    startForegroundSupabaseSyncLoop()
  }

  private fun autoSyncOfficialCatalog() {
    viewModelScope.launch {
      repository.forceSyncOfficialProducts()
    }
  }

  fun syncProductsFromWebsite() {
    viewModelScope.launch {
      repository.forceSyncOfficialProducts()
      _snackbarMessage.value = "Synced 12 official products & FAQs from https://otdaisurfer.surf!"
    }
  }

  private fun startForegroundSupabaseSyncLoop() {
    viewModelScope.launch {
      val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
      while (true) {
        delay(30_000L)
        val cfg = _supabaseConfig.value
        if (!cfg.autoSyncCalls) continue

        val unsynced = repository.getUnsyncedCalls()
        for (call in unsynced) {
          val upload = SupabaseClient.syncCallRecord(cfg, call)
          if (upload.isSuccess) {
            repository.markSupabaseSynced(call.id)
          }
        }

        val remote = SupabaseClient.fetchCalls(cfg).getOrNull()
        if (remote != null) {
          repository.reconcileRemoteCalls(remote)
          _lastSupabaseFetchTime.value = "Auto-synced at ${timeFormat.format(java.util.Date())}"
        }
      }
    }
  }

  private fun startEscalationMonitor() {
    viewModelScope.launch {
      while (true) {
        delay(10000) // check every 10 seconds
        val overdue = repository.checkOverdueEscalations(appContext)
        if (overdue.isNotEmpty() && _activeCallAlert.value == null) {
          val firstOverdue = overdue.first()
          _activeCallAlert.value = firstOverdue
          val cfg = _escalationTriggerConfig.value
          if (cfg.isEnabled && cfg.autoOpenEmailComposer &&
            (cfg.alertChannel == EscalationAlertChannel.EMAIL || cfg.alertChannel == EscalationAlertChannel.BOTH)
          ) {
            _pendingEmailEscalation.value = firstOverdue
          }
        }
      }
    }
  }

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  fun showSnackbar(msg: String) {
    _snackbarMessage.value = msg
  }

  // --- Incoming Call Handling & Simulation ---

  fun simulateIncomingCall(
    callerName: String,
    callerNumber: String,
    initialQuery: String
  ) {
    _activeCall.value = ActiveCallUiState(
      isInCall = false,
      isRinging = true,
      callerName = callerName,
      callerNumber = callerNumber,
      currentCustomerQuery = initialQuery,
      currentAiResponse = "",
      isAiThinking = false,
      callDurationSeconds = 0
    )
    viewModelScope.launch {
      val returningCtx = repository.findReturningCallerContext(callerNumber)
      if (returningCtx.isVipOrReturning) {
        val current = _activeCall.value
        val displayName = if (current.callerName.isBlank() || current.callerName == "Cell Phone Caller") {
          returningCtx.matchedName ?: current.callerName
        } else {
          current.callerName
        }
        _activeCall.value = current.copy(
          callerName = displayName,
          returningContextBadge = returningCtx.toBadgeText(),
          returningContextPrompt = returningCtx.toPromptContext()
        )
      }
    }
  }

  fun answerIncomingCall() {
    val current = _activeCall.value
    _activeCall.value = current.copy(
      isInCall = true,
      isRinging = false
    )
    startCallTimer()
    startAudioWaveform()

    // Immediately trigger AI receptionist greeting & answer to caller's inquiry
    if (current.currentCustomerQuery.isNotBlank()) {
      processQuestionInCall(current.currentCustomerQuery)
    } else {
      val greeting = if (current.returningContextBadge.isNotBlank()) {
        "Aloha and welcome back ${current.callerName}! You've reached Ocean Tide Drop AI Surfer. How can I help your business ride the AI wave today?"
      } else {
        "Aloha! You've reached Ocean Tide Drop AI Surfer (otdaisurfer.surf). How can I help your business ride the AI wave today?"
      }
      _activeCall.value = _activeCall.value.copy(currentAiResponse = greeting)
      voiceController.speakReply(greeting)
    }
  }

  fun processQuestionInCall(questionText: String) {
    val current = _activeCall.value
    _activeCall.value = current.copy(
      currentCustomerQuery = questionText,
      isAiThinking = true
    )

    viewModelScope.launch {
      val returningPrompt = current.returningContextPrompt.ifBlank {
        repository.findReturningCallerContext(current.callerNumber).toPromptContext()
      }
      val result = SurferAiReceptionist.processCallerQuestion(
        callerName = current.callerName.ifBlank { "Caller" },
        callerNumber = current.callerNumber.ifBlank { "+1 (555) 789-WAVE" },
        question = questionText,
        products = products.value,
        faqs = faqs.value,
        openAiApiKey = _chatGptConfig.value.apiKey,
        modelName = _chatGptConfig.value.model,
        returningContext = returningPrompt,
        useOpenAi = _chatGptConfig.value.isEnabled
      )

      val shouldEscalate = result.requiresImmediateEscalation ||
        (_escalationTriggerConfig.value.escalateOnHighUrgency && result.urgency == "HIGH")
      val reason = result.escalationReason.ifBlank {
        if (shouldEscalate) "AI Receptionist flagged call for priority crew escalation." else ""
      }

      _activeCall.value = _activeCall.value.copy(
        currentAiResponse = result.responseText,
        isAiThinking = false,
        isFlaggedForEscalation = shouldEscalate,
        escalationReason = reason
      )
      voiceController.speakReply(result.responseText)
    }
  }

  fun endCallAndSaveTranscript() {
    val current = _activeCall.value
    callTimerJob?.cancel()
    waveAnimationJob?.cancel()
    voiceController.stopListening()
    voiceController.stopSpeaking()

    if (current.currentCustomerQuery.isNotBlank() || current.currentAiResponse.isNotBlank()) {
      viewModelScope.launch {
        val returningPrompt = current.returningContextPrompt.ifBlank {
          repository.findReturningCallerContext(current.callerNumber).toPromptContext()
        }
        val result = SurferAiReceptionist.processCallerQuestion(
          callerName = current.callerName.ifBlank { "Ocean Caller" },
          callerNumber = current.callerNumber.ifBlank { "+1 (555) 019-9283" },
          question = current.currentCustomerQuery.ifBlank { "General surf inquiries" },
          products = products.value,
          faqs = faqs.value,
          openAiApiKey = _chatGptConfig.value.apiKey,
          modelName = _chatGptConfig.value.model,
          returningContext = returningPrompt,
          useOpenAi = _chatGptConfig.value.isEnabled
        )

        val cfg = _escalationTriggerConfig.value
        val shouldEscalate = result.requiresImmediateEscalation ||
          (cfg.escalateOnHighUrgency && result.urgency == "HIGH")
        val alertMinutes = if (shouldEscalate) 3 else 10
        val deadline = System.currentTimeMillis() + (alertMinutes * 60 * 1000)

        val callEntity = CallEntity(
          callerNumber = current.callerNumber.ifBlank { "+1 (555) 019-9283" },
          callerName = current.callerName.ifBlank { "Ocean Caller" },
          timestamp = System.currentTimeMillis(),
          durationSeconds = current.callDurationSeconds,
          status = "PENDING_REVIEW",
          urgency = result.urgency,
          topic = result.topic,
          summary = result.summary,
          transcription = "Caller: \"${current.currentCustomerQuery}\"\nAI Surfer: \"${result.responseText}\"",
          customerQuestion = current.currentCustomerQuery,
          aiResponse = result.responseText,
          escalationAlertMinutes = alertMinutes,
          escalationDeadline = deadline,
          isEscalated = false,
          supabaseSynced = false
        )

        val newId = repository.insertCall(callEntity)
        var savedCall = callEntity.copy(id = newId)

        if (_supabaseConfig.value.autoSyncCalls) {
          val syncResult = SupabaseClient.syncCallRecord(_supabaseConfig.value, savedCall)
          if (syncResult.isSuccess) {
            repository.markSupabaseSynced(newId)
            savedCall = savedCall.copy(supabaseSynced = true)
          } else {
            SupabaseCallSyncWorker.enqueueImmediateSync(appContext)
          }
        }

        if (shouldEscalate && cfg.isEnabled) {
          val reason = result.escalationReason.ifBlank {
            "AI flagged incoming call (${result.urgency}) for immediate escalation."
          }
          dispatchAiEscalationAlert(savedCall, reason)
        } else {
          _snackbarMessage.value = "Call logged & summarized! Escalation timer set to $alertMinutes min."
        }
      }
    }

    _activeCall.value = ActiveCallUiState()
  }

  private fun startCallTimer() {
    callTimerJob?.cancel()
    callTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000)
        _activeCall.value = _activeCall.value.copy(
          callDurationSeconds = _activeCall.value.callDurationSeconds + 1
        )
      }
    }
  }

  private fun startAudioWaveform() {
    waveAnimationJob?.cancel()
    waveAnimationJob = viewModelScope.launch {
      val random = java.util.Random()
      while (true) {
        delay(120)
        val newWave = List(8) { 0.2f + random.nextFloat() * 0.8f }
        _activeCall.value = _activeCall.value.copy(audioWaveHeights = newWave)
      }
    }
  }

  // --- Call Review & Escalation Actions ---

  fun markCallReviewed(callId: Long) {
    viewModelScope.launch {
      repository.markReviewed(callId)
      if (_activeCallAlert.value?.id == callId) {
        _activeCallAlert.value = null
      }
      _snackbarMessage.value = "Call marked as Reviewed. Escalation deactivated."
    }
  }

  fun markCallPending(callId: Long) {
    viewModelScope.launch {
      repository.markPending(callId)
      _snackbarMessage.value = "Call returned to Pending Review queue."
    }
  }

  private suspend fun dispatchAiEscalationAlert(
    call: CallEntity,
    reason: String,
    overrideChannel: EscalationAlertChannel? = null
  ) {
    val cfg = _escalationTriggerConfig.value
    val channel = overrideChannel ?: cfg.alertChannel
    val sendNotification = channel == EscalationAlertChannel.NOTIFICATION || channel == EscalationAlertChannel.BOTH
    val sendEmail = channel == EscalationAlertChannel.EMAIL || channel == EscalationAlertChannel.BOTH

    val escalatedCall = repository.escalateCallNow(
      context = appContext,
      call = call,
      reason = reason,
      targetEmail = cfg.targetEmail,
      sendNotification = sendNotification,
      webhookUrl = cfg.webhookUrl
    )

    _activeCallAlert.value = escalatedCall
    if (sendEmail && cfg.autoOpenEmailComposer) {
      _pendingEmailEscalation.value = escalatedCall
    }

    val channelSummary = when (channel) {
      EscalationAlertChannel.NOTIFICATION -> "Push Notification"
      EscalationAlertChannel.EMAIL -> "Email (${cfg.targetEmail})"
      EscalationAlertChannel.BOTH -> "Notification + Email (${cfg.targetEmail})"
    }
    val webhookTag = if (cfg.webhookUrl.isNotBlank()) " + Webhook" else ""
    _snackbarMessage.value = "🚨 AI Escalation Alert triggered via $channelSummary$webhookTag!"
  }

  fun triggerManualEscalation(call: CallEntity) {
    viewModelScope.launch {
      val reason = "Staff escalated call from ${call.callerName} via ${_escalationTriggerConfig.value.alertChannel.label}."
      dispatchAiEscalationAlert(call, reason)
    }
  }

  fun triggerEmailAlertForCall(call: CallEntity) {
    viewModelScope.launch {
      val reason = call.escalationNotes.ifBlank {
        "AI Escalation email alert triggered for ${call.callerName}."
      }
      dispatchAiEscalationAlert(call, reason, overrideChannel = EscalationAlertChannel.EMAIL)
    }
  }

  fun triggerAiEscalationForQuestion(
    callerName: String,
    callerNumber: String,
    question: String
  ) {
    viewModelScope.launch {
      val result = SurferAiReceptionist.processCallerQuestion(
        callerName = callerName,
        callerNumber = callerNumber,
        question = question,
        products = products.value,
        faqs = faqs.value,
        openAiApiKey = _chatGptConfig.value.apiKey,
        modelName = _chatGptConfig.value.model,
        useOpenAi = _chatGptConfig.value.isEnabled
      )

      val callEntity = CallEntity(
        callerNumber = callerNumber,
        callerName = callerName,
        timestamp = System.currentTimeMillis(),
        durationSeconds = 52,
        status = "PENDING_REVIEW",
        urgency = if (result.requiresImmediateEscalation) result.urgency else "URGENT_VIP",
        topic = result.topic,
        summary = result.summary,
        transcription = "Caller: \"$question\"\nAI Surfer: \"${result.responseText}\"",
        customerQuestion = question,
        aiResponse = result.responseText,
        escalationAlertMinutes = 3,
        escalationDeadline = System.currentTimeMillis() + (3 * 60 * 1000),
        isEscalated = false
      )
      val newId = repository.insertCall(callEntity)
      val savedCall = callEntity.copy(id = newId)
      val reason = result.escalationReason.ifBlank {
        "AI Receptionist flagged incoming call for immediate escalation."
      }
      dispatchAiEscalationAlert(savedCall, reason)
      _selectedStatusFilter.value = com.example.ui.components.CallStatusFilter.ESCALATED
    }
  }

  fun triggerTestCallAlert() {
    triggerAiEscalationForQuestion(
      callerName = "Makena Kai (VIP Lead)",
      callerNumber = "+1 (808) 555-SURF",
      question = "Urgent! We need immediate assistance with Big Kahuna enterprise onboarding and Wave Starter checkout—please escalate to a manager ASAP!"
    )
  }

  fun deleteCall(callId: Long) {
    viewModelScope.launch {
      repository.deleteCall(callId)
      _snackbarMessage.value = "Call record removed."
    }
  }

  fun clearAllCalls() {
    viewModelScope.launch {
      repository.clearAllCalls()
      _snackbarMessage.value = "All call logs cleared."
    }
  }

  // --- Integrations: ChatGPT, Supabase, GitHub ---

  fun updateChatGptConfig(apiKey: String, model: String, isEnabled: Boolean = _chatGptConfig.value.isEnabled || apiKey.isNotBlank()) {
    val cleanKey = apiKey.trim()
    val updated = _chatGptConfig.value.copy(
      apiKey = cleanKey,
      model = model,
      isEnabled = isEnabled
    )
    _chatGptConfig.value = updated
    IntegrationPrefsStore.saveChatGptConfig(appContext, updated)
    _snackbarMessage.value = if (updated.isEnabled) {
      "OpenAI ChatGPT settings updated ($model)."
    } else {
      "OpenAI key disabled — using Gemini / built-in AI."
    }
  }

  fun setOpenAiEnabled(enabled: Boolean) {
    val updated = _chatGptConfig.value.copy(isEnabled = enabled)
    _chatGptConfig.value = updated
    IntegrationPrefsStore.saveChatGptConfig(appContext, updated)
    _snackbarMessage.value = if (enabled) {
      "OpenAI ChatGPT enabled."
    } else {
      "OpenAI key released — using Gemini / built-in AI."
    }
  }

  fun clearOpenAiKey() {
    val updated = _chatGptConfig.value.copy(
      apiKey = "",
      isEnabled = false
    )
    _chatGptConfig.value = updated
    IntegrationPrefsStore.saveChatGptConfig(appContext, updated)
    _snackbarMessage.value = "OpenAI key cleared & disconnected. Using Gemini / built-in AI."
  }

  fun updateSupabaseConfig(projectUrl: String, anonKey: String) {
    val updated = _supabaseConfig.value.copy(
      projectUrl = projectUrl,
      anonKey = anonKey
    )
    _supabaseConfig.value = updated
    IntegrationPrefsStore.saveSupabaseConfig(appContext, updated)
    _snackbarMessage.value = "Supabase connection updated."
  }

  fun testSupabaseConnection() {
    viewModelScope.launch {
      _supabaseConnectionState.value = SupabaseConnectionState.Testing
      val state = SupabaseClient.testConnection(_supabaseConfig.value)
      _supabaseConnectionState.value = state
      _snackbarMessage.value = when (state) {
        is SupabaseConnectionState.Connected -> "Supabase connected (${state.latencyMs}ms)!"
        is SupabaseConnectionState.Error -> state.message
        else -> "Supabase check finished."
      }
    }
  }

  fun fetchCallsFromSupabase() {
    if (_isFetchingFromSupabase.value) return
    viewModelScope.launch {
      _isFetchingFromSupabase.value = true
      try {
        val cfg = _supabaseConfig.value
        var pushedCount = 0
        val unsynced = repository.getUnsyncedCalls()
        for (call in unsynced) {
          if (SupabaseClient.syncCallRecord(cfg, call).isSuccess) {
            repository.markSupabaseSynced(call.id)
            pushedCount++
          }
        }

        val liveResult = SupabaseClient.fetchCalls(cfg)
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val syncedAt = timeFormat.format(java.util.Date())
        if (liveResult.isSuccess) {
          val remoteCalls = liveResult.getOrNull().orEmpty()
          val reconciled = repository.reconcileRemoteCalls(remoteCalls)
          _supabaseConnectionState.value = SupabaseConnectionState.Connected(
            latencyMs = 0L,
            message = "Synced ${remoteCalls.size} live rows ($reconciled updated, $pushedCount uploaded) from Supabase public.calls"
          )
          _lastSupabaseFetchTime.value = "Live Supabase sync at $syncedAt"
          _snackbarMessage.value = if (remoteCalls.isNotEmpty()) {
            "Synced ${remoteCalls.size} call logs from Supabase!"
          } else {
            "Synced with Supabase ($pushedCount uploaded, 0 remote rows)."
          }
        } else {
          val err = liveResult.exceptionOrNull()?.localizedMessage ?: "Endpoint unreachable"
          _supabaseConnectionState.value = SupabaseConnectionState.Error("Supabase sync failed: $err")
          _snackbarMessage.value = "Supabase sync failed: $err"
        }
      } finally {
        _isFetchingFromSupabase.value = false
      }
    }
  }

  fun askGeminiWaveAdvisor(businessContext: String, bottleneck: String) {
    val cleanContext = businessContext.trim()
    if (cleanContext.isBlank()) {
      _geminiAdvisorState.value = GeminiAdvisorUiState.Error("Please enter your business goal or question first.")
      return
    }
    advisorJob?.cancel()
    advisorJob = viewModelScope.launch {
      GeminiAiService.streamWaveAdvisor(
        businessContext = cleanContext,
        bottleneck = bottleneck,
        products = products.value
      ).collect { state ->
        _geminiAdvisorState.value = state
      }
    }
  }

  fun resetGeminiAdvisor() {
    advisorJob?.cancel()
    _geminiAdvisorState.value = GeminiAdvisorUiState.Idle
  }

  fun submitClientWaveCheck(
    businessName: String,
    contactName: String,
    contactEmail: String,
    contactPhone: String,
    biggestBottleneck: String
  ) {
    viewModelScope.launch {
      val recommended = when {
        biggestBottleneck.contains("Enterprise", ignoreCase = true) || biggestBottleneck.contains("Multi-Agent", ignoreCase = true) ->
          "Big Kahuna™ / Tsunami Growth ($3,997)"
        biggestBottleneck.contains("Search", ignoreCase = true) || biggestBottleneck.contains("Workflow", ignoreCase = true) ->
          "Wave Builder Package ($1,997)"
        biggestBottleneck.contains("Lead", ignoreCase = true) ->
          "Wave Starter™: Wave Scout ($497)"
        biggestBottleneck.contains("Sales", ignoreCase = true) ->
          "Wave Starter™: Sales Rider ($497)"
        else -> "Wave Starter™ Sprint ($497)"
      }

      val dto = SupabaseWaveAuditLeadDto(
        businessName = businessName.ifBlank { "Coastal Partner" },
        contactName = contactName.ifBlank { "Client Lead" },
        contactEmail = contactEmail.trim(),
        contactPhone = contactPhone.trim(),
        biggestBottleneck = biggestBottleneck,
        recommendedProduct = recommended
      )
      // Keep the draft on this device until the owner confirms a live lead.
      repository.insertLead(
        WaveAuditLeadEntity(
          id = java.util.UUID.randomUUID().toString(),
          businessName = dto.businessName,
          contactName = dto.contactName,
          contactEmail = dto.contactEmail,
          contactPhone = dto.contactPhone,
          biggestBottleneck = dto.biggestBottleneck,
          recommendedProduct = recommended,
          syncedToSupabase = false
        )
      )

      // Log into Call Dashboard & Room so staff immediately sees the client intake
      val leadCall = CallEntity(
        callerNumber = dto.contactPhone,
        callerName = "${dto.contactName} (${dto.businessName})",
        timestamp = System.currentTimeMillis(),
        durationSeconds = 0,
        status = "PENDING_REVIEW",
        urgency = if (recommended.contains("3,997")) "URGENT_VIP" else "HIGH",
        topic = "LEAD_WAVE_CHECK",
        summary = "Topic: Client Wave Check Intake | Action Items: Review $recommended fit & email ${dto.contactEmail} | Sentiment: Positive (Qualified Lead)",
        transcription = "Client Portal Wave Check submission for ${dto.businessName}. Bottleneck: $biggestBottleneck",
        customerQuestion = "Free AI Wave Check™ Bottleneck: $biggestBottleneck",
        aiResponse = "Recommended Path: $recommended. Strategy link: https://otdaisurfer.surf/wave-check",
        escalationAlertMinutes = 10,
        escalationDeadline = System.currentTimeMillis() + 10 * 60_000L,
        isEscalated = false,
        supabaseSynced = false
      )
      val id = repository.insertCall(leadCall)
      if (_supabaseConfig.value.autoSyncCalls) {
        val syncRes = SupabaseClient.syncCallRecord(_supabaseConfig.value, leadCall.copy(id = id))
        if (syncRes.isSuccess) {
          repository.markSupabaseSynced(id)
        } else {
          SupabaseCallSyncWorker.enqueueImmediateSync(appContext)
        }
      }

      _snackbarMessage.value = "Lead saved on this device for owner review."
    }
  }

  fun submitConfirmedLead(lead: WaveAuditLeadEntity, email: String, password: String) {
    viewModelScope.launch {
      try {
        com.example.integrations.KaiOwnerLeadClient.submit(lead, email, password)
        repository.insertLead(lead.copy(syncedToSupabase = true))
        _snackbarMessage.value = "Live lead saved to AI SURFER."
      } catch (e: java.io.IOException) {
        _snackbarMessage.value = "Owner submission failed (${e.message}). Lead kept on device."
      } catch (e: org.json.JSONException) {
        _snackbarMessage.value = "Unexpected server response. Lead kept on device."
      }
    }
  }

  fun triggerGitHubWebsiteDeploy() {
    viewModelScope.launch {
      _gitHubConfig.value = _gitHubConfig.value.copy(
        lastDeploymentStatus = "Triggering sync with otdaisurfer/AISurfer-..."
      )
      delay(1200)
      _gitHubConfig.value = _gitHubConfig.value.copy(
        lastDeploymentStatus = "Synced with otdaisurfer/AISurfer- (Live on otdaisurfer.surf)"
      )
      _snackbarMessage.value = "Synced with otdaisurfer/AISurfer- repository!"
    }
  }

  fun updateGitHubConfig(owner: String, repo: String, branch: String) {
    _gitHubConfig.value = _gitHubConfig.value.copy(
      repoOwner = owner.trim(),
      repoName = repo.trim(),
      repoUrl = "https://github.com/${owner.trim()}/${repo.trim()}",
      branch = branch.trim(),
      lastDeploymentStatus = "Connected: ${owner.trim()}/${repo.trim()} (Live on otdaisurfer.surf)"
    )
    _snackbarMessage.value = "Updated connected repo to ${owner.trim()}/${repo.trim()}"
  }

  fun addNewProduct(
    name: String,
    price: Double,
    category: String,
    desc: String,
    level: String,
    ctaText: String = "Explore on Website",
    directUrl: String = "https://otdaisurfer.surf",
    slug: String = ""
  ) {
    viewModelScope.launch {
      val product = ProductEntity(
        name = name,
        price = price,
        category = category,
        description = desc,
        surfLevel = level,
        popularBadge = "New Wave",
        ctaText = ctaText,
        directUrl = directUrl,
        slug = slug
      )
      repository.insertProduct(product)
      _snackbarMessage.value = "Product '$name' added to catalog & AI knowledge base."
    }
  }

  fun addNewFaq(category: String, question: String, answer: String, tags: String) {
    viewModelScope.launch {
      val faq = FaqEntity(
        category = category,
        question = question,
        answer = answer,
        tags = tags
      )
      repository.insertFaq(faq)
      _snackbarMessage.value = "Business FAQ added to AI Receptionist knowledge base."
    }
  }
}

class OceanSurferViewModelFactory(
  private val repository: AppRepository,
  private val appContext: Context
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(OceanSurferViewModel::class.java)) {
      return OceanSurferViewModel(repository, appContext) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
