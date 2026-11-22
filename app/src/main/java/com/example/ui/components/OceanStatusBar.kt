package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.OceanAbyss
import com.example.ui.theme.OceanCard
import com.example.ui.theme.OceanCardSecondary
import com.example.ui.theme.OceanDeepBlue
import com.example.ui.theme.SandTextPrimary
import com.example.ui.theme.SandTextSecondary
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfCyan
import com.example.ui.theme.SurfTeal
import com.example.ui.theme.SurfTurquoise

@Composable
fun OceanStatusBar(
  isOnline: Boolean = true,
  pendingCount: Int = 0,
  escalatedCount: Int = 0,
  reviewedCount: Int = 0,
  triggerChannelLabel: String = "NOTIFY + EMAIL",
  targetEmail: String = "oceantidedropservice@gmail.com",
  onOpenAlerts: () -> Unit = {},
  onTriggerTestAlert: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  // Pulsing animation for the green live indicator & alert indicator
  val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
  val pulseColor by infiniteTransition.animateColor(
    initialValue = SeaGreen,
    targetValue = SeaGreen.copy(alpha = 0.3f),
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_color"
  )

  val alertPulseColor by infiniteTransition.animateColor(
    initialValue = CoralAlert,
    targetValue = CoralAlert.copy(alpha = 0.4f),
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alert_pulse"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          colors = listOf(OceanAbyss, OceanDeepBlue)
        )
      )
      .statusBarsPadding()
      .testTag("app_status_bar")
  ) {
    // Primary Status Bar Line
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Live Status Indicator & System Name
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clickable { isExpanded = !isExpanded }
          .padding(vertical = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (isOnline) pulseColor else SandTextSecondary)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
          text = if (isOnline) "24/7 AI RECEPTIONIST" else "STANDBY",
          color = if (isOnline) SeaGreen else SandTextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = SurfTeal.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CellTower,
              contentDescription = null,
              tint = SurfCyan,
              modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "CELL GUARD",
              color = SurfCyan,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Call Alerts Status Pill
      if (escalatedCount > 0) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = alertPulseColor,
          border = BorderStroke(1.dp, CoralAlert),
          modifier = Modifier
            .clickable(onClick = onOpenAlerts)
            .testTag("status_bar_alert_pill")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = "Alerts",
              tint = Color.White,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "⚠️ $escalatedCount CALL ALERT${if (escalatedCount > 1) "S" else ""}",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = OceanCard,
          border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.3f)),
          modifier = Modifier.clickable { isExpanded = !isExpanded }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = SeaGreen,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (pendingCount > 0) "$pendingCount In Queue" else "Queue Clear",
              color = if (pendingCount > 0) SunsetOrange else SeaGreen,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Expand/Collapse Chevron
      IconButton(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier.size(24.dp)
      ) {
        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = "Expand Status Details",
          tint = SandTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    // Expandable System Health & Quick Telemetry Tray
    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(OceanDeepBlue)
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        // Detailed metrics strip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          StatusBarMetricPill(
            label = "Pending SLA",
            value = "$pendingCount Calls",
            color = SunsetOrange,
            icon = Icons.Default.HourglassTop,
            modifier = Modifier.weight(1f)
          )
          StatusBarMetricPill(
            label = "Escalated",
            value = "$escalatedCount Alerts",
            color = if (escalatedCount > 0) CoralAlert else SeaGreen,
            icon = Icons.Default.Warning,
            modifier = Modifier.weight(1f)
          )
          StatusBarMetricPill(
            label = "Reviewed",
            value = "$reviewedCount Closed",
            color = SeaGreen,
            icon = Icons.Default.CloudDone,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Crew Dispatch destination & Test Alert trigger
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = SurfCyan,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Trigger ($triggerChannelLabel) -> $targetEmail",
            color = SandTextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
          )

          Spacer(modifier = Modifier.width(6.dp))

          Button(
            onClick = onTriggerTestAlert,
            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .height(30.dp)
              .testTag("trigger_test_alert_button")
          ) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = null,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Test Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Bottom subtle hairline border
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(SurfTeal.copy(alpha = 0.35f))
    )
  }
}

@Composable
private fun StatusBarMetricPill(
  label: String,
  value: String,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = OceanCard,
    border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Column {
        Text(
          text = value,
          color = color,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = label,
          color = SandTextSecondary,
          fontSize = 9.sp
        )
      }
    }
  }
}
