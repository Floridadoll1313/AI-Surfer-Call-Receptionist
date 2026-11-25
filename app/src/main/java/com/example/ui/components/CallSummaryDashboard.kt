package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CallEntity
import com.example.integrations.BusinessLineId
import com.example.integrations.BusinessPhonesConfig
import com.example.ui.OceanSurferViewModel
import com.example.ui.theme.OceanTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * Room-backed CallSummaryDashboard connected via [OceanSurferViewModel] and its Room Flow.
 */
@Composable
fun CallSummaryDashboard(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier,
  maxItems: Int = 25,
  onCallSelected: (CallEntity) -> Unit = {}
) {
  val latestCalls by viewModel.latestCalls.collectAsStateWithLifecycle()
  val isFetching by viewModel.isFetchingFromSupabase.collectAsStateWithLifecycle()
  val lastSyncTime by viewModel.lastSupabaseFetchTime.collectAsStateWithLifecycle()
  val searchQuery by viewModel.callSearchQuery.collectAsStateWithLifecycle()
  val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
  val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
  val phonesConfig by viewModel.businessPhonesConfig.collectAsStateWithLifecycle()
  val selectedPhoneLineFilter by viewModel.selectedPhoneLineFilter.collectAsStateWithLifecycle()

  val displayCalls = remember(latestCalls, maxItems) {
    latestCalls.take(maxItems)
  }

  CallSummaryDashboard(
    calls = displayCalls,
    modifier = modifier,
    isSyncing = isFetching,
    lastSyncedLabel = lastSyncTime,
    searchQuery = searchQuery,
    onSearchQueryChange = { query -> viewModel.setCallSearchQuery(query) },
    selectedDateFilter = selectedDateFilter,
    onSelectDateFilter = { filter -> viewModel.setDateFilter(filter) },
    customStartDateUtcMillis = customDateRange.first,
    customEndDateUtcMillis = customDateRange.second,
    onSelectCustomDateRange = { start, end -> viewModel.setCustomDateRange(start, end) },
    businessPhonesConfig = phonesConfig,
    selectedPhoneLineFilter = selectedPhoneLineFilter,
    onSelectPhoneLineFilter = { line -> viewModel.setPhoneLineFilter(line) },
    onSyncLatest = { viewModel.fetchCallsFromSupabase() },
    onMarkReviewed = { callId -> viewModel.markCallReviewed(callId) },
    onTriggerEscalation = { call -> viewModel.triggerManualEscalation(call) },
    onCallSelected = onCallSelected
  )
}

