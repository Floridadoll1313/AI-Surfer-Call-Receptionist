package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_faq")
data class FaqEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val category: String,
  val question: String,
  val answer: String,
  val tags: String
)
