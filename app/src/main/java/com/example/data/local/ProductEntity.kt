package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val price: Double,
  val category: String,
  val description: String,
  val inStock: Boolean = true,
  val popularBadge: String = "",
  val surfLevel: String = "All Levels",
  val ctaText: String = "",
  val directUrl: String = "",
  val slug: String = ""
)
