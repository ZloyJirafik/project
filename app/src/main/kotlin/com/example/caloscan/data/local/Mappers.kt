package com.example.caloscan.data.local

import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.model.Serving
import com.example.caloscan.domain.ActivityLevel
import com.example.caloscan.domain.Goal
import com.example.caloscan.domain.MacroSplit
import com.example.caloscan.domain.Profile
import com.example.caloscan.domain.Sex
import java.time.LocalDate

/* Conversions between database rows and the app's own classes. */

private fun serving(label: String?, amount: Double?): Serving? =
  amount?.takeIf { it > 0 }?.let { Serving(label?.ifBlank { null } ?: "1 порция", it) }

fun FoodEntity.toFood() = Food(
  id = id,
  name = name,
  kcal = kcal,
  protein = protein,
  carbs = carbs,
  fat = fat,
  source = FoodSource.fromId(source),
  brand = brand,
  category = category,
  serving = serving(servingLabel, servingAmount),
  unit = unit,
  emoji = emoji,
  barcode = barcode,
  imageUrl = imageUrl,
  packageSize = packageSize,
)

fun Food.toEntity(savedAt: Long) = FoodEntity(
  id = id,
  name = name,
  brand = brand,
  category = category,
  kcal = kcal,
  protein = protein,
  carbs = carbs,
  fat = fat,
  servingLabel = serving?.label,
  servingAmount = serving?.amount,
  unit = unit,
  emoji = emoji,
  source = source.id,
  barcode = barcode,
  imageUrl = imageUrl,
  packageSize = packageSize,
  savedAt = savedAt,
)

fun Food.toSnapshot() = FoodSnapshot(
  id = id,
  name = name,
  brand = brand,
  kcal = kcal,
  protein = protein,
  carbs = carbs,
  fat = fat,
  servingLabel = serving?.label,
  servingAmount = serving?.amount,
  unit = unit,
  emoji = emoji,
  source = source.id,
  barcode = barcode,
  imageUrl = imageUrl,
)

fun FoodSnapshot.toFood() = Food(
  id = id,
  name = name,
  kcal = kcal,
  protein = protein,
  carbs = carbs,
  fat = fat,
  source = FoodSource.fromId(source),
  brand = brand,
  serving = serving(servingLabel, servingAmount),
  unit = unit,
  emoji = emoji,
  barcode = barcode,
  imageUrl = imageUrl,
)

fun DiaryEntryEntity.toEntry() = DiaryEntry(
  id = id,
  date = LocalDate.parse(date),
  meal = Meal.fromId(meal),
  amount = amount,
  addedAt = addedAt,
  food = food.toFood(),
)

fun DiaryEntry.toEntity() = DiaryEntryEntity(
  id = id,
  date = date.toString(),
  meal = meal.id,
  amount = amount,
  addedAt = addedAt,
  food = food.toSnapshot(),
)

fun ProfileEntity.toProfile() = Profile(
  sex = Sex.entries.firstOrNull { it.id == sex },
  age = age,
  heightCm = heightCm,
  weightKg = weightKg,
  activity = ActivityLevel.entries.firstOrNull { it.id == activity } ?: ActivityLevel.LIGHT,
  goal = Goal.entries.firstOrNull { it.id == goal } ?: Goal.MAINTAIN,
  macroSplit = MacroSplit.entries.firstOrNull { it.id == macroSplit } ?: MacroSplit.BALANCED,
  customTarget = customTarget,
)

fun Profile.toEntity() = ProfileEntity(
  sex = sex?.id,
  age = age,
  heightCm = heightCm,
  weightKg = weightKg,
  activity = activity.id,
  goal = goal.id,
  macroSplit = macroSplit.id,
  customTarget = customTarget,
)
