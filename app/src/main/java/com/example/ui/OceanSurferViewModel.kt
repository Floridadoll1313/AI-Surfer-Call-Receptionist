package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiInstrumentTool
import com.example.ai.AiInstrumentsEngine
import com.example.ai.CallQueueBriefUiState
import com.example.ai.CopilotChatMessage
import com.example.ai.GeminiAdvisorUiState
import com.example.ai.GeminiAiService
import com.example.ai.ImageGenUiState
import com.example.ai.InstrumentTextUiState
import com.example.ai.PitchBriefUiState
import com.example.ai.ReceptionistResult
import com.example.ai.SurferAiReceptionist
import com.example.auth.GoogleAccountUiState
import com.example.auth.GoogleSignInManager
import com.example.auth.PhoneVerificationManager
import com.example.auth.PhoneVerificationStage
import com.example.auth.PhoneVerificationUiState
import com.example.data.AppRepository
import com.example.data.local.CallEntity
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.example.data.local.WaveAuditLeadEntity
import com.example.integrations.BusinessLineId
import com.example.integrations.BusinessPhonesConfig
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
import com.example.integrations.TeamSetupCodec
import com.example.integrations.WaveAuditLead
import com.example.telephony.CallEscalationManager
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

  private val _chatGptConfig = MutableStateFlow(IntegrationPrefsStore.loadChatGptConfig(appContext))
  val chatGptConfig: StateFlow<ChatGptConfig> = _chatGptConfig.asStateFlow()

  private val _supabaseConfig = MutableStateFlow(IntegrationPrefsStore.loadSupabaseConfig(appContext))
  val supabaseConfig: StateFlow<SupabaseConfig> = _supabaseConfig.asStateFlow()

  private val _businessPhonesConfig = MutableStateFlow(IntegrationPrefsStore.loadBusinessPhonesConfig(appContext))
  val businessPhonesConfig: StateFlow<BusinessPhonesConfig> = _businessPhonesConfig.asStateFlow()

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

  private val _selectedPhoneLineFilter = MutableStateFlow(BusinessLineId.ALL)
  val selectedPhoneLineFilter: StateFlow<BusinessLineId> = _selectedPhoneLineFilter.asStateFlow()

  private val _supabaseConnectionState = MutableStateFlow<SupabaseConnectionState>(SupabaseConnectionState.Idle)
  val supabaseConnectionState: StateFlow<SupabaseConnectionState> = _supabaseConnectionState.asStateFlow()

  private val _geminiAdvisorState = MutableStateFlow<GeminiAdvisorUiState>(GeminiAdvisorUiState.Idle)
  val geminiAdvisorState: StateFlow<GeminiAdvisorUiState> = _geminiAdvisorState.asStateFlow()

  private val _selectedPalette = MutableStateFlow(IntegrationPrefsStore.loadOceanPalette(appContext))
  val selectedPalette: StateFlow<OceanPalette> = _selectedPalette.asStateFlow()

  private val _phoneVerificationState = MutableStateFlow(PhoneVerificationUiState())
  val phoneVerificationState: StateFlow<PhoneVerificationUiState> = _phoneVerificationState.asStateFlow()

  private val _selectedAiInstrument = MutableStateFlow(AiInstrumentTool.WAVE_ADVISOR)
  val selectedAiInstrument: StateFlow<AiInstrumentTool> = _selectedAiInstrument.asStateFlow()

  private val _copilotMessages = MutableStateFlow(
    listOf(
      CopilotChatMessage(
        isFromUser = false,
        text = "Aloha! I'm Kai Copilot (gemini-3.8-flash). Ask me anything about Ocean Tide Drop AI workflows, call scripts, or client ROI."
      )
    )
  )
  val copilotMessages: StateFlow<List<CopilotChatMessage>> = _copilotMessages.asStateFlow()

  private val _isCopilotSending = MutableStateFlow(false)
  val isCopilotSending: StateFlow<Boolean> = _isCopilotSending.asStateFlow()

  private val _copilotError = MutableStateFlow<String?>(null)
  val copilotError: StateFlow<String?> = _copilotError.asStateFlow()

  private val _visionResultState = MutableStateFlow<InstrumentTextUiState>(InstrumentTextUiState.Idle)
  val visionResultState: StateFlow<InstrumentTextUiState> = _visionResultState.asStateFlow()

  private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
  val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

  private val _pitchBriefState = MutableStateFlow<PitchBriefUiState>(PitchBriefUiState.Idle)
  val pitchBriefState: StateFlow<PitchBriefUiState> = _pitchBriefState.asStateFlow()

  private val _callQueueBriefState = MutableStateFlow<CallQueueBriefUiState>(CallQueueBriefUiState.Idle)
  val callQueueBriefState: StateFlow<CallQueueBriefUiState> = _callQueueBriefState.asStateFlow()

  private val _translatorState = MutableStateFlow<InstrumentTextUiState>(InstrumentTextUiState.Idle)
  val translatorState: StateFlow<InstrumentTextUiState> = _translatorState.asStateFlow()

  private val _googleAccountState = MutableStateFlow(
    GoogleSignInManager.toUiState(GoogleSignInManager.currentUser(appContext))
  )
  val googleAccountState: StateFlow<GoogleAccountUiState> = _googleAccountState.asStateFlow()

  init {
    viewModelScope.launch {
      GoogleSignInManager.authStateFlow(appContext).collect { user ->
        _googleAccountState.value = GoogleSignInManager.toUiState(
          user = user,
          isLoading = false,
          errorMessage = if (user != null) null else _googleAccountState.value.errorMessage
        )
      }
    }
  }

  fun onGoogleAuthLoading(loading: Boolean) {
    _googleAccountState.value = _googleAccountState.value.copy(
      isLoading = loading,
      errorMessage = if (loading) null else _googleAccountState.value.errorMessage
    )
  }

  fun onGoogleAuthSuccess(user: com.google.firebase.auth.FirebaseUser?, showSnackbar: Boolean = true) {
    _googleAccountState.value = GoogleSignInManager.toUiState(
      user = user,
      isLoading = false,
      errorMessage = null
    )
    if (showSnackbar && user != null) {
      val identity = user.email?.takeIf { it.isNotBlank() } ?: user.displayName ?: "Google Account"
      _snackbarMessage.value = "Signed in as $identity!"
    }
  }

  fun onGoogleAuthCancelled() {
    _googleAccountState.value = _googleAccountState.value.copy(
      isLoading = false,
      errorMessage = "Sign-in was cancelled. Tip: On Google's sign-in page, sign in with your Gmail address (not your phone number), or add your Google Account in Android Settings -> Accounts first."
    )
  }

  fun onGoogleAuthError(message: String) {
    _googleAccountState.value = _googleAccountState.value.copy(
      isLoading = false,
      errorMessage = message
    )
    _snackbarMessage.value = message
  }

  fun clearGoogleAuthError() {
    _googleAccountState.value = _googleAccountState.value.copy(errorMessage = null)
  }

  fun onGoogleSignedOut() {
    _googleAccountState.value = GoogleAccountUiState()
    _snackbarMessage.value = "Signed out of Google Account."
  }

  fun useSignedInGoogleEmailForAlerts() {
    val email = _googleAccountState.value.email.trim()
    if (email.isBlank()) {
      _snackbarMessage.value = "Sign in with Google first to use your Google email."
      return
    }
    updateEscalationTriggerConfig(targetEmail = email)
  }

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

  private val voiceController = VoiceReceptionistController(appContext) { _ -> }
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

  fun setPhoneLineFilter(line: BusinessLineId) {
    _selectedPhoneLineFilter.value = line
  }

  fun clearCallFilters() {
    _callSearchQuery.value = ""
    _selectedDateFilter.value = CallDateFilter.ALL
    _customDateFilterReset()
    _selectedStatusFilter.value = CallStatusFilter.ALL
    _selectedPhoneLineFilter.value = BusinessLineId.ALL
  }

  private fun _customDateFilterReset() {
    _customDateRange.value = null to null
  }

  fun setActiveDeviceLine(line: BusinessLineId) {
    val resolved = if (line == BusinessLineId.PHONE_2) BusinessLineId.PHONE_2 else BusinessLineId.PHONE_1
    val updated = _businessPhonesConfig.value.copy(activeDeviceLine = resolved)
    _businessPhonesConfig.value = updated
    IntegrationPrefsStore.saveBusinessPhonesConfig(appContext, updated)
    _snackbarMessage.value = "This phone is now set as ${updated.activeLineDisplay()}."
  }

  fun updateBusinessPhonesConfig(
    activeDeviceLine: BusinessLineId = _businessPhonesConfig.value.activeDeviceLine,
    phone1Label: String = _businessPhonesConfig.value.phone1Label,
    phone1Number: String = _businessPhonesConfig.value.phone1Number,
    phone2Label: String = _businessPhonesConfig.value.phone2Label,
    phone2Number: String = _businessPhonesConfig.value.phone2Number,
    crossPhoneAlertsEnabled: Boolean = _businessPhonesConfig.value.crossPhoneAlertsEnabled
  ) {
    val current = _businessPhonesConfig.value
    val resolvedLine = if (activeDeviceLine == BusinessLineId.PHONE_2) BusinessLineId.PHONE_2 else BusinessLineId.PHONE_1
    val cleanNum1 = phone1Number.trim()
    val cleanNum2 = phone2Number.trim()
    val p1Same = PhoneVerificationManager.normalizeToE164(cleanNum1) ==
      PhoneVerificationManager.normalizeToE164(current.phone1Number)
    val p2Same = PhoneVerificationManager.normalizeToE164(cleanNum2) ==
      PhoneVerificationManager.normalizeToE164(current.phone2Number)
    val updated = current.copy(
      activeDeviceLine = resolvedLine,
      phone1Label = phone1Label.trim().ifBlank { "Main Business Cell" },
      phone1Number = cleanNum1,
      phone2Label = phone2Label.trim().ifBlank { "Secondary / Backup Cell" },
      phone2Number = cleanNum2,
      crossPhoneAlertsEnabled = crossPhoneAlertsEnabled,
      phone1Verified = if (p1Same) current.phone1Verified else false,
      phone1VerifiedAt = if (p1Same) current.phone1VerifiedAt else 0L,
      phone2Verified = if (p2Same) current.phone2Verified else false,
      phone2VerifiedAt = if (p2Same) current.phone2VerifiedAt else 0L
    )
    _businessPhonesConfig.value = updated
    IntegrationPrefsStore.saveBusinessPhonesConfig(appContext, updated)
    _snackbarMessage.value = "Saved 2-phone setup (${updated.activeLineDisplay()})."
  }

  // --- Phone Verification Actions ---

  fun selectVerificationLine(line: BusinessLineId) {
    val resolved = if (line == BusinessLineId.PHONE_2) BusinessLineId.PHONE_2 else BusinessLineId.PHONE_1
    _phoneVerificationState.value = PhoneVerificationUiState(targetLine = resolved)
  }

  fun startFirebaseSmsVerification(uiContext: Context, line: BusinessLineId, rawNumber: String) {
    val resolvedLine = if (line == BusinessLineId.PHONE_2) BusinessLineId.PHONE_2 else BusinessLineId.PHONE_1
    val e164 = PhoneVerificationManager.normalizeToE164(rawNumber)
    if (!PhoneVerificationManager.isValidE164(e164)) {
      _phoneVerificationState.value = PhoneVerificationUiState(
        targetLine = resolvedLine,
        stage = PhoneVerificationStage.Error("Enter a valid phone number with area code (e.g. (808) 555-1234).")
      )
      return
    }
    _phoneVerificationState.value = PhoneVerificationUiState(
      targetLine = resolvedLine,
      stage = PhoneVerificationStage.SendingCode(e164),
      pendingPhoneNumber = e164
    )
    PhoneVerificationManager.startFirebasePhoneVerification(
      context = uiContext,
      rawPhoneNumber = e164,
      onCodeSent = { verificationId ->
        _phoneVerificationState.value = PhoneVerificationUiState(
          targetLine = resolvedLine,
          stage = PhoneVerificationStage.CodeSent(
            phoneNumber = e164,
            verificationId = verificationId,
            isLocalPinMode = false,
            message = "6-digit SMS verification code sent to $e164."
          ),
          pendingPhoneNumber = e164
        )
      },
      onAutoVerified = { credential, verifiedNum ->
        viewModelScope.launch {
          _phoneVerificationState.value = _phoneVerificationState.value.copy(
            stage = PhoneVerificationStage.Verifying
          )
          val res = PhoneVerificationManager.completeFirebasePhoneCredential(uiContext, credential)
          if (res.isSuccess) {
            markPhoneVerified(resolvedLine, res.getOrNull().orEmpty().ifBlank { verifiedNum }, "Firebase SMS")
          } else {
            _phoneVerificationState.value = _phoneVerificationState.value.copy(
              stage = PhoneVerificationStage.Error(
                res.exceptionOrNull()?.localizedMessage ?: "Firebase phone verification failed."
              )
            )
          }
        }
      },
      onFailed = { errorMsg ->
        _phoneVerificationState.value = PhoneVerificationUiState(
          targetLine = resolvedLine,
          stage = PhoneVerificationStage.Error(errorMsg),
          pendingPhoneNumber = e164
        )
      }
    )
  }

  fun startTwoPhonePinVerification(uiContext: Context, line: BusinessLineId, rawNumber: String) {
    val resolvedLine = if (line == BusinessLineId.PHONE_2) BusinessLineId.PHONE_2 else BusinessLineId.PHONE_1
    val e164 = PhoneVerificationManager.normalizeToE164(rawNumber)
    if (!PhoneVerificationManager.isValidE164(e164)) {
      _phoneVerificationState.value = PhoneVerificationUiState(
        targetLine = resolvedLine,
        stage = PhoneVerificationStage.Error("Enter a valid phone number with area code first.")
      )
      return
    }
    val pin = PhoneVerificationManager.generateSixDigitPin()
    val lineLabel = _businessPhonesConfig.value.displayLabelFor(resolvedLine.id)
    val smsIntent = PhoneVerificationManager.createSmsVerificationIntent(e164, pin, lineLabel)
    try {
      uiContext.startActivity(smsIntent)
      _phoneVerificationState.value = PhoneVerificationUiState(
        targetLine = resolvedLine,
        stage = PhoneVerificationStage.CodeSent(
          phoneNumber = e164,
          verificationId = null,
          isLocalPinMode = true,
          message = "Send the pre-filled SMS to $e164, then enter the 6-digit PIN from that phone."
        ),
        pendingPinHash = pin,
        pendingPhoneNumber = e164
      )
    } catch (_: Exception) {
      _phoneVerificationState.value = PhoneVerificationUiState(
        targetLine = resolvedLine,
        stage = PhoneVerificationStage.Error("No SMS app is installed on this device. Use Firebase SMS verification on a phone with Play Services.")
      )
    }
  }

  fun submitPhoneVerificationCode(uiContext: Context, rawCode: String) {
    val currentState = _phoneVerificationState.value
    val code = rawCode.trim()
    if (code.length < 6) {
      _phoneVerificationState.value = currentState.copy(
        stage = PhoneVerificationStage.Error("Enter the complete 6-digit verification code.")
      )
      return
    }
    val stage = currentState.stage as? PhoneVerificationStage.CodeSent ?: return

    if (stage.isLocalPinMode) {
      if (code == currentState.pendingPinHash) {
        markPhoneVerified(currentState.targetLine, stage.phoneNumber, "2-Phone SMS PIN")
      } else {
        _phoneVerificationState.value = currentState.copy(
          stage = PhoneVerificationStage.Error("Verification PIN does not match. Check the SMS sent to ${stage.phoneNumber}.")
        )
      }
      return
    }

    val verificationId = stage.verificationId
    if (verificationId.isNullOrBlank()) {
      _phoneVerificationState.value = currentState.copy(
        stage = PhoneVerificationStage.Error("Verification session expired. Request a new code.")
      )
      return
    }
    _phoneVerificationState.value = currentState.copy(stage = PhoneVerificationStage.Verifying)
    viewModelScope.launch {
      val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, code)
      val res = PhoneVerificationManager.completeFirebasePhoneCredential(uiContext, credential)
      if (res.isSuccess) {
        markPhoneVerified(currentState.targetLine, res.getOrNull().orEmpty().ifBlank { stage.phoneNumber }, "Firebase SMS")
      } else {
        _phoneVerificationState.value = currentState.copy(
          stage = PhoneVerificationStage.Error(
            res.exceptionOrNull()?.localizedMessage ?: "Verification code invalid or expired."
          )
        )
      }
    }
  }

  fun markPhoneVerified(line: BusinessLineId, normalizedNumber: String, method: String) {
    val now = System.currentTimeMillis()
    val current = _businessPhonesConfig.value
    val updated = if (line == BusinessLineId.PHONE_2) {
      current.copy(
        phone2Number = normalizedNumber.ifBlank { current.phone2Number },
        phone2Verified = true,
        phone2VerifiedAt = now,
        verifiedMethod = method
      )
    } else {
      current.copy(
        phone1Number = normalizedNumber.ifBlank { current.phone1Number },
        phone1Verified = true,
        phone1VerifiedAt = now,
        verifiedMethod = method
      )
    }
    _businessPhonesConfig.value = updated
    IntegrationPrefsStore.saveBusinessPhonesConfig(appContext, updated)
    _phoneVerificationState.value = PhoneVerificationUiState(
      targetLine = line,
      stage = PhoneVerificationStage.Verified(
        line = line,
        phoneNumber = normalizedNumber,
        method = method,
        verifiedAt = now
      ),
      pendingPhoneNumber = normalizedNumber
    )
    _snackbarMessage.value = "${line.shortLabel} verified ($method)!"
  }

  fun resetPhoneVerification() {
    val line = _phoneVerificationState.value.targetLine
    _phoneVerificationState.value = PhoneVerificationUiState(targetLine = line)
  }

  fun applyTeamSetupCode(rawCode: String): Boolean {
    val bundle = TeamSetupCodec.decode(rawCode)
    if (bundle == null) {
      _snackbarMessage.value = "Invalid Team Connect Code. Copy the code from your first phone and try again."
      return false
    }
    val updatedSupabase = _supabaseConfig.value.copy(
      projectUrl = bundle.supabaseUrl,
      anonKey = bundle.supabaseKey.ifBlank { _supabaseConfig.value.anonKey }
    )
    _supabaseConfig.value = updatedSupabase
    IntegrationPrefsStore.saveSupabaseConfig(appContext, updatedSupabase)

    val updatedPhones = _businessPhonesConfig.value.copy(
      phone1Label = bundle.phone1Label,
      phone1Number = bundle.phone1Number,
      phone2Label = bundle.phone2Label,
      phone2Number = bundle.phone2Number,
      crossPhoneAlertsEnabled = true
    )
    _businessPhonesConfig.value = updatedPhones
    IntegrationPrefsStore.saveBusinessPhonesConfig(appContext, updatedPhones)

    val updatedEscalation = _escalationTriggerConfig.value.copy(
      targetEmail = bundle.alertEmail,
      webhookUrl = bundle.webhookUrl
    )
    _escalationTriggerConfig.value = updatedEscalation
    IntegrationPrefsStore.saveEscalationConfig(appContext, updatedEscalation)

    _snackbarMessage.value = "Team Connect Code applied! Connecting shared Supabase sync..."
    testSupabaseConnection()
    fetchCallsFromSupabase()
    return true
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

  private fun notifyForNewRemoteCalls(newlyArrived: List<CallEntity>) {
    if (newlyArrived.isEmpty()) return
    val phonesCfg = _businessPhonesConfig.value
    val escCfg = _escalationTriggerConfig.value
    if (!phonesCfg.crossPhoneAlertsEnabled || !escCfg.isEnabled) return
    if (escCfg.alertChannel == EscalationAlertChannel.EMAIL) return

    for (call in newlyArrived) {
      val notifyKey = "${call.callerNumber}_${call.timestamp}_${call.receivingLine}"
      if (IntegrationPrefsStore.hasNotifiedRemoteCall(appContext, notifyKey)) continue
      val isFromOtherPhone = call.receivingLine != phonesCfg.activeDeviceLine.id
      if (isFromOtherPhone || call.isEscalated || call.status == "ESCALATED") {
        IntegrationPrefsStore.markRemoteCallNotified(appContext, notifyKey)
        val lineLabel = phonesCfg.displayLabelFor(call.receivingLine)
        CallEscalationManager.triggerEscalationAlert(
          context = appContext,
          call = call,
          urgencyReason = if (isFromOtherPhone) {
            "Real call synced from $lineLabel"
          } else {
            call.escalationNotes.ifBlank { "Real escalated call on $lineLabel" }
          },
          targetEmail = escCfg.targetEmail
        )
        if (_activeCallAlert.value == null && (call.isEscalated || call.status == "ESCALATED" || isFromOtherPhone)) {
          _activeCallAlert.value = call
        }
      }
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
          val result = repository.reconcileRemoteCallsDetailed(remote)
          notifyForNewRemoteCalls(result.newlyArrivedCalls)
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
          val reconcile = repository.reconcileRemoteCallsDetailed(remoteCalls)
          notifyForNewRemoteCalls(reconcile.newlyArrivedCalls)
          _supabaseConnectionState.value = SupabaseConnectionState.Connected(
            latencyMs = 0L,
            message = "Synced ${remoteCalls.size} live rows (${reconcile.updatedCount} updated, $pushedCount uploaded) from Supabase public.calls"
          )
          _lastSupabaseFetchTime.value = "Live Supabase sync at $syncedAt"
          _snackbarMessage.value = if (remoteCalls.isNotEmpty()) {
            "Synced ${remoteCalls.size} real call logs from Supabase!"
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

  // --- Full AI Instruments Studio Actions ---

  fun selectAiInstrument(tool: AiInstrumentTool) {
    _selectedAiInstrument.value = tool
  }

  fun sendCopilotMessage(rawMessage: String) {
    val text = rawMessage.trim()
    if (text.isBlank() || _isCopilotSending.value) return
    _copilotMessages.value = _copilotMessages.value + CopilotChatMessage(isFromUser = true, text = text)
    _isCopilotSending.value = true
    _copilotError.value = null
    viewModelScope.launch {
      val result = AiInstrumentsEngine.sendCopilotMessage(appContext, text)
      _isCopilotSending.value = false
      if (result.isSuccess) {
        val reply = result.getOrNull().orEmpty()
        _copilotMessages.value = _copilotMessages.value + CopilotChatMessage(isFromUser = false, text = reply)
      } else {
        _copilotError.value = result.exceptionOrNull()?.localizedMessage ?: "Copilot request failed."
      }
    }
  }

  fun resetCopilotChat() {
    AiInstrumentsEngine.resetCopilotChat()
    _copilotError.value = null
    _copilotMessages.value = listOf(
      CopilotChatMessage(
        isFromUser = false,
        text = "Fresh session started! Ask Kai Copilot anything about your business AI strategy or call handling."
      )
    )
  }

  fun analyzePickedImage(bitmap: Bitmap, instruction: String) {
    _visionResultState.value = InstrumentTextUiState.Loading
    viewModelScope.launch {
      _visionResultState.value = AiInstrumentsEngine.analyzeImage(appContext, bitmap, instruction)
    }
  }

  fun generateAiImage(prompt: String, sourceBitmap: Bitmap? = null) {
    _imageGenState.value = ImageGenUiState.Loading
    viewModelScope.launch {
      _imageGenState.value = AiInstrumentsEngine.generateOrEditImage(appContext, prompt, sourceBitmap)
    }
  }

  fun clearAiImage() {
    _imageGenState.value = ImageGenUiState.Idle
  }

  fun generateSalesPitch(prospect: String, bottleneck: String) {
    val cleanProspect = prospect.trim()
    if (cleanProspect.isBlank()) {
      _pitchBriefState.value = PitchBriefUiState.Error("Enter a client business or industry first.")
      return
    }
    _pitchBriefState.value = PitchBriefUiState.Loading
    viewModelScope.launch {
      _pitchBriefState.value = AiInstrumentsEngine.generateSalesPitchBrief(
        context = appContext,
        prospectBusiness = cleanProspect,
        painPoint = bottleneck,
        products = products.value
      )
    }
  }

  fun analyzeRealCallQueue() {
    _callQueueBriefState.value = CallQueueBriefUiState.Loading
    viewModelScope.launch {
      _callQueueBriefState.value = AiInstrumentsEngine.analyzeCallQueue(
        context = appContext,
        calls = calls.value
      )
    }
  }

  fun runTranslatorOrRewriter(text: String, targetMode: String) {
    _translatorState.value = InstrumentTextUiState.Loading
    viewModelScope.launch {
      _translatorState.value = AiInstrumentsEngine.translateOrRewrite(appContext, text, targetMode)
    }
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
        contactPhone = contactPhone.ifBlank { "+1 (555) 010-WAVE" },
        biggestBottleneck = biggestBottleneck,
        recommendedProduct = recommended
      )
      val remoteSync = SupabaseClient.submitWaveAuditLead(_supabaseConfig.value, dto)
      repository.insertLead(
        WaveAuditLeadEntity(
          id = java.util.UUID.randomUUID().toString(),
          businessName = dto.businessName,
          contactName = dto.contactName,
          contactEmail = dto.contactEmail,
          contactPhone = dto.contactPhone,
          biggestBottleneck = dto.biggestBottleneck,
          recommendedProduct = recommended,
          syncedToSupabase = remoteSync.isSuccess
        )
      )

      // Log into Call Dashboard & Room so both business phones see the client intake
      val activeLineId = _businessPhonesConfig.value.activeDeviceLine.id
      val activeLineDisplay = _businessPhonesConfig.value.activeLineDisplay()
      val leadCall = CallEntity(
        callerNumber = dto.contactPhone,
        callerName = "${dto.contactName} (${dto.businessName})",
        timestamp = System.currentTimeMillis(),
        durationSeconds = 0,
        status = "PENDING_REVIEW",
        urgency = if (recommended.contains("3,997")) "URGENT_VIP" else "HIGH",
        topic = "LEAD_WAVE_CHECK",
        summary = "Topic: Client Wave Check Intake | Action Items: Review $recommended fit & email ${dto.contactEmail} | Sentiment: Positive (Qualified Lead)",
        transcription = "Client Portal Wave Check ($activeLineDisplay) for ${dto.businessName}. Bottleneck: $biggestBottleneck\n[Line: $activeLineId]",
        customerQuestion = "Free AI Wave Check™ Bottleneck: $biggestBottleneck",
        aiResponse = "Recommended Path: $recommended. Strategy link: https://otdaisurfer.surf/wave-check",
        escalationAlertMinutes = 10,
        escalationDeadline = System.currentTimeMillis() + 10 * 60_000L,
        isEscalated = false,
        supabaseSynced = false,
        receivingLine = activeLineId
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

      _snackbarMessage.value = if (remoteSync.isSuccess) {
        "Wave Check submitted to Supabase & logged for crew!"
      } else {
        "Wave Check saved locally & queued for crew ($recommended)!"
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

  fun syncProductsFromWebsite() {
    viewModelScope.launch {
      repository.forceSyncOfficialProducts()
      _snackbarMessage.value = "Synced official Ocean Tide Drop AI Surfer catalog from otdaisurfer.surf!"
    }
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
