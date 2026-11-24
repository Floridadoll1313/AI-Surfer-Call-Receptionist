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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.integrations.SupabaseClient
import com.example.integrations.SupabaseConfig
import com.example.telephony.CallEscalationManager
import com.example.ui.theme.OceanTheme
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
  supabaseProjectUrl: String = SupabaseConfig.defaultProjectUrl(),
  searchQuery: String? = null,
  onSearchQueryChange: ((String) -> Unit)? = null,
  selectedDateFilter: CallDateFilter? = null,
  onSelectDateFilter: ((CallDateFilter) -> Unit)? = null,
  customStartDateUtcMillis: Long? = null,
  customEndDateUtcMillis: Long? = null,
  onSelectCustomDateRange: ((Long?, Long?) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  var selectedCategory by remember { mutableStateOf(DashboardCategory.ALL) }
  var localSearchQuery by rememberSaveable { mutableStateOf("") }
  var localDateFilter by rememberSaveable { mutableStateOf(CallDateFilter.ALL) }
  var localCustomStart by rememberSaveable { mutableStateOf<Long?>(null) }
  var localCustomEnd by rememberSaveable { mutableStateOf<Long?>(null) }
  var inspectingCallForSupabaseJson by remember { mutableStateOf<CallEntity?>(null) }
  var showTriggerSettings by remember { mutableStateOf(false) }

  val activeSearchQuery = searchQuery ?: localSearchQuery
  val activeDateFilter = selectedDateFilter ?: localDateFilter
  val activeCustomStart = if (onSelectCustomDateRange != null) customStartDateUtcMillis else localCustomStart
  val activeCustomEnd = if (onSelectCustomDateRange != null) customEndDateUtcMillis else localCustomEnd

  val handleSearchChange: (String) -> Unit = { updated ->
    if (onSearchQueryChange != null) onSearchQueryChange(updated) else localSearchQuery = updated
  }
  val handleDateFilterChange: (CallDateFilter) -> Unit = { updated ->
    if (onSelectDateFilter != null) onSelectDateFilter(updated) else localDateFilter = updated
  }
  val handleCustomRangeChange: (Long?, Long?) -> Unit = { start, end ->
    if (onSelectCustomDateRange != null) {
      onSelectCustomDateRange(start, end)
    } else {
      localCustomStart = start
      localCustomEnd = end
    }
  }

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

  // Filtered by Status, Category, Date Range, and Search
  val filteredCalls = remember(
    calls,
    selectedStatusFilter,
    selectedCategory,
    activeSearchQuery,
    activeDateFilter,
    activeCustomStart,
    activeCustomEnd
  ) {
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

      val matchesSearch = CallFilterMatcher.matchesSearch(call, activeSearchQuery)
      val matchesDate = CallFilterMatcher.matchesDateFilter(
        timestampMillis = call.timestamp,
        filter = activeDateFilter,
        customStartUtcMillis = activeCustomStart,
        customEndUtcMillis = activeCustomEnd
      )

      matchesStatus && matchesCategory && matchesSearch && matchesDate
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // Top Bar: Supabase Sync Status and Action
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
      border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.5f)),
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
          color = colors.seaGreen.copy(alpha = 0.2f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Storage,
              contentDescription = "Supabase DB",
              tint = colors.seaGreen,
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
              color = colors.textPrimary,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = colors.seaGreen.copy(alpha = 0.25f)
            ) {
              Text(
                text = "public.calls",
                color = colors.seaGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Connected: ${SupabaseClient.extractHost(supabaseProjectUrl)} • $lastSupabaseFetchTime",
            color = colors.textSecondary,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (unsyncedCallCount > 0) colors.sunsetOrange.copy(alpha = 0.22f) else colors.seaGreen.copy(alpha = 0.18f),
            modifier = Modifier.testTag("supabase_retry_queue_badge")
          ) {
            Text(
              text = if (unsyncedCallCount > 0) {
                "$unsyncedCallCount queued for background retry"
              } else {
                "All synced with WorkManager retry guard"
              },
              color = if (unsyncedCallCount > 0) colors.goldenSun else colors.seaGreen,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Button(
          onClick = onFetchFromSupabase,
          enabled = !isFetchingFromSupabase,
          colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
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
      colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
      border = BorderStroke(1.dp, if (escalationTriggerConfig.isEnabled) colors.coralAlert.copy(alpha = 0.5f) else colors.teal.copy(alpha = 0.3f)),
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
            tint = if (escalationTriggerConfig.isEnabled) colors.coralAlert else colors.textSecondary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "AI Escalation Alert Trigger: ${if (escalationTriggerConfig.isEnabled) escalationTriggerConfig.alertChannel.label else "Paused"}",
              color = colors.textPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Destination: ${escalationTriggerConfig.targetEmail}",
              color = colors.textSecondary,
              fontSize = 10.sp
            )
          }
          TextButton(onClick = { showTriggerSettings = !showTriggerSettings }) {
            Text(
              text = if (showTriggerSettings) "Hide" else "Configure",
              color = colors.turquoise,
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
                color = colors.textPrimary,
                fontSize = 12.sp
              )
              Switch(
                checked = escalationTriggerConfig.isEnabled,
                onCheckedChange = { enabled ->
                  onUpdateTriggerConfig(enabled, escalationTriggerConfig.alertChannel, emailDraft, webhookDraft)
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = colors.seaGreen
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
                    selectedContainerColor = colors.coralAlert,
                    selectedLabelColor = Color.White,
                    containerColor = colors.card,
                    labelColor = colors.textPrimary
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
                  focusedBorderColor = colors.turquoise,
                  unfocusedBorderColor = colors.teal.copy(alpha = 0.5f),
                  focusedTextColor = colors.textPrimary,
                  unfocusedTextColor = colors.textPrimary
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
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
                  focusedBorderColor = colors.turquoise,
                  unfocusedBorderColor = colors.teal.copy(alpha = 0.5f),
                  focusedTextColor = colors.textPrimary,
                  unfocusedTextColor = colors.textPrimary
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Save", fontSize = 11.sp, color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = onTriggerTestEscalation,
              colors = ButtonDefaults.buttonColors(containerColor = colors.coralAlert),
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
        tint = colors.cyan,
        modifier = Modifier.weight(1f)
      )
      KpiMetricCard(
        title = "Pending",
        count = (statusCounts[CallStatusFilter.PENDING] ?: 0).toString(),
        tint = colors.sunsetOrange,
        modifier = Modifier.weight(1f)
      )
      KpiMetricCard(
        title = "Leads",
        count = (categoryCounts[DashboardCategory.LEADS] ?: 0).toString(),
        tint = colors.seaGreen,
        modifier = Modifier.weight(1f)
      )
      KpiMetricCard(
        title = "Escalated",
        count = (statusCounts[CallStatusFilter.ESCALATED] ?: 0).toString(),
        tint = colors.coralAlert,
        modifier = Modifier.weight(1f)
      )
    }

    // Search Input & Date Filter
    CallSearchBar(
      query = activeSearchQuery,
      onQueryChange = handleSearchChange,
      searchFieldTag = "call_search_input",
      clearButtonTag = "clear_call_search_button",
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )

    CallDateFilterBar(
      selectedFilter = activeDateFilter,
      customStartUtcMillis = activeCustomStart,
      customEndUtcMillis = activeCustomEnd,
      onSelectDateFilter = handleDateFilterChange,
      onSelectCustomRange = handleCustomRangeChange,
      chipTagPrefix = "date_filter_chip_",
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )

    // Status Filters (Horizontal Scrollable)
    Text(
      text = "STATUS FILTERS",
      color = colors.cyan,
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
                isSelected && statusFilter == CallStatusFilter.ALL -> colors.abyss
                isSelected && statusFilter == CallStatusFilter.REVIEWED -> colors.abyss
                isSelected -> Color.White
                statusFilter == CallStatusFilter.ESCALATED -> colors.coralAlert
                statusFilter == CallStatusFilter.PENDING -> colors.sunsetOrange
                statusFilter == CallStatusFilter.REVIEWED -> colors.seaGreen
                statusFilter == CallStatusFilter.URGENT_VIP -> colors.goldenSun
                else -> colors.cyan
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
              CallStatusFilter.ESCALATED -> colors.coralAlert
              CallStatusFilter.PENDING -> colors.sunsetOrange
              CallStatusFilter.REVIEWED -> colors.seaGreen
              CallStatusFilter.URGENT_VIP -> colors.goldenSun
              CallStatusFilter.ALL -> colors.turquoise
            },
            selectedLabelColor = if (statusFilter == CallStatusFilter.ALL || statusFilter == CallStatusFilter.REVIEWED || statusFilter == CallStatusFilter.URGENT_VIP) colors.abyss else Color.White,
            containerColor = colors.deepBlue,
            labelColor = colors.textPrimary
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else colors.teal.copy(alpha = 0.45f)
          ),
          modifier = Modifier.testTag("status_filter_chip_${statusFilter.id.lowercase()}")
        )
      }
    }

    // Category Filter Chips (Horizontal Scrollable)
    Text(
      text = "TOPIC CATEGORIES",
      color = colors.textSecondary,
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
              DashboardCategory.ESCALATED -> colors.coralAlert
              DashboardCategory.LEADS -> colors.seaGreen
              DashboardCategory.PURCHASES -> colors.goldenSun
              DashboardCategory.ENTERPRISE -> colors.sunsetOrange
              else -> colors.turquoise
            },
            selectedLabelColor = if (category == DashboardCategory.PURCHASES) colors.abyss else if (category == DashboardCategory.ALL) colors.abyss else Color.White,
            containerColor = colors.card,
            labelColor = colors.textPrimary
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (selectedCategory == category) Color.Transparent else colors.teal.copy(alpha = 0.4f)
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
            tint = colors.textSecondary,
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No call logs in '${selectedCategory.label}'",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = if (activeSearchQuery.isNotEmpty() || activeDateFilter != CallDateFilter.ALL) {
              "Try adjusting your search query or date filter."
            } else {
              "Tap 'Fetch' to sync latest logs from Supabase."
            },
            color = colors.textSecondary,
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
          Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = colors.cyan)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Supabase Record Payload", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            text = "Table: public.calls • ID: ${call.id}",
            color = colors.seaGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.abyss,
            border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp)
          ) {
            val scroll = rememberScrollState()
            Text(
              text = jsonPayload,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = colors.textPrimary,
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
          colors = ButtonDefaults.buttonColors(containerColor = colors.teal)
        ) {
          Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Copy JSON", fontSize = 12.sp)
        }
      },
      dismissButton = {
        TextButton(onClick = { inspectingCallForSupabaseJson = null }) {
          Text("Close", color = colors.textSecondary)
        }
      },
      containerColor = colors.deepBlue
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
  val colors = OceanTheme.colors
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
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
        color = colors.textSecondary,
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
  val colors = OceanTheme.colors
  var isExpanded by remember { mutableStateOf(false) }
  val category = remember(call) { categorizeCall(call) }

  val isPending = call.status == "PENDING_REVIEW"
  val isEscalated = call.status == "ESCALATED" || call.isEscalated

  val cardBorder = when {
    isEscalated -> colors.coralAlert
    isPending -> colors.sunsetOrange
    else -> colors.teal.copy(alpha = 0.4f)
  }

  val categoryPillColor = when (category) {
    DashboardCategory.LEADS -> colors.seaGreen
    DashboardCategory.PURCHASES -> colors.goldenSun
    DashboardCategory.BOOKINGS -> colors.teal
    DashboardCategory.ENTERPRISE -> colors.sunsetOrange
    DashboardCategory.AEO -> colors.turquoise
    DashboardCategory.SUPPORT -> colors.seaGreen
    DashboardCategory.ESCALATED -> colors.coralAlert
    else -> colors.cyan
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
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
          color = if (isEscalated) colors.coralAlert.copy(alpha = 0.2f) else colors.teal.copy(alpha = 0.2f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isEscalated) Icons.Default.PriorityHigh else Icons.Default.PhoneInTalk,
              contentDescription = null,
              tint = if (isEscalated) colors.coralAlert else colors.cyan,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = call.callerName,
              color = colors.textPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
          Text(
            text = "${call.callerNumber} • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.timestamp))} (${call.durationSeconds}s)",
            color = colors.textSecondary,
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
        color = colors.abyss,
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = "Caller Question:",
            color = colors.cyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "\"${call.customerQuestion}\"",
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // AI-Generated Call Summary
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.card,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Surfing,
              contentDescription = null,
              tint = colors.goldenSun,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Kai AI Call Summary",
              color = colors.goldenSun,
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
              Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = call.summary,
            color = colors.textPrimary,
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
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Full AI Surfer Response Delivered to Caller:",
                color = colors.turquoise,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = call.aiResponse,
                color = colors.textPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }

          if (call.escalationNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = colors.coralAlert.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, colors.coralAlert),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "⚠️ Escalation Note: ${call.escalationNotes}",
                color = colors.textPrimary,
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
              color = colors.textSecondary,
              fontSize = 11.sp
            )
            TextButton(onClick = onDelete) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Call Log",
                tint = colors.coralAlert,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Remove Log", color = colors.coralAlert, fontSize = 11.sp)
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
            colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Review", color = colors.abyss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          OutlinedButton(
            onClick = onMarkPending,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, colors.seaGreen),
            modifier = Modifier.weight(1f)
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.seaGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Reviewed", color = colors.seaGreen, fontSize = 11.sp)
          }
        }

        // Trigger Escalation Alert (Push + Email)
        Button(
          onClick = onTriggerEscalation,
          colors = ButtonDefaults.buttonColors(containerColor = if (isEscalated) colors.coralAlert.copy(alpha = 0.85f) else colors.sunsetOrange),
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
          colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
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
          Icon(imageVector = Icons.Default.Email, contentDescription = "Email Escalation Alert", tint = colors.cyan, modifier = Modifier.size(18.dp))
        }

        // Supabase JSON payload
        IconButton(
          onClick = onViewSupabaseJson,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Code, contentDescription = "Supabase JSON", tint = colors.cyan, modifier = Modifier.size(18.dp))
        }

        // Expand / Collapse details
        IconButton(
          onClick = { isExpanded = !isExpanded },
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Less" else "More",
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
