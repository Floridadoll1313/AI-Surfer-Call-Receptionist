package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wave_audit_leads")
data class WaveAuditLeadEntity(
  @PrimaryKey
  val id: String,
  val businessName: String,
  val contactName: String,
  val contactEmail: String,
  val contactPhone: String,
  val biggestBottleneck: String,
  val recommendedProduct: String,
  val syncedToSupabase: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)