/**
 * Stateless CallSummaryDashboard rendering latest Room call logs over an immersive wave background
 * with Material 3 pull-to-refresh support, call search, date range filtering, and 2-phone line filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSummaryDashboard(
  calls: List<CallEntity>,
  modifier: Modifier = Modifier,
  isSyncing: Boolean = false,
  lastSyncedLabel: String = "Synced with Supabase",
  searchQuery: String? = null,
  onSearchQueryChange: ((String) -> Unit)? = null,
  selectedDateFilter: CallDateFilter? = null,
  onSelectDateFilter: ((CallDateFilter) -> Unit)? = null,
  customStartDateUtcMillis: Long? = null,
  customEndDateUtcMillis: Long? = null,
  onSelectCustomDateRange: ((Long?, Long?) -> Unit)? = null,
  businessPhonesConfig: BusinessPhonesConfig = BusinessPhonesConfig(),
  selectedPhoneLineFilter: BusinessLineId? = null,
  onSelectPhoneLineFilter: ((BusinessLineId) -> Unit)? = null,
  onSyncLatest: () -> Unit = {},
  onMarkReviewed: (Long) -> Unit = {},
  onTriggerEscalation: (CallEntity) -> Unit = {},
  onCallSelected: (CallEntity) -> Unit = {}
) {
  val colors = OceanTheme.colors
  val pullToRefreshState = rememberPullToRefreshState()

  var localSearchQuery by rememberSaveable { mutableStateOf("") }
  var localDateFilter by rememberSaveable { mutableStateOf(CallDateFilter.ALL) }
  var localCustomStart by rememberSaveable { mutableStateOf<Long?>(null) }
  var localCustomEnd by rememberSaveable { mutableStateOf<Long?>(null) }
  var localPhoneLineFilter by rememberSaveable { mutableStateOf(BusinessLineId.ALL) }

  val activeSearchQuery = searchQuery ?: localSearchQuery
  val activeDateFilter = selectedDateFilter ?: localDateFilter
  val activeCustomStart = if (onSelectCustomDateRange != null) customStartDateUtcMillis else localCustomStart
  val activeCustomEnd = if (onSelectCustomDateRange != null) customEndDateUtcMillis else localCustomEnd
  val activePhoneLineFilter = selectedPhoneLineFilter ?: localPhoneLineFilter

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
  val handlePhoneLineFilterChange: (BusinessLineId) -> Unit = { updated ->
    if (onSelectPhoneLineFilter != null) onSelectPhoneLineFilter(updated) else localPhoneLineFilter = updated
  }

  val filteredCalls = remember(
    calls,
    activeSearchQuery,
    activeDateFilter,
    activeCustomStart,
    activeCustomEnd,
    activePhoneLineFilter
  ) {
    calls.filter { call ->
      CallFilterMatcher.matchesSearch(call, activeSearchQuery) &&
        CallFilterMatcher.matchesDateFilter(
          timestampMillis = call.timestamp,
          filter = activeDateFilter,
          customStartUtcMillis = activeCustomStart,
          customEndUtcMillis = activeCustomEnd
        ) &&
        CallFilterMatcher.matchesPhoneLine(call, activePhoneLineFilter)
    }
  }

  val pendingCount = remember(filteredCalls) { filteredCalls.count { it.status == "PENDING_REVIEW" } }
  val escalatedCount = remember(filteredCalls) { filteredCalls.count { it.status == "ESCALATED" || it.isEscalated } }
  val avgDurationSeconds = remember(filteredCalls) {
    if (filteredCalls.isEmpty()) 0 else filteredCalls.sumOf { it.durationSeconds } / filteredCalls.size
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("call_summary_dashboard")
  ) {
    ImmersiveWaveBackground(modifier = Modifier.fillMaxSize())

    PullToRefreshBox(
      isRefreshing = isSyncing,
      onRefresh = onSyncLatest,
      state = pullToRefreshState,
      indicator = {
        PullToRefreshDefaults.Indicator(
          state = pullToRefreshState,
          isRefreshing = isSyncing,
          containerColor = colors.deepBlue,
          color = colors.turquoise,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .testTag("call_summary_pull_indicator")
        )
      },
      modifier = Modifier
        .fillMaxSize()
        .testTag("call_summary_pull_to_refresh")
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .testTag("call_summary_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item(key = "wave_summary_header") {
            WaveSummaryHeaderCard(
              totalCount = filteredCalls.size,
              pendingCount = pendingCount,
              escalatedCount = escalatedCount,
              avgDurationSeconds = avgDurationSeconds,
              isSyncing = isSyncing,
              lastSyncedLabel = lastSyncedLabel,
              onSyncLatest = onSyncLatest
            )
          }

          item(key = "wave_search_and_date_filters") {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("call_summary_filter_card"),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CallSearchBar(
                query = activeSearchQuery,
                onQueryChange = handleSearchChange,
                searchFieldTag = "summary_call_search_input",
                clearButtonTag = "summary_clear_search_button",
                containerColor = colors.deepBlue.copy(alpha = 0.9f)
              )
              CallDateFilterBar(
                selectedFilter = activeDateFilter,
                customStartUtcMillis = activeCustomStart,
                customEndUtcMillis = activeCustomEnd,
                onSelectDateFilter = handleDateFilterChange,
                onSelectCustomRange = handleCustomRangeChange,
                chipTagPrefix = "summary_date_filter_chip_",
                containerColor = colors.deepBlue.copy(alpha = 0.9f),
                showHeader = false
              )
              CallPhoneLineFilterBar(
                selectedLine = activePhoneLineFilter,
                phonesConfig = businessPhonesConfig,
                calls = calls,
                onSelectLine = handlePhoneLineFilterChange,
                chipTagPrefix = "summary_phone_line_chip_",
                containerColor = colors.deepBlue.copy(alpha = 0.9f),
                showHeader = false
              )
            }
          }

          if (calls.isEmpty()) {
            item(key = "wave_summary_empty") {
              WaveEmptyCallsCard(
                isSyncing = isSyncing,
                onSyncLatest = onSyncLatest
              )
            }
          } else if (filteredCalls.isEmpty()) {
            item(key = "wave_summary_no_matches") {
              WaveNoFilterMatchesCard(
                searchQuery = activeSearchQuery,
                selectedDateFilter = activeDateFilter,
                onClearFilters = {
                  handleSearchChange("")
                  handleCustomRangeChange(null, null)
                  handleDateFilterChange(CallDateFilter.ALL)
                  handlePhoneLineFilterChange(BusinessLineId.ALL)
                }
              )
            }
          } else {
            items(
              items = filteredCalls,
              key = { call -> call.id }
            ) { call ->
              CallSummaryWaveItemCard(
                call = call,
                phonesConfig = businessPhonesConfig,
                onClick = { onCallSelected(call) },
                onMarkReviewed = { onMarkReviewed(call.id) },
                onTriggerEscalation = { onTriggerEscalation(call) }
              )
            }
          }

          item(key = "bottom_wave_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }
    }
  }
}

@Composable
fun ImmersiveWaveBackground(modifier: Modifier = Modifier) {
  val colors = OceanTheme.colors
  val infiniteTransition = rememberInfiniteTransition(label = "immersive_wave_bg")

  val primaryPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "primary_wave_phase"
  )

  val secondaryPhase by infiniteTransition.animateFloat(
    initialValue = (2f * PI).toFloat(),
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "secondary_wave_phase"
  )

  Box(
    modifier = modifier
      .background(
        Brush.verticalGradient(
          colors = listOf(
            colors.abyss,
            colors.deepBlue,
            colors.surface.copy(alpha = 0.92f),
            colors.abyss
          )
        )
      )
      .testTag("immersive_wave_background")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val width = size.width
      val height = size.height
      if (width <= 0f || height <= 0f) return@Canvas

      // Soft bioluminescent ocean glow behind the crest
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            colors.turquoise.copy(alpha = 0.16f),
            colors.cyan.copy(alpha = 0.06f),
            Color.Transparent
          ),
          center = Offset(width * 0.82f, height * 0.16f),
          radius = width * 0.65f
        ),
        center = Offset(width * 0.82f, height * 0.16f),
        radius = width * 0.65f
      )

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            colors.teal.copy(alpha = 0.18f),
            Color.Transparent
          ),
          center = Offset(width * 0.18f, height * 0.68f),
          radius = width * 0.55f
        ),
        center = Offset(width * 0.18f, height * 0.68f),
        radius = width * 0.55f
      )

      fun buildWavePath(
        baseY: Float,
        amplitude: Float,
        cycles: Float,
        phase: Float
      ): Path {
        val path = Path()
        path.moveTo(0f, baseY)
        val step = (width / 48f).coerceAtLeast(10f)
        var x = 0f
        while (x <= width + step) {
          val clampedX = x.coerceAtMost(width)
          val angle = (clampedX / width) * (cycles * 2f * PI.toFloat()) + phase
          val y = baseY + sin(angle) * amplitude
          path.lineTo(clampedX, y)
          x += step
        }
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        return path
      }

      fun buildCrestStroke(
        baseY: Float,
        amplitude: Float,
        cycles: Float,
        phase: Float
      ): Path {
        val path = Path()
        val step = (width / 48f).coerceAtLeast(10f)
        var x = 0f
        var first = true
        while (x <= width + step) {
          val clampedX = x.coerceAtMost(width)
          val angle = (clampedX / width) * (cycles * 2f * PI.toFloat()) + phase
          val y = baseY + sin(angle) * amplitude
          if (first) {
            path.moveTo(clampedX, y)
            first = false
          } else {
            path.lineTo(clampedX, y)
          }
          x += step
        }
        return path
      }

      // Far swell wave layer
      val backWaveY = height * 0.24f
      val backWave = buildWavePath(
        baseY = backWaveY,
        amplitude = height * 0.028f,
        cycles = 1.4f,
        phase = secondaryPhase
      )
      drawPath(
        path = backWave,
        brush = Brush.verticalGradient(
          colors = listOf(
            colors.teal.copy(alpha = 0.22f),
            colors.deepBlue.copy(alpha = 0.08f)
          ),
          startY = backWaveY - height * 0.03f,
          endY = height
        )
      )

      // Midground surf wave layer
      val midWaveY = height * 0.52f
      val midWave = buildWavePath(
        baseY = midWaveY,
        amplitude = height * 0.034f,
        cycles = 1.8f,
        phase = primaryPhase
      )
      drawPath(
        path = midWave,
        brush = Brush.verticalGradient(
          colors = listOf(
            colors.cyan.copy(alpha = 0.18f),
            colors.card.copy(alpha = 0.14f)
          ),
          startY = midWaveY - height * 0.04f,
          endY = height
        )
      )
      drawPath(
        path = buildCrestStroke(
          baseY = midWaveY,
          amplitude = height * 0.034f,
          cycles = 1.8f,
          phase = primaryPhase
        ),
        color = colors.waveFoam.copy(alpha = 0.18f),
        style = Stroke(width = 2.5f)
      )

      // Foreground tide swell layer
      val frontWaveY = height * 0.78f
      val frontWave = buildWavePath(
        baseY = frontWaveY,
        amplitude = height * 0.025f,
        cycles = 2.2f,
        phase = secondaryPhase + 1.2f
      )
      drawPath(
        path = frontWave,
        brush = Brush.verticalGradient(
          colors = listOf(
            colors.turquoise.copy(alpha = 0.20f),
            colors.abyss.copy(alpha = 0.55f)
          ),
          startY = frontWaveY - height * 0.03f,
          endY = height
        )
      )
      drawPath(
        path = buildCrestStroke(
          baseY = frontWaveY,
          amplitude = height * 0.025f,
          cycles = 2.2f,
          phase = secondaryPhase + 1.2f
        ),
        color = colors.turquoise.copy(alpha = 0.35f),
        style = Stroke(width = 2.dp.toPx())
      )
    }
  }
}

@Composable
private fun WaveSummaryHeaderCard(
  totalCount: Int,
  pendingCount: Int,
  escalatedCount: Int,
  avgDurationSeconds: Int,
  isSyncing: Boolean,
  lastSyncedLabel: String,
  onSyncLatest: () -> Unit
) {
  val colors = OceanTheme.colors

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue.copy(alpha = 0.88f)),
    border = BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("call_summary_header_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = colors.turquoise.copy(alpha = 0.2f),
          modifier = Modifier.size(44.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Waves,
              contentDescription = "Ocean Wave Summary",
              tint = colors.turquoise,
              modifier = Modifier.size(24.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Call Summary Dashboard",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Storage,
              contentDescription = null,
              tint = colors.seaGreen,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isSyncing) {
                "Re-syncing calls from Supabase..."
              } else {
                "$lastSyncedLabel • Pull down to sync"
              },
              color = colors.cyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("call_summary_sync_status")
            )
          }
        }

        Button(
          onClick = onSyncLatest,
          enabled = !isSyncing,
          colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag("call_summary_sync_button")
        ) {
          if (isSyncing) {
            CircularProgressIndicator(
              color = Color.White,
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp
            )
          } else {
            Icon(
              imageVector = Icons.Default.CloudDownload,
              contentDescription = "Sync Latest Calls",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        WaveMetricPill(
          label = "Latest",
          value = totalCount.toString(),
          accent = colors.turquoise,
          modifier = Modifier.weight(1f)
        )
        WaveMetricPill(
          label = "Pending",
          value = pendingCount.toString(),
          accent = colors.sunsetOrange,
          modifier = Modifier.weight(1f)
        )
        WaveMetricPill(
          label = "Escalated",
          value = escalatedCount.toString(),
          accent = colors.coralAlert,
          modifier = Modifier.weight(1f)
        )
        WaveMetricPill(
          label = "Avg Ride",
          value = "${avgDurationSeconds}s",
          accent = colors.seaGreen,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun WaveMetricPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  val colors = OceanTheme.colors
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = colors.abyss.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        color = accent,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = label,
        color = colors.textSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
private fun WaveEmptyCallsCard(
  isSyncing: Boolean,
  onSyncLatest: () -> Unit
) {
  val colors = OceanTheme.colors
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue.copy(alpha = 0.85f)),
    border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.45f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("call_summary_empty_state")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        shape = CircleShape,
        color = colors.cyan.copy(alpha = 0.16f),
        modifier = Modifier.size(64.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Waves,
            contentDescription = "No calls yet",
            tint = colors.cyan,
            modifier = Modifier.size(34.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Calm Waters — No Calls Logged",
        color = colors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Real incoming cell calls on Phone 1 or Phone 2, Client Wave Check intakes, and synced Supabase logs appear here automatically.",
        color = colors.textSecondary,
        fontSize = 12.sp
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onSyncLatest,
        enabled = !isSyncing,
        colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text(
          text = if (isSyncing) "Syncing..." else "Sync Recent Calls",
          color = colors.abyss,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun WaveNoFilterMatchesCard(
  searchQuery: String,
  selectedDateFilter: CallDateFilter,
  onClearFilters: () -> Unit
) {
  val colors = OceanTheme.colors
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue.copy(alpha = 0.88f)),
    border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.45f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("call_summary_no_matches_state")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "No Matching Calls Found",
        color = colors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      val activeHint = buildString {
        if (searchQuery.isNotBlank()) append("query \"${searchQuery.trim()}\"")
        if (selectedDateFilter != CallDateFilter.ALL) {
          if (isNotEmpty()) append(" and ")
          append("date filter (${selectedDateFilter.label})")
        }
      }.ifBlank { "active filters" }
      Text(
        text = "No calls match $activeHint.",
        color = colors.textSecondary,
        fontSize = 12.sp
      )
      Spacer(modifier = Modifier.height(14.dp))
      OutlinedButton(
        onClick = onClearFilters,
        border = BorderStroke(1.dp, colors.turquoise),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("clear_summary_filters_button")
      ) {
        Text(
          text = "Clear Search & Date Filter",
          color = colors.turquoise,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }
    }
  }
}

@Composable
fun CallSummaryWaveItemCard(
  call: CallEntity,
  onClick: () -> Unit,
  onMarkReviewed: () -> Unit,
  onTriggerEscalation: () -> Unit,
  modifier: Modifier = Modifier,
  phonesConfig: BusinessPhonesConfig = BusinessPhonesConfig()
) {
  val colors = OceanTheme.colors
  var isExpanded by remember { mutableStateOf(false) }

  val isEscalated = call.status == "ESCALATED" || call.isEscalated
  val isPending = call.status == "PENDING_REVIEW"

  val borderColor = when {
    isEscalated -> colors.coralAlert.copy(alpha = 0.8f)
    isPending -> colors.sunsetOrange.copy(alpha = 0.7f)
    else -> colors.turquoise.copy(alpha = 0.45f)
  }

  val formattedTime = remember(call.timestamp) {
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.timestamp))
  }

  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue.copy(alpha = 0.88f)),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("call_summary_item_${call.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = if (isEscalated) colors.coralAlert.copy(alpha = 0.2f) else colors.cyan.copy(alpha = 0.18f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.PhoneInTalk,
              contentDescription = "Caller ${call.callerName}",
              tint = if (isEscalated) colors.coralAlert else colors.turquoise,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = call.callerName,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = colors.textSecondary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${call.callerNumber} • $formattedTime • ${call.durationSeconds}s",
              color = colors.textSecondary,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
          UrgencyBadge(urgency = call.urgency)
          Spacer(modifier = Modifier.height(4.dp))
          CallLineBadge(receivingLine = call.receivingLine, phonesConfig = phonesConfig)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // AI Call Summary Highlight
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.card.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.goldenSun,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "AI Call Summary",
                color = colors.goldenSun,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = when {
                isEscalated -> colors.coralAlert.copy(alpha = 0.22f)
                isPending -> colors.sunsetOrange.copy(alpha = 0.22f)
                else -> colors.seaGreen.copy(alpha = 0.22f)
              }
            ) {
              Text(
                text = call.status.replace("_", " "),
                color = when {
                  isEscalated -> colors.coralAlert
                  isPending -> colors.goldenSun
                  else -> colors.seaGreen
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = call.summary.ifBlank { "No summary recorded yet." },
            color = colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.abyss.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Caller Asked:",
                color = colors.cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "\"${call.customerQuestion}\"",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "AI Surfer Reply:",
                color = colors.turquoise,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = call.aiResponse,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isPending || isEscalated) {
          Button(
            onClick = onMarkReviewed,
            colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("summary_review_button_${call.id}")
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mark Reviewed", color = colors.abyss, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          OutlinedButton(
            onClick = { isExpanded = !isExpanded },
            border = BorderStroke(1.dp, colors.seaGreen.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.seaGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reviewed", color = colors.seaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        Button(
          onClick = onTriggerEscalation,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isEscalated) colors.coralAlert else colors.sunsetOrange
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("summary_escalate_button_${call.id}")
        ) {
          Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isEscalated) "Re-Alert" else "Escalate",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(
          onClick = { isExpanded = !isExpanded },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("summary_expand_button_${call.id}")
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse call details" else "Expand call details",
            tint = colors.cyan
          )
        }
      }
    }
  }
}
