package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CallEntity
import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity
import com.example.data.local.WaveAuditLeadEntity
import com.example.integrations.IntegrationPrefsStore
import com.example.integrations.SupabaseCallSyncWorker
import com.example.telephony.CallEscalationManager
import com.example.telephony.EscalationWebhookDispatcher
import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {

  private val callDao = database.callDao()
  private val productDao = database.productDao()
  private val faqDao = database.faqDao()
  private val waveAuditLeadDao = database.waveAuditLeadDao()

  val allCalls: Flow<List<CallEntity>> = callDao.getAllCalls()
  val latestCalls: Flow<List<CallEntity>> = callDao.getLatestCalls()
  val pendingCalls: Flow<List<CallEntity>> = callDao.getPendingCalls()

  fun observeLatestCalls(limit: Int = 25): Flow<List<CallEntity>> = callDao.getLatestCalls(limit)
  val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
  val allFaqs: Flow<List<FaqEntity>> = faqDao.getAllFaqs()
  val allLeads: Flow<List<WaveAuditLeadEntity>> = waveAuditLeadDao.getAllLeads()

  suspend fun insertCall(call: CallEntity): Long {
    return callDao.insertCall(call)
  }

  suspend fun insertCalls(calls: List<CallEntity>) {
    callDao.insertCalls(calls)
  }

  suspend fun getUnsyncedCalls(): List<CallEntity> {
    return callDao.getUnsyncedCalls()
  }

  suspend fun markSupabaseSynced(callId: Long) {
    callDao.markSupabaseSynced(callId)
  }

  suspend fun reconcileRemoteCalls(remoteCalls: List<CallEntity>): Int {
    if (remoteCalls.isEmpty()) return 0
    val localCalls = callDao.getAllCallsSnapshot()
    var updated = 0
    for (remote in remoteCalls) {
      val match = localCalls.firstOrNull { local ->
        local.id == remote.id ||
          (ReturningCallerMatcher.normalizePhone(local.callerNumber) == ReturningCallerMatcher.normalizePhone(remote.callerNumber) &&
            kotlin.math.abs(local.timestamp - remote.timestamp) < 5_000L)
      }
      if (match != null) {
        if (match.status != remote.status || !match.supabaseSynced || match.isEscalated != remote.isEscalated) {
          callDao.updateCall(
            match.copy(
              status = remote.status,
              isEscalated = remote.isEscalated,
              escalationNotes = remote.escalationNotes.ifBlank { match.escalationNotes },
              supabaseSynced = true
            )
          )
          updated++
        }
      } else {
        callDao.insertCall(remote.copy(supabaseSynced = true))
        updated++
      }
    }
    return updated
  }

  suspend fun insertLead(lead: WaveAuditLeadEntity) {
    waveAuditLeadDao.insertLead(lead)
  }

  suspend fun findReturningCallerContext(phoneNumber: String): ReturningCallerContext {
    val calls = callDao.getAllCallsSnapshot()
    val leads = waveAuditLeadDao.getAllLeadsSnapshot()
    return ReturningCallerMatcher.match(phoneNumber, calls, leads)
  }

  suspend fun getCallCount(): Int {
    return callDao.getCallCount()
  }

  suspend fun markReviewed(callId: Long) {
    callDao.markReviewed(callId)
  }

  suspend fun markPending(callId: Long) {
    callDao.markPending(callId)
  }

  suspend fun deleteCall(callId: Long) {
    callDao.deleteCall(callId)
  }

  suspend fun clearAllCalls() {
    callDao.clearAllCalls()
  }

  suspend fun escalateCallNow(
    context: Context,
    call: CallEntity,
    reason: String = "Manual crew escalation or overdue trigger",
    targetEmail: String = CallEscalationManager.TARGET_CREW_EMAIL,
    sendNotification: Boolean = true,
    webhookUrl: String = IntegrationPrefsStore.loadEscalationConfig(context).webhookUrl
  ): CallEntity {
    val updatedCall = call.copy(
      status = "ESCALATED",
      isEscalated = true,
      escalatedAt = System.currentTimeMillis(),
      escalationNotes = reason,
      supabaseSynced = false
    )
    callDao.updateCall(updatedCall)
    if (sendNotification) {
      CallEscalationManager.triggerEscalationAlert(context, updatedCall, reason, targetEmail)
    }
    if (webhookUrl.isNotBlank()) {
      EscalationWebhookDispatcher.dispatch(
        webhookUrl = webhookUrl,
        call = updatedCall,
        reason = reason,
        targetEmail = targetEmail
      )
    }
    SupabaseCallSyncWorker.enqueueImmediateSync(context)
    return updatedCall
  }

  suspend fun checkOverdueEscalations(context: Context): List<CallEntity> {
    val currentTime = System.currentTimeMillis()
    val overdue = callDao.getCallsNeedingEscalation(currentTime)
    val cfg = IntegrationPrefsStore.loadEscalationConfig(context)
    for (call in overdue) {
      escalateCallNow(
        context = context,
        call = call,
        reason = "Call unreviewed past ${call.escalationAlertMinutes} minutes limit! Escalated to ocean crew.",
        targetEmail = cfg.targetEmail,
        sendNotification = cfg.alertChannel != com.example.integrations.EscalationAlertChannel.EMAIL,
        webhookUrl = cfg.webhookUrl
      )
    }
    return overdue
  }

  suspend fun insertProduct(product: ProductEntity): Long {
    return productDao.insertProduct(product)
  }

  suspend fun insertFaq(faq: FaqEntity): Long {
    return faqDao.insertFaq(faq)
  }

  suspend fun forceSyncOfficialProducts() {
    AppDatabase.forceSyncOfficialProducts(database)
  }

  suspend fun getProductsSnapshot(): List<ProductEntity> {
    val stored = productDao.getAllProductsSnapshot()
    return stored.ifEmpty { AppDatabase.initialProducts }
  }

  suspend fun getFaqsSnapshot(): List<FaqEntity> {
    val stored = faqDao.getAllFaqsSnapshot()
    return stored.ifEmpty { AppDatabase.initialFaqs }
  }
}
