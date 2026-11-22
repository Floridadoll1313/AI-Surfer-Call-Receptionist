package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallEntity
import com.example.telephony.CallEscalationManager
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.OceanAbyss
import com.example.ui.theme.OceanDeepBlue
import com.example.ui.theme.SandTextPrimary
import com.example.ui.theme.SandTextSecondary
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SurfTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallAlertBanner(
  callAlert: CallEntity?,
  targetAlertEmail: String = CallEscalationManager.TARGET_CREW_EMAIL,
  onDismiss: () -> Unit,
  onMarkReviewed: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  AnimatedVisibility(
    visible = callAlert != null,
    enter = slideInVertically() + fadeIn(),
    exit = slideOutVertically() + fadeOut()
  ) {
    if (callAlert != null) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
        border = BorderStroke(2.dp, CoralAlert),
        modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp)
          .testTag("call_alert_banner")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Alert Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = CoralAlert.copy(alpha = 0.2f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = "Alert",
                  tint = CoralAlert,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "⚠️ AI ESCALATION CALL ALERT",
                color = CoralAlert,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
              )
              Text(
                text = "Flagged for immediate crew review • Target: $targetAlertEmail",
                color = SandTextSecondary,
                fontSize = 10.sp
              )
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss Alert",
                tint = SandTextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Caller Details & Snippet
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = OceanAbyss,
            border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = callAlert.callerName,
                  color = SandTextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "(${callAlert.callerNumber})",
                  color = SandTextSecondary,
                  fontSize = 12.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = CoralAlert
                ) {
                  Text(
                    text = callAlert.urgency,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "\"${callAlert.customerQuestion}\"",
                color = SandTextPrimary,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
              )

              if (callAlert.escalationNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Note: ${callAlert.escalationNotes}",
                  color = GoldenSun,
                  fontSize = 10.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Quick Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Call Back
            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${callAlert.callerNumber}"))
                try { context.startActivity(intent) } catch (e: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Call Back", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Email Crew
            Button(
              onClick = {
                val intent = CallEscalationManager.createEmailEscalationIntent(callAlert, targetAlertEmail)
                try {
                  context.startActivity(Intent.createChooser(intent, "Send Escalation Email"))
                } catch (_: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Email Alert", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Mark Reviewed & Clear Alert
            Button(
              onClick = {
                onMarkReviewed(callAlert.id)
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = SeaGreen),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = OceanAbyss, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Resolve", color = OceanAbyss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
