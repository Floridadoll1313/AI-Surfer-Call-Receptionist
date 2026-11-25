package com.example.integrations

import com.example.data.local.CallEntity

object SupabaseCallFetcher {

  fun formatCallAsSupabaseJson(call: CallEntity): String {
    return """
{
  "id": ${call.id},
  "caller_id": "${call.callerName} <${call.callerNumber}>",
  "caller_name": "${call.callerName}",
  "caller_number": "${call.callerNumber}",
  "receiving_line": "${call.receivingLine}",
  "call_timestamp": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date(call.timestamp))}",
  "call_duration_seconds": ${call.durationSeconds},
  "status": "${call.status}",
  "urgency": "${call.urgency}",
  "topic": "${call.topic}",
  "chatgpt_summary": "${call.summary.replace("\"", "\\\"")}",
  "customer_question": "${call.customerQuestion.replace("\"", "\\\"")}",
  "ai_response": "${call.aiResponse.replace("\"", "\\\"")}",
  "is_escalated": ${call.isEscalated},
  "escalated_at": ${if (call.escalatedAt > 0) "\"${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date(call.escalatedAt))}\"" else "null"},
  "source_table": "public.swell_call_logs (public.calls)"
}
""".trimIndent()
  }
}
