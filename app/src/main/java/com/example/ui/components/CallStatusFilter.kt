package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class CallStatusFilter(
  val id: String,
  val label: String,
  val icon: ImageVector,
  val badgeText: String
) {
  ALL("ALL", "All Calls", Icons.Default.AllInclusive, "ALL"),
  PENDING("PENDING", "Pending Review", Icons.Default.HourglassTop, "PENDING"),
  ESCALATED("ESCALATED", "Escalated Alerts", Icons.Default.Warning, "ALERTS"),
  REVIEWED("REVIEWED", "Reviewed", Icons.Default.CheckCircle, "RESOLVED"),
  URGENT_VIP("URGENT_VIP", "Urgent / VIP", Icons.Default.PriorityHigh, "VIP")
}
