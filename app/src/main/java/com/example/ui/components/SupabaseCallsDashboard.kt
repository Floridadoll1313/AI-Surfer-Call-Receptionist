package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallEntity
import com.example.integrations.EscalationAlertChannel
import com.example.integrations.EscalationTriggerConfig
import com.example.integrations.SupabaseCallFetcher
import com.example.telephony.CallEscalationManager
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.OceanAbyss
import com.example.ui.theme.OceanCard
import com.example.ui.theme.OceanCardSecondary
import com.example.ui.theme.OceanDeepBlue
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.SandTextPrimary
import com.example.ui.theme.SandTextSecondary
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfCyan
import com.example.ui.theme.SurfTeal
import com.example.ui.theme.SurfTurquoise
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DashboardCategory(val id: String, val label: String, val icon: ImageVector) {
  ALL("ALL", "All Calls", Icons.Default.FilterList),
  LEADS("LEADS", "🎯 Leads & Audits", Icons.Default.AutoAwesome),
  PURCHASES("PURCHASES", "💳 Wave Starter Purchases", Icons.Default.CreditCard),
  BOOKINGS("BOOKINGS", "📅 Strategy Bookings", Icons.Default.Alarm),
  ENTERPRISE("ENTERPRISE", "🌊 Big Kahuna VIP", Icons.Default.Surfing),
  AEO("AEO", "🔍 AEO Visibility", Icons.Default.Search),
  SUPPORT("SUPPORT", "💬 Customer Care", Icons.Default.PhoneInTalk),
  ESCALATED("ESCALATED", "⚠️ Overdue Escalations", Icons.Default.Warning)
}

fun categorizeCall(call: CallEntity): DashboardCategory {
  val q = call.customerQuestion.lowercase()
  val t = call.topic.lowercase()
  val s = call.summary.lowercase()

  return when {
    call.status == "ESCALATED" || call.isEscalated || call.urgency == "URGENT_VIP" && (q.contains("urgent") || q.contains("timeout") || q.contains("down")) -> DashboardCategory.ESCALATED
    t.contains("wave_check") || t.contains("lead") || q.contains("wave check") || q.contains("diagnostic") || q.contains("audit") || s.contains("diagnostic") -> DashboardCategory.LEADS
    t.contains("purchase") || q.contains("wave starter") || q.contains("497") || q.contains("stripe") || q.contains("buy") || s.contains("stripe") -> DashboardCategory.PURCHASES
    t.contains("booking") || q.contains("calendly") || q.contains("schedule") || q.contains("meeting") || q.contains("consult") || s.contains("calendly") -> DashboardCategory.BOOKINGS
    t.contains("enterprise") || q.contains("big kahuna") || q.contains("tsunami") || q.contains("3997") || q.contains("enterprise") || s.contains("kahuna") -> DashboardCategory.ENTERPRISE
    t.contains("aeo") || q.contains("aeo") || q.contains("perplexity") || q.contains("answer engine") || s.contains("aeo") -> DashboardCategory.AEO
    t.contains("support") || t.contains("care") || q.contains("support") || q.contains("help") || q.contains("holiday") || s.contains("care cove") -> DashboardCategory.SUPPORT
    else -> DashboardCategory.ALL
  }
}

