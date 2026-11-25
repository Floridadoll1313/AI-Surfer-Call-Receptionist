package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallEntity
import com.example.integrations.BusinessLineId
import com.example.integrations.BusinessPhonesConfig
import com.example.ui.theme.OceanTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CallDateFilter(
  val id: String,
  val label: String,
  val icon: ImageVector
) {
  ALL("ALL", "All Dates", Icons.Default.DateRange),
  TODAY("TODAY", "Today", Icons.Default.Today),
  LAST_7_DAYS("LAST_7_DAYS", "Last 7 Days", Icons.Default.CalendarMonth),
  LAST_30_DAYS("LAST_30_DAYS", "Last 30 Days", Icons.Default.CalendarMonth),
  CUSTOM("CUSTOM", "Pick Dates", Icons.Default.DateRange)
}

object CallFilterMatcher {

  fun matchesSearch(call: CallEntity, query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val lower = trimmed.lowercase(Locale.getDefault())
    return call.callerName.lowercase(Locale.getDefault()).contains(lower) ||
      call.callerNumber.lowercase(Locale.getDefault()).contains(lower) ||
      call.customerQuestion.lowercase(Locale.getDefault()).contains(lower) ||
      call.summary.lowercase(Locale.getDefault()).contains(lower) ||
      call.aiResponse.lowercase(Locale.getDefault()).contains(lower) ||
      call.topic.lowercase(Locale.getDefault()).contains(lower) ||
      call.urgency.lowercase(Locale.getDefault()).contains(lower) ||
      call.status.lowercase(Locale.getDefault()).contains(lower) ||
      call.receivingLine.lowercase(Locale.getDefault()).contains(lower)
  }

  fun matchesPhoneLine(call: CallEntity, filter: BusinessLineId): Boolean {
    return when (filter) {
      BusinessLineId.ALL -> true
      BusinessLineId.PHONE_1 -> call.receivingLine.isBlank() || call.receivingLine.equals(BusinessLineId.PHONE_1.id, ignoreCase = true)
      BusinessLineId.PHONE_2 -> call.receivingLine.equals(BusinessLineId.PHONE_2.id, ignoreCase = true)
    }
  }

  fun matchesDateFilter(
    timestampMillis: Long,
    filter: CallDateFilter,
    customStartUtcMillis: Long? = null,
    customEndUtcMillis: Long? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): Boolean {
    return when (filter) {
      CallDateFilter.ALL -> true
      CallDateFilter.TODAY -> {
        val start = startOfLocalDay(nowMillis)
        val end = endOfLocalDay(nowMillis)
        timestampMillis in start..end
      }
      CallDateFilter.LAST_7_DAYS -> {
        val start = startOfLocalDayOffset(nowMillis, -6)
        val end = endOfLocalDay(nowMillis)
        timestampMillis in start..end
      }
      CallDateFilter.LAST_30_DAYS -> {
        val start = startOfLocalDayOffset(nowMillis, -29)
        val end = endOfLocalDay(nowMillis)
        timestampMillis in start..end
      }
      CallDateFilter.CUSTOM -> {
        if (customStartUtcMillis == null && customEndUtcMillis == null) {
          true
        } else {
          val startLocal = utcDateMillisToLocalStartOfDay(customStartUtcMillis ?: customEndUtcMillis!!)
          val endLocal = utcDateMillisToLocalEndOfDay(customEndUtcMillis ?: customStartUtcMillis!!)
          val minBound = minOf(startLocal, endLocal)
          val maxBound = maxOf(startLocal, endLocal)
          timestampMillis in minBound..maxBound
        }
      }
    }
  }

  fun formatCustomRangeLabel(startUtcMillis: Long?, endUtcMillis: Long?): String {
    if (startUtcMillis == null && endUtcMillis == null) return CallDateFilter.CUSTOM.label
    val fmt = SimpleDateFormat("MMM d", Locale.getDefault()).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val startStr = fmt.format(Date(startUtcMillis ?: endUtcMillis!!))
    val endStr = endUtcMillis?.let { fmt.format(Date(it)) }
    return if (endStr == null || endStr == startStr) {
      startStr
    } else {
      "$startStr – $endStr"
    }
  }

  private fun startOfLocalDay(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = epochMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  private fun endOfLocalDay(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = epochMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
  }

  private fun startOfLocalDayOffset(nowMillis: Long, daysOffset: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      add(Calendar.DAY_OF_YEAR, daysOffset)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  private fun utcDateMillisToLocalStartOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMillis
    }
    return Calendar.getInstance().apply {
      set(
        utcCal.get(Calendar.YEAR),
        utcCal.get(Calendar.MONTH),
        utcCal.get(Calendar.DAY_OF_MONTH),
        0,
        0,
        0
      )
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  private fun utcDateMillisToLocalEndOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMillis
    }
    return Calendar.getInstance().apply {
      set(
        utcCal.get(Calendar.YEAR),
        utcCal.get(Calendar.MONTH),
        utcCal.get(Calendar.DAY_OF_MONTH),
        23,
        59,
        59
      )
      set(Calendar.MILLISECOND, 999)
    }.timeInMillis
  }
}

