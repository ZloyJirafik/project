package com.example.caloscan.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Database tables (Room / SQLite):
 *
 *   foods          – foods the user created (source "custom") and products
 *                    scanned before (source "off"), so they work offline
 *   diary_entries  – everything logged, one row per food per meal
 *   recent_foods   – ids of the last foods added, for the "Recent" list
 *   profile        – one row with the user's details for the calorie calculator
 *
 * The 149 built-in foods are not in the database; they live in BuiltInFoods.kt.
 */

@Entity(tableName = "foods", indices = [Index("barcode"), Index("source")])
data class FoodEntity(
  @PrimaryKey val id: String,
  val name: String,
  val brand: String,
  val category: String,
  val kcal: Double,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
  val servingLabel: String?,
  val servingAmount: Double?,
  val unit: String,
  val emoji: String,
  val source: String,
  val barcode: String,
  val imageUrl: String,
  val packageSize: String,
  /** When it was created or scanned (milliseconds). Newest are shown first. */
  val savedAt: Long,
)

@Entity(tableName = "diary_entries", indices = [Index("date")])
data class DiaryEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  /** "YYYY-MM-DD", so dates sort correctly as text. */
  val date: String,
  val meal: String,
  /** Grams or ml. */
  val amount: Double,
  val addedAt: Long,
  @Embedded(prefix = "food_") val food: FoodSnapshot,
)

/** A copy of the food inside a diary entry (values per 100 g). */
data class FoodSnapshot(
  val id: String,
  val name: String,
  val brand: String,
  val kcal: Double,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
  val servingLabel: String?,
  val servingAmount: Double?,
  val unit: String,
  val emoji: String,
  val source: String,
  val barcode: String,
  val imageUrl: String,
)

@Entity(tableName = "recent_foods")
data class RecentFoodEntity(
  @PrimaryKey val foodId: String,
  val usedAt: Long,
)

@Entity(tableName = "profile")
data class ProfileEntity(
  @PrimaryKey val id: Int = 1,
  val sex: String?,
  val age: Int?,
  val heightCm: Double?,
  val weightKg: Double?,
  val activity: String,
  val goal: String,
  val macroSplit: String,
  val customTarget: Int?,
)