@Composable
fun SupabaseCallsDashboard(
  calls: List<CallEntity>,
  isFetchingFromSupabase: Boolean,
  lastSupabaseFetchTime: String,
  unsyncedCallCount: Int = 0,
  selectedStatusFilter: CallStatusFilter,
  onSelectStatusFilter: (CallStatusFilter) -> Unit,
  escalationTriggerConfig: EscalationTriggerConfig,
  onUpdateTriggerConfig: (Boolean, EscalationAlertChannel, String, String) -> Unit,
  onTriggerTestEscalation: () -> Unit,
  onFetchFromSupabase: () -> Unit,
  onMarkReviewed: (Long) -> Unit,
  onMarkPending: (Long) -> Unit,
  onTriggerEscalation: (CallEntity) -> Unit,
  onDeleteCall: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedCategory by remember { mutableStateOf(DashboardCategory.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var inspectingCallForSupabaseJson by remember { mutableStateOf<CallEntity?>(null) }
  var showTriggerSettings by remember { mutableStateOf(false) }

  // Status filter counts
  val statusCounts = remember(calls) {
    CallStatusFilter.values().associateWith { statusFilter ->
      when (statusFilter) {
        CallStatusFilter.ALL -> calls.size
        CallStatusFilter.PENDING -> calls.count { it.status == "PENDING_REVIEW" }
        CallStatusFilter.ESCALATED -> calls.count { it.status == "ESCALATED" || it.isEscalated }
        CallStatusFilter.REVIEWED -> calls.count { it.status == "REVIEWED" }
        CallStatusFilter.URGENT_VIP -> calls.count { it.urgency == "URGENT_VIP" }
      }
    }
  }

  // Categorize counts
  val categoryCounts = remember(calls) {
    DashboardCategory.values().associateWith { cat ->
      if (cat == DashboardCategory.ALL) calls.size
      else calls.count { categorizeCall(it) == cat }
    }
  }

  // Filtered by Status, Category, and Search
  val filteredCalls = remember(calls, selectedStatusFilter, selectedCategory, searchQuery) {
    calls.filter { call ->
      val matchesStatus = when (selectedStatusFilter) {
        CallStatusFilter.ALL -> true
        CallStatusFilter.PENDING -> call.status == "PENDING_REVIEW"
        CallStatusFilter.ESCALATED -> call.status == "ESCALATED" || call.isEscalated
        CallStatusFilter.REVIEWED -> call.status == "REVIEWED"
        CallStatusFilter.URGENT_VIP -> call.urgency == "URGENT_VIP"
      }

      val matchesCategory = when (selectedCategory) {
        DashboardCategory.ALL -> true
        DashboardCategory.ESCALATED -> call.status == "ESCALATED" || call.isEscalated
        else -> categorizeCall(call) == selectedCategory
      }

      val matchesSearch = if (searchQuery.isBlank()) true else {
        val query = searchQuery.trim().lowercase()
        call.callerName.lowercase().contains(query) ||
        call.callerNumber.lowercase().contains(query) ||
        call.customerQuestion.lowercase().contains(query) ||
        call.summary.lowercase().contains(query) ||
        call.aiResponse.lowercase().contains(query)
      }

      matchesStatus && matchesCategory && matchesSearch
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // Top Bar: Supabase Sync Status and Action
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
      border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("supabase_sync_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = SeaGreen.copy(alpha = 0.2f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Storage,
              contentDescription = "Supabase DB",
              tint = SeaGreen,
              modifier = Modifier.size(24.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Supabase Call Logs",
              fontWeight = FontWeight.Bold,
              color = SandTextPrimary,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = SeaGreen.copy(alpha = 0.25f)
            ) {
              Text(
                text = "public.calls",
                color = SeaGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Connected: kbgujyuxdopx.supabase.co • $lastSupabaseFetchTime",
            color = SandTextSecondary,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (unsyncedCallCount > 0) SunsetOrange.copy(alpha = 0.22f) else SeaGreen.copy(alpha = 0.18f),
            modifier = Modifier.testTag("supabase_retry_queue_badge")
          ) {
            Text(
              text = if (unsyncedCallCount > 0) {
                "$unsyncedCallCount queued for background retry"
              } else {
                "All synced with WorkManager retry guard"
              },
              color = if (unsyncedCallCount > 0) GoldenSun else SeaGreen,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Button(
          onClick = onFetchFromSupabase,
          enabled = !isFetchingFromSupabase,
          colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("fetch_supabase_button")
        ) {
          if (isFetchingFromSupabase) {
            CircularProgressIndicator(
              color = Color.White,
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp
            )
          } else {
            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Fetch", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // AI Escalation Alert Trigger Quick Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
      border = BorderStroke(1.dp, if (escalationTriggerConfig.isEnabled) CoralAlert.copy(alpha = 0.5f) else SurfTeal.copy(alpha = 0.3f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .testTag("dashboard_escalation_trigger_card")
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = if (escalationTriggerConfig.isEnabled) CoralAlert else SandTextSecondary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "AI Escalation Alert Trigger: ${if (escalationTriggerConfig.isEnabled) escalationTriggerConfig.alertChannel.label else "Paused"}",
              color = SandTextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Destination: ${escalationTriggerConfig.targetEmail}",
              color = SandTextSecondary,
              fontSize = 10.sp
            )
          }
          TextButton(onClick = { showTriggerSettings = !showTriggerSettings }) {
            Text(
              text = if (showTriggerSettings) "Hide" else "Configure",
              color = SurfTurquoise,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        AnimatedVisibility(visible = showTriggerSettings) {
          var emailDraft by remember(escalationTriggerConfig.targetEmail) {
            mutableStateOf(escalationTriggerConfig.targetEmail)
          }
          var webhookDraft by remember(escalationTriggerConfig.webhookUrl) {
            mutableStateOf(escalationTriggerConfig.webhookUrl)
          }
          Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Auto-alert when AI flags call",
                color = SandTextPrimary,
                fontSize = 12.sp
              )
              Switch(
                checked = escalationTriggerConfig.isEnabled,
                onCheckedChange = { enabled ->
                  onUpdateTriggerConfig(enabled, escalationTriggerConfig.alertChannel, emailDraft, webhookDraft)
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = SeaGreen
                )
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              EscalationAlertChannel.values().forEach { channel ->
                FilterChip(
                  selected = escalationTriggerConfig.alertChannel == channel,
                  onClick = {
                    onUpdateTriggerConfig(escalationTriggerConfig.isEnabled, channel, emailDraft, webhookDraft)
                  },
                  label = { Text(channel.label, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CoralAlert,
                    selectedLabelColor = Color.White,
                    containerColor = OceanCard,
                    labelColor = SandTextPrimary
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = emailDraft,
                onValueChange = { emailDraft = it },
                label = { Text("Alert Email", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SurfTurquoise,
                  unfocusedBorderColor = SurfTeal.copy(alpha = 0.5f),
                  focusedTextColor = SandTextPrimary,
                  unfocusedTextColor = SandTextPrimary
                )
              )
              Button(
                onClick = {
                  onUpdateTriggerConfig(
                    escalationTriggerConfig.isEnabled,
                    escalationTriggerConfig.alertChannel,
                    emailDraft,
                    webhookDraft
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Save", fontSize = 11.sp, color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = webhookDraft,
                onValueChange = { webhookDraft = it },
                label = { Text("Webhook / Slack URL", fontSize = 11.sp) },
                placeholder = { Text("https://hooks.slack.com/...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("dashboard_escalation_webhook_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SurfTurquoise,
                  unfocusedBorderColor = SurfTeal.copy(alpha = 0.5f),
                  focusedTextColor = SandTextPrimary,
                  unfocusedTextColor = SandTextPrimary
                )
              )
              Button(
                onClick = {
                  onUpdateTriggerConfig(
                    escalationTriggerConfig.isEnabled,
                    escalationTriggerConfig.alertChannel,
                    emailDraft,
                    webhookDraft
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Save", fontSize = 11.sp, color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = onTriggerTestEscalation,
              colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_test_escalation_button")
            ) {
              Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Test AI Escalation Trigger Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // KPI Metrics Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      KpiMetricCard(
        title = "Total Calls",
        count = calls.size.toString(),
        tint = SurfCyan,
        modifier = Modifier.weight(1f)
      )
      KpiMetricCard(
        title = "Pending",
        count = (statusCounts[CallStatusFilter.PENDING] ?: 0).toString(),
        tint = SunsetOrange,
        modifier = Modifier.weight(1f)
      )
      KpiMetricCard(
        title = "Leads",
        count = (categoryCounts[DashboardCategory.LEADS] ?: 0).toString(),
        tint = SeaGreen,
        modifier = Modifier.weight(1f)
      )
      KpiMetricCard(
        title = "Escalated",
        count = (statusCounts[CallStatusFilter.ESCALATED] ?: 0).toString(),
        tint = CoralAlert,
        modifier = Modifier.weight(1f)
      )
    }

    // Search Input
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search caller, phone, inquiry or summary...", color = SandTextSecondary, fontSize = 12.sp) },
      leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SurfCyan, modifier = Modifier.size(18.dp)) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = SandTextSecondary, modifier = Modifier.size(18.dp))
          }
        }
      },
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SurfTurquoise,
        unfocusedBorderColor = SurfTeal.copy(alpha = 0.5f),
        focusedTextColor = SandTextPrimary,
        unfocusedTextColor = SandTextPrimary,
        focusedContainerColor = OceanDeepBlue,
        unfocusedContainerColor = OceanDeepBlue
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("call_search_input")
    )

    // Status Filters (Horizontal Scrollable)
    Text(
      text = "STATUS FILTERS",
      color = SurfCyan,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.sp,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      CallStatusFilter.values().forEach { statusFilter ->
        val count = statusCounts[statusFilter] ?: 0
        val isSelected = selectedStatusFilter == statusFilter
        FilterChip(
          selected = isSelected,
          onClick = { onSelectStatusFilter(statusFilter) },
          leadingIcon = {
            Icon(
              imageVector = statusFilter.icon,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = when {
                isSelected && statusFilter == CallStatusFilter.ALL -> OceanAbyss
                isSelected && statusFilter == CallStatusFilter.REVIEWED -> OceanAbyss
                isSelected -> Color.White
                statusFilter == CallStatusFilter.ESCALATED -> CoralAlert
                statusFilter == CallStatusFilter.PENDING -> SunsetOrange
                statusFilter == CallStatusFilter.REVIEWED -> SeaGreen
                statusFilter == CallStatusFilter.URGENT_VIP -> GoldenSun
                else -> SurfCyan
              }
            )
          },
          label = {
            Text(
              text = "${statusFilter.label} ($count)",
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = when (statusFilter) {
              CallStatusFilter.ESCALATED -> CoralAlert
              CallStatusFilter.PENDING -> SunsetOrange
              CallStatusFilter.REVIEWED -> SeaGreen
              CallStatusFilter.URGENT_VIP -> GoldenSun
              CallStatusFilter.ALL -> SurfTurquoise
            },
            selectedLabelColor = if (statusFilter == CallStatusFilter.ALL || statusFilter == CallStatusFilter.REVIEWED || statusFilter == CallStatusFilter.URGENT_VIP) OceanAbyss else Color.White,
            containerColor = OceanDeepBlue,
            labelColor = SandTextPrimary
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else SurfTeal.copy(alpha = 0.45f)
          ),
          modifier = Modifier.testTag("status_filter_chip_${statusFilter.id.lowercase()}")
        )
      }
    }

    // Category Filter Chips (Horizontal Scrollable)
    Text(
      text = "TOPIC CATEGORIES",
      color = SandTextSecondary,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.sp,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
    )
    val horizontalScroll = rememberScrollState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(horizontalScroll)
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      DashboardCategory.values().forEach { category ->
        val count = categoryCounts[category] ?: 0
        FilterChip(
          selected = selectedCategory == category,
          onClick = { selectedCategory = category },
          label = {
            Text(
              text = "${category.label} ($count)",
              fontSize = 12.sp,
              fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = when (category) {
              DashboardCategory.ESCALATED -> CoralAlert
              DashboardCategory.LEADS -> SeaGreen
              DashboardCategory.PURCHASES -> GoldenSun
              DashboardCategory.ENTERPRISE -> SunsetOrange
              else -> SurfTurquoise
            },
            selectedLabelColor = if (category == DashboardCategory.PURCHASES) OceanAbyss else if (category == DashboardCategory.ALL) OceanAbyss else Color.White,
            containerColor = OceanCard,
            labelColor = SandTextPrimary
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (selectedCategory == category) Color.Transparent else SurfTeal.copy(alpha = 0.4f)
          ),
          modifier = Modifier.testTag("filter_chip_${category.id.lowercase()}")
        )
      }
    }

    // Call Log Items List
    if (filteredCalls.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            tint = SandTextSecondary,
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No call logs in '${selectedCategory.label}'",
            color = SandTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = if (searchQuery.isNotEmpty()) "Try adjusting your search query." else "Tap 'Fetch' to sync latest logs from Supabase.",
            color = SandTextSecondary,
            fontSize = 12.sp
          )
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        filteredCalls.forEach { call ->
          StaffCallDetailCard(
            call = call,
            targetAlertEmail = escalationTriggerConfig.targetEmail,
            onMarkReviewed = { onMarkReviewed(call.id) },
            onMarkPending = { onMarkPending(call.id) },
            onTriggerEscalation = { onTriggerEscalation(call) },
            onViewSupabaseJson = { inspectingCallForSupabaseJson = call },
            onDelete = { onDeleteCall(call.id) }
          )
        }
      }
    }
  }

  // Supabase JSON Inspection Dialog
  inspectingCallForSupabaseJson?.let { call ->
    val jsonPayload = remember(call) { SupabaseCallFetcher.formatCallAsSupabaseJson(call) }
    AlertDialog(
      onDismissRequest = { inspectingCallForSupabaseJson = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = SurfCyan)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Supabase Record Payload", color = SandTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            text = "Table: public.calls • ID: ${call.id}",
            color = SeaGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = OceanAbyss,
            border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp)
          ) {
            val scroll = rememberScrollState()
            Text(
              text = jsonPayload,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = SandTextPrimary,
              modifier = Modifier
                .padding(10.dp)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(scroll)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Supabase Call Record", jsonPayload))
            Toast.makeText(context, "Copied Supabase JSON to clipboard", Toast.LENGTH_SHORT).show()
            inspectingCallForSupabaseJson = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SurfTeal)
        ) {
          Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Copy JSON", fontSize = 12.sp)
        }
      },
      dismissButton = {
        TextButton(onClick = { inspectingCallForSupabaseJson = null }) {
          Text("Close", color = SandTextSecondary)
        }
      },
      containerColor = OceanDeepBlue
    )
  }
}

@Composable
fun KpiMetricCard(
  title: String,
  count: String,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count,
        color = tint,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        color = SandTextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
fun StaffCallDetailCard(
  call: CallEntity,
  targetAlertEmail: String = CallEscalationManager.TARGET_CREW_EMAIL,
  onMarkReviewed: () -> Unit,
  onMarkPending: () -> Unit,
  onTriggerEscalation: () -> Unit,
  onViewSupabaseJson: () -> Unit,
  onDelete: () -> Unit
) {
  val context = LocalContext.current
  var isExpanded by remember { mutableStateOf(false) }
  val category = remember(call) { categorizeCall(call) }

  val isPending = call.status == "PENDING_REVIEW"
  val isEscalated = call.status == "ESCALATED" || call.isEscalated

  val cardBorder = when {
    isEscalated -> CoralAlert
    isPending -> SunsetOrange
    else -> SurfTeal.copy(alpha = 0.4f)
  }

  val categoryPillColor = when (category) {
    DashboardCategory.LEADS -> SeaGreen
    DashboardCategory.PURCHASES -> GoldenSun
    DashboardCategory.BOOKINGS -> SurfTeal
    DashboardCategory.ENTERPRISE -> SunsetOrange
    DashboardCategory.AEO -> SurfTurquoise
    DashboardCategory.SUPPORT -> SeaGreen
    DashboardCategory.ESCALATED -> CoralAlert
    else -> SurfCyan
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
    border = BorderStroke(1.dp, cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("staff_call_card_${call.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Top Row: Caller Details & Status Badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = if (isEscalated) CoralAlert.copy(alpha = 0.2f) else SurfTeal.copy(alpha = 0.2f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isEscalated) Icons.Default.PriorityHigh else Icons.Default.PhoneInTalk,
              contentDescription = null,
              tint = if (isEscalated) CoralAlert else SurfCyan,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = call.callerName,
              color = SandTextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
          Text(
            text = "${call.callerNumber} • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.timestamp))} (${call.durationSeconds}s)",
            color = SandTextSecondary,
            fontSize = 11.sp
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          UrgencyBadge(urgency = call.urgency)
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = categoryPillColor.copy(alpha = 0.2f)
          ) {
            Text(
              text = category.label.take(16),
              color = categoryPillColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Customer Question / Transcript Snippet
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = OceanAbyss,
        border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = "Caller Question:",
            color = SurfCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "\"${call.customerQuestion}\"",
            color = SandTextPrimary,
            fontSize = 12.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // AI-Generated Call Summary
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = OceanCard,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Surfing,
              contentDescription = null,
              tint = GoldenSun,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Kai AI Call Summary",
              color = GoldenSun,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("AI Call Summary", call.summary))
                Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = SandTextSecondary, modifier = Modifier.size(14.dp))
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = call.summary,
            color = SandTextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      }

      // Expandable section for Full AI Response, Supabase Record, and Notes
      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = OceanSurface,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Full AI Surfer Response Delivered to Caller:",
                color = SurfTurquoise,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = call.aiResponse,
                color = SandTextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }

          if (call.escalationNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CoralAlert.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, CoralAlert),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "⚠️ Escalation Note: ${call.escalationNotes}",
                color = SandTextPrimary,
                fontSize = 11.sp,
                modifier = Modifier.padding(8.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Status: ${call.status}",
              color = SandTextSecondary,
              fontSize = 11.sp
            )
            TextButton(onClick = onDelete) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Call Log",
                tint = CoralAlert,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Remove Log", color = CoralAlert, fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Action Toolbar for Staff
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Mark as Reviewed / Return to Pending
        if (isPending || isEscalated) {
          Button(
            onClick = onMarkReviewed,
            colors = ButtonDefaults.buttonColors(containerColor = SeaGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Review", color = OceanAbyss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          OutlinedButton(
            onClick = onMarkPending,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SeaGreen),
            modifier = Modifier.weight(1f)
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SeaGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Reviewed", color = SeaGreen, fontSize = 11.sp)
          }
        }

        // Trigger Escalation Alert (Push + Email)
        Button(
          onClick = onTriggerEscalation,
          colors = ButtonDefaults.buttonColors(containerColor = if (isEscalated) CoralAlert.copy(alpha = 0.85f) else SunsetOrange),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("escalate_call_button_${call.id}")
        ) {
          Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(if (isEscalated) "Re-Alert" else "Alert", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Call Back
        Button(
          onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${call.callerNumber}"))
            try { context.startActivity(intent) } catch (_: Exception) {}
          },
          colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(0.85f)
        ) {
          Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("Call", color = Color.White, fontSize = 11.sp)
        }

        // Email Lead or Crew
        IconButton(
          onClick = {
            val intent = CallEscalationManager.createEmailEscalationIntent(call, targetAlertEmail)
            try {
              context.startActivity(Intent.createChooser(intent, "Send Call Escalation Email"))
            } catch (_: Exception) {}
          },
          modifier = Modifier.size(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Email, contentDescription = "Email Escalation Alert", tint = SurfCyan, modifier = Modifier.size(18.dp))
        }

        // Supabase JSON payload
        IconButton(
          onClick = onViewSupabaseJson,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Code, contentDescription = "Supabase JSON", tint = SurfCyan, modifier = Modifier.size(18.dp))
        }

        // Expand / Collapse details
        IconButton(
          onClick = { isExpanded = !isExpanded },
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Less" else "More",
            tint = SandTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