@Composable
fun CallSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  searchFieldTag: String = "call_search_input",
  clearButtonTag: String = "clear_call_search_button",
  containerColor: Color = OceanTheme.colors.deepBlue
) {
  val colors = OceanTheme.colors
  var localQuery by remember { mutableStateOf(query) }
  var lastDispatchedQuery by remember { mutableStateOf(query) }

  LaunchedEffect(query) {
    if (query != lastDispatchedQuery) {
      localQuery = query
      lastDispatchedQuery = query
    }
  }

  OutlinedTextField(
    value = localQuery,
    onValueChange = { updated ->
      localQuery = updated
      lastDispatchedQuery = updated
      onQueryChange(updated)
    },
    placeholder = {
      Text(
        text = "Search caller, phone, topic, inquiry, or summary...",
        color = colors.textSecondary,
        fontSize = 12.sp,
        maxLines = 1
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search calls",
        tint = colors.cyan,
        modifier = Modifier.size(18.dp)
      )
    },
    trailingIcon = {
      if (localQuery.isNotEmpty()) {
        IconButton(
          onClick = {
            localQuery = ""
            lastDispatchedQuery = ""
            onQueryChange("")
          },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(clearButtonTag)
        ) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear search",
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    },
    singleLine = true,
    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
    shape = RoundedCornerShape(14.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = colors.turquoise,
      unfocusedBorderColor = colors.teal.copy(alpha = 0.5f),
      focusedTextColor = colors.textPrimary,
      unfocusedTextColor = colors.textPrimary,
      focusedContainerColor = containerColor,
      unfocusedContainerColor = containerColor
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag(searchFieldTag)
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDateFilterBar(
  selectedFilter: CallDateFilter,
  customStartUtcMillis: Long?,
  customEndUtcMillis: Long?,
  onSelectDateFilter: (CallDateFilter) -> Unit,
  onSelectCustomRange: (Long?, Long?) -> Unit,
  modifier: Modifier = Modifier,
  chipTagPrefix: String = "date_filter_chip_",
  containerColor: Color = OceanTheme.colors.deepBlue,
  showHeader: Boolean = true
) {
  val colors = OceanTheme.colors
  var showDateRangeDialog by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    if (showHeader || selectedFilter != CallDateFilter.ALL) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        if (showHeader) {
          Text(
            text = "DATE FILTER",
            color = colors.cyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
          )
        } else {
          Spacer(modifier = Modifier.width(1.dp))
        }
        if (selectedFilter != CallDateFilter.ALL) {
          TextButton(
            onClick = {
              onSelectCustomRange(null, null)
              onSelectDateFilter(CallDateFilter.ALL)
            },
            modifier = Modifier
              .defaultMinSize(minHeight = 32.dp)
              .testTag("${chipTagPrefix}reset")
          ) {
            Text(
              text = "Clear Date",
              color = colors.turquoise,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      CallDateFilter.entries.forEach { dateFilter ->
        val isSelected = selectedFilter == dateFilter
        val label = if (dateFilter == CallDateFilter.CUSTOM && isSelected) {
          CallFilterMatcher.formatCustomRangeLabel(customStartUtcMillis, customEndUtcMillis)
        } else {
          dateFilter.label
        }

        FilterChip(
          selected = isSelected,
          onClick = {
            if (dateFilter == CallDateFilter.CUSTOM) {
              showDateRangeDialog = true
            } else {
              onSelectDateFilter(dateFilter)
            }
          },
          leadingIcon = {
            Icon(
              imageVector = dateFilter.icon,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (isSelected) colors.abyss else colors.cyan
            )
          },
          label = {
            Text(
              text = label,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.turquoise,
            selectedLabelColor = colors.abyss,
            containerColor = containerColor,
            labelColor = colors.textPrimary
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else colors.teal.copy(alpha = 0.45f)
          ),
          modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .testTag("$chipTagPrefix${dateFilter.id.lowercase(Locale.US)}")
        )
      }
    }
  }

  if (showDateRangeDialog) {
    val pickerState = rememberDateRangePickerState(
      initialSelectedStartDateMillis = customStartUtcMillis,
      initialSelectedEndDateMillis = customEndUtcMillis
    )
    DatePickerDialog(
      onDismissRequest = { showDateRangeDialog = false },
      colors = DatePickerDefaults.colors(containerColor = colors.deepBlue),
      confirmButton = {
        Button(
          onClick = {
            val start = pickerState.selectedStartDateMillis
            val end = pickerState.selectedEndDateMillis ?: start
            if (start != null) {
              onSelectCustomRange(start, end)
              onSelectDateFilter(CallDateFilter.CUSTOM)
            }
            showDateRangeDialog = false
          },
          enabled = pickerState.selectedStartDateMillis != null,
          colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise)
        ) {
          Text("Apply Range", color = colors.abyss, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDateRangeDialog = false }) {
          Text("Cancel", color = colors.textSecondary)
        }
      }
    ) {
      DateRangePicker(
        state = pickerState,
        title = {
          Text(
            text = "Filter Calls by Date Range",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp)
          )
        },
        headline = {
          Text(
            text = CallFilterMatcher.formatCustomRangeLabel(
              pickerState.selectedStartDateMillis,
              pickerState.selectedEndDateMillis
            ),
            color = colors.turquoise,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
          )
        },
        showModeToggle = false,
        colors = DatePickerDefaults.colors(
          containerColor = colors.deepBlue,
          titleContentColor = colors.textPrimary,
          headlineContentColor = colors.turquoise,
          weekdayContentColor = colors.cyan,
          subheadContentColor = colors.textSecondary,
          yearContentColor = colors.textPrimary,
          currentYearContentColor = colors.turquoise,
          selectedYearContentColor = colors.abyss,
          selectedYearContainerColor = colors.turquoise,
          dayContentColor = colors.textPrimary,
          selectedDayContentColor = colors.abyss,
          selectedDayContainerColor = colors.turquoise,
          dayInSelectionRangeContentColor = colors.textPrimary,
          dayInSelectionRangeContainerColor = colors.teal.copy(alpha = 0.45f),
          todayContentColor = colors.turquoise,
          todayDateBorderColor = colors.turquoise
        ),
        modifier = Modifier.height(440.dp)
      )
    }
  }
}

@Composable
fun CallPhoneLineFilterBar(
  selectedLine: BusinessLineId,
  phonesConfig: BusinessPhonesConfig,
  calls: List<CallEntity>,
  onSelectLine: (BusinessLineId) -> Unit,
  modifier: Modifier = Modifier,
  chipTagPrefix: String = "phone_line_filter_chip_",
  containerColor: Color = OceanTheme.colors.deepBlue,
  showHeader: Boolean = true
) {
  val colors = OceanTheme.colors
  val counts = remember(calls) {
    mapOf(
      BusinessLineId.ALL to calls.size,
      BusinessLineId.PHONE_1 to calls.count { CallFilterMatcher.matchesPhoneLine(it, BusinessLineId.PHONE_1) },
      BusinessLineId.PHONE_2 to calls.count { CallFilterMatcher.matchesPhoneLine(it, BusinessLineId.PHONE_2) }
    )
  }

  Column(modifier = modifier.fillMaxWidth()) {
    if (showHeader) {
      Text(
        text = "BUSINESS PHONE LINE",
        color = colors.cyan,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 2.dp)
      )
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      BusinessLineId.entries.forEach { line ->
        val isSelected = selectedLine == line
        val count = counts[line] ?: 0
        val lineText = when (line) {
          BusinessLineId.ALL -> "All Phones ($count)"
          BusinessLineId.PHONE_1 -> "Phone 1: ${phonesConfig.phone1Label.take(14)} ($count)"
          BusinessLineId.PHONE_2 -> "Phone 2: ${phonesConfig.phone2Label.take(14)} ($count)"
        }
        FilterChip(
          selected = isSelected,
          onClick = { onSelectLine(line) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.PhoneAndroid,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (isSelected) colors.abyss else colors.cyan
            )
          },
          label = {
            Text(
              text = lineText,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.turquoise,
            selectedLabelColor = colors.abyss,
            containerColor = containerColor,
            labelColor = colors.textPrimary
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else colors.teal.copy(alpha = 0.45f)
          ),
          modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .testTag("$chipTagPrefix${line.id.lowercase(Locale.US)}")
        )
      }
    }
  }
}

@Composable
fun CallLineBadge(
  receivingLine: String,
  phonesConfig: BusinessPhonesConfig,
  modifier: Modifier = Modifier
) {
  val colors = OceanTheme.colors
  val isPhone2 = receivingLine.equals(BusinessLineId.PHONE_2.id, ignoreCase = true)
  val tint = if (isPhone2) colors.goldenSun else colors.cyan
  val label = if (isPhone2) {
    "Phone 2 • ${phonesConfig.phone2Label.take(14)}"
  } else {
    "Phone 1 • ${phonesConfig.phone1Label.take(14)}"
  }
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = tint.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.PhoneAndroid,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(10.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = label,
        color = tint,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
