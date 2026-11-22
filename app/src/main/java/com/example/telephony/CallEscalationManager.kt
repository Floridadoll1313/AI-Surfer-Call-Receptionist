package com.example.telephony

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.OceanSurferApp
import com.example.R
import com.example.data.local.CallEntity

object CallEscalationManager {

  const val TARGET_CREW_EMAIL = "oceantidedropservice@gmail.com"

  fun triggerEscalationAlert(
    context: Context,
    call: CallEntity,
    urgencyReason: String = "Call message left unreviewed past SLA threshold",
    targetEmail: String = TARGET_CREW_EMAIL
  ) {
    try {
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("NAVIGATE_TO", "calls")
        putExtra("CALL_ID", call.id)
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        call.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val emailIntent = createEmailEscalationIntent(call, targetEmail, urgencyReason)
      val emailPendingIntent = PendingIntent.getActivity(
        context,
        call.id.toInt() + 10000,
        Intent.createChooser(emailIntent, "Send Escalation Alert Email"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, OceanSurferApp.CHANNEL_CALL_ALERTS)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("🌊 AI Escalation Alert: ${call.callerName}")
        .setContentText("${call.urgency} • ${call.customerQuestion.take(65)}")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(
              "Caller: ${call.callerName} (${call.callerNumber})\n" +
              "Topic: ${call.topic} | Urgency: ${call.urgency}\n" +
              "Question: ${call.customerQuestion}\n" +
              "AI Summary: ${call.summary.take(140)}\n\n" +
              "⚠️ Escalation Reason: $urgencyReason"
            )
        )
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .addAction(
          android.R.drawable.ic_dialog_email,
          "Email Alert",
          emailPendingIntent
        )
        .addAction(
          android.R.drawable.ic_menu_view,
          "Call Dashboard",
          pendingIntent
        )
        .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
        .build()

      val manager = NotificationManagerCompat.from(context)
      manager.notify(call.id.toInt(), notification)
    } catch (e: SecurityException) {
      // Notification permission may need to be requested in runtime on Android 13+
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun createEmailEscalationIntent(
    call: CallEntity,
    targetEmail: String = TARGET_CREW_EMAIL,
    escalationReason: String = call.escalationNotes.ifBlank { "AI Receptionist flagged this call for human escalation." }
  ): Intent {
    val recipient = targetEmail.ifBlank { TARGET_CREW_EMAIL }
    val subject = "⚠️ AI Escalation Alert [${call.urgency}]: Call from ${call.callerName}"
    val body = """
Ocean Tide Drop AI Surfer - AI Escalation Alert Notice
------------------------------------------------------------
Caller: ${call.callerName}
Phone Number: ${call.callerNumber}
Urgency Level: ${call.urgency}
Topic: ${call.topic}
Escalation Reason: $escalationReason
Call Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(call.timestamp))}

Customer Inquiry:
"${call.customerQuestion}"

AI Surfer Receptionist Response:
"${call.aiResponse}"

AI Summary:
${call.summary}

Status: Flagged for human crew review & immediate follow-up.
Recipient: $recipient
""".trimIndent()

    return Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:$recipient")
      putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, body)
    }
  }

  fun createSmsEscalationIntent(call: CallEntity): Intent {
    val message = "Ocean Tide Alert: Unreviewed call from ${call.callerName} (${call.callerNumber}): '${call.customerQuestion.take(80)}'. Review in Ocean Surfer App!"
    return Intent(Intent.ACTION_VIEW).apply {
      data = Uri.parse("sms:${call.callerNumber}")
      putExtra("sms_body", message)
    }
  }

  fun createDialerIntent(phoneNumber: String): Intent {
    return Intent(Intent.ACTION_DIAL).apply {
      data = Uri.parse("tel:$phoneNumber")
    }
  }
}
