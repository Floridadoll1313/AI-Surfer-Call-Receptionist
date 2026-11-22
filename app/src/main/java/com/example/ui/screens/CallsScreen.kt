package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.OceanWaveHeader
import com.example.ui.components.SupabaseCallsDashboard
import com.example.ui.theme.OceanAbyss

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
  val escalationTriggerConfig by viewModel.escalationTriggerConfig.collectAsStateWithLifecycle()
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OceanAbyss)
      .verticalScroll(scrollState)
  ) {
    OceanWaveHeader(
      title = "AI Call Dashboard",
      subtitle = "Supabase Logs, Status Filters & Escalation Alerts"
    )

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
      onTriggerTestEscalation = { viewModel.triggerTestCallAlert() },
      onFetchFromSupabase = { viewModel.fetchCallsFromSupabase() },
      onMarkReviewed = { callId -> viewModel.markCallReviewed(callId) },
      onMarkPending = { callId -> viewModel.markCallPending(callId) },
      onTriggerEscalation = { call -> viewModel.triggerManualEscalation(call) },
      onDeleteCall = { callId -> viewModel.deleteCall(callId) },
      modifier = Modifier.padding(bottom = 24.dp)
    )
  }
}
