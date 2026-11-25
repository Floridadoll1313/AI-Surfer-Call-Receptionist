package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val callerNumber: String,
  val callerName: String,
  val timestamp: Long = System.currentTimeMillis(),
  val durationSeconds: Int = 45,
  val status: String = "PENDING_REVIEW", // PENDING_REVIEW, REVIEWED, ESCALATED
  val urgency: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT_VIP
  val topic: String = "GENERAL", // BUSINESS_INFO, PRODUCT_INQUIRY, LESSON_BOOKING, VIP_DROP
  val summary: String,
  val transcription: String,
  val customerQuestion: String,
  val aiResponse: String,
  val escalationAlertMinutes: Int = 10,
  val escalationDeadline: Long = System.currentTimeMillis() + (10 * 60 * 1000),
  val isEscalated: Boolean = false,
  val escalatedAt: Long = 0L,
  val escalationNotes: String = "",
  val supabaseSynced: Boolean = false,
  val receivingLine: String = "PHONE_1"
)
