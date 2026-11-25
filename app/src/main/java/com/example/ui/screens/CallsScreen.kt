package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.CallStatusFilter
import com.example.ui.components.CallSummaryDashboard
import com.example.ui.components.OceanWaveHeader
import com.example.ui.components.SupabaseCallsDashboard
import com.example.ui.theme.OceanTheme

@Composable
fun CallsScreen(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val allCalls by viewModel.calls.collectAsStateWithLifecycle()
  val isFetching by viewModel.isFetchingFromSupabase.collectAsStateWithLifecycle()
  val lastSyncTime by viewModel.lastSupabaseFetchTime.collectAsStateWithLifecycle()
  val unsyncedCount by viewModel.unsyncedCallCount.collectAsStateWithLifecycle()
  val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
  val callSearchQuery by viewModel.callSearchQuery.collectAsStateWithLifecycle()
  val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
  val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
  val businessPhonesConfig by viewModel.businessPhonesConfig.collectAsStateWithLifecycle()
  val selectedPhoneLineFilter by viewModel.selectedPhoneLineFilter.collectAsStateWithLifecycle()
  val escalationTriggerConfig by viewModel.escalationTriggerConfig.collectAsStateWithLifecycle()
  val supabaseConfig by viewModel.supabaseConfig.collectAsStateWithLifecycle()
  val scrollState = rememberScrollState()
  val colors = OceanTheme.colors

  var showDetailedFilters by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(selectedStatusFilter) {
    if (selectedStatusFilter != CallStatusFilter.ALL) {
      showDetailedFilters = true
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.abyss)
  ) {
    OceanWaveHeader(
      title = "AI Call Dashboard",
      subtitle = "2-Phone Real Call Summaries, Search & Alerts"
    )

    // Mode Switcher: Immersive Room Wave Summary vs. Search & Operations Filters
    Surface(
      color = colors.deepBlue,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val summarySelected = !showDetailedFilters
        OutlinedButton(
          onClick = { showDetailedFilters = false },
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (summarySelected) colors.turquoise else colors.card,
            contentColor = if (summarySelected) colors.abyss else colors.textPrimary
          ),
          border = BorderStroke(1.dp, if (summarySelected) Color.Transparent else colors.teal.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("mode_wave_summary_button")
        ) {
          Icon(
            imageVector = Icons.Default.Waves,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Wave Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        val filtersSelected = showDetailedFilters
        OutlinedButton(
          onClick = { showDetailedFilters = true },
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (filtersSelected) colors.turquoise else colors.card,
            contentColor = if (filtersSelected) colors.abyss else colors.textPrimary
          ),
          border = BorderStroke(1.dp, if (filtersSelected) Color.Transparent else colors.teal.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("mode_search_filters_button")
        ) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Search & Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      if (!showDetailedFilters) {
        CallSummaryDashboard(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize(),
          onCallSelected = { showDetailedFilters = true }
        )
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
        ) {
          SupabaseCallsDashboard(
            calls = allCalls,
            isFetchingFromSupabase = isFetching,
            lastSupabaseFetchTime = lastSyncTime,
            unsyncedCallCount = unsyncedCount,
            selectedStatusFilter = selectedStatusFilter,
            onSelectStatusFilter = { filter -> viewModel.setStatusFilter(filter) },
            escalationTriggerConfig = escalationTriggerConfig,
            onUpdateTriggerConfig = { enabled, channel, email, webhookUrl ->
              viewModel.updateEscalationTriggerConfig(
                isEnabled = enabled,
                alertChannel = channel,
                targetEmail = email,
                webhookUrl = webhookUrl
              )
            },
            onFetchFromSupabase = { viewModel.fetchCallsFromSupabase() },
            onMarkReviewed = { callId -> viewModel.markCallReviewed(callId) },
            onMarkPending = { callId -> viewModel.markCallPending(callId) },
            onTriggerEscalation = { call -> viewModel.triggerManualEscalation(call) },
            onDeleteCall = { callId -> viewModel.deleteCall(callId) },
            supabaseProjectUrl = supabaseConfig.projectUrl,
            searchQuery = callSearchQuery,
            onSearchQueryChange = { query -> viewModel.setCallSearchQuery(query) },
            selectedDateFilter = selectedDateFilter,
            onSelectDateFilter = { filter -> viewModel.setDateFilter(filter) },
            customStartDateUtcMillis = customDateRange.first,
            customEndDateUtcMillis = customDateRange.second,
            onSelectCustomDateRange = { start, end -> viewModel.setCustomDateRange(start, end) },
            businessPhonesConfig = businessPhonesConfig,
            selectedPhoneLineFilter = selectedPhoneLineFilter,
            onSelectPhoneLineFilter = { line -> viewModel.setPhoneLineFilter(line) },
            modifier = Modifier.padding(bottom = 24.dp)
          )
        }
      }
    }
  }
}
