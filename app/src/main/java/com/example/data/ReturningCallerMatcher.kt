package com.example.data

import com.example.data.local.CallEntity
import com.example.data.local.WaveAuditLeadEntity

data class ReturningCallerContext(
  val normalizedPhone: String,
  val matchedName: String? = null,
  val businessName: String? = null,
  val priorCallCount: Int = 0,
  val lastTopic: String? = null,
  val lastQuestion: String? = null,
  val lastUrgency: String? = null,
  val recommendedProduct: String? = null,
  val bottleneck: String? = null,
  val isVipOrReturning: Boolean = false
) {
  fun toBadgeText(): String {
    if (!isVipOrReturning) return ""
    val parts = buildList {
      if (priorCallCount > 0) add("$priorCallCount prior ${if (priorCallCount == 1) "call" else "calls"}")
      businessName?.takeIf { it.isNotBlank() }?.let { add(it) }
      recommendedProduct?.takeIf { it.isNotBlank() }?.let { add(it) }
        ?: lastTopic?.takeIf { it.isNotBlank() }?.let { add("Last: ${it.replace('_', ' ')}") }
    }
    val prefix = if (lastUrgency == "URGENT_VIP" || priorCallCount >= 2) "VIP Returning Caller" else "Returning Lead"
    return if (parts.isEmpty()) prefix else "$prefix • ${parts.joinToString(" • ")}"
  }

  fun toPromptContext(): String {
    if (!isVipOrReturning) return ""
    return buildString {
      append("Returning Caller Profile: ")
      matchedName?.let { append("Name=$it; ") }
      businessName?.let { append("Business=$it; ") }
      append("PriorCalls=$priorCallCount; ")
      lastTopic?.let { append("LastTopic=$it; ") }
      lastQuestion?.let { append("PreviousQuestion=\"${it.take(120)}\"; ") }
      recommendedProduct?.let { append("RecommendedSolution=$it; ") }
      bottleneck?.let { append("Bottleneck=$it; ") }
      append("Greet them warmly by name as a returning guest and reference their prior context.")
    }
  }
}

object ReturningCallerMatcher {

  fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isBlank()) return ""
    return if (digits.length >= 10) digits.takeLast(10) else digits
  }

  fun match(
    phoneNumber: String,
    priorCalls: List<CallEntity>,
    priorLeads: List<WaveAuditLeadEntity>
  ): ReturningCallerContext {
    val norm = normalizePhone(phoneNumber)
    if (norm.length < 7) {
      return ReturningCallerContext(normalizedPhone = norm)
    }

    val matchingCalls = priorCalls.filter { normalizePhone(it.callerNumber) == norm }
      .sortedByDescending { it.timestamp }
    val matchingLead = priorLeads.firstOrNull { normalizePhone(it.contactPhone) == norm }

    if (matchingCalls.isEmpty() && matchingLead == null) {
      return ReturningCallerContext(normalizedPhone = norm)
    }

    val latestCall = matchingCalls.firstOrNull()
    val resolvedName = matchingLead?.contactName?.takeIf { it.isNotBlank() }
      ?: latestCall?.callerName?.takeIf { it.isNotBlank() && !it.equals("Cell Phone Caller", ignoreCase = true) }

    return ReturningCallerContext(
      normalizedPhone = norm,
      matchedName = resolvedName,
      businessName = matchingLead?.businessName?.takeIf { it.isNotBlank() },
      priorCallCount = matchingCalls.size,
      lastTopic = latestCall?.topic,
      lastQuestion = latestCall?.customerQuestion,
      lastUrgency = latestCall?.urgency,
      recommendedProduct = matchingLead?.recommendedProduct,
      bottleneck = matchingLead?.biggestBottleneck,
      isVipOrReturning = true
    )
  }
}
