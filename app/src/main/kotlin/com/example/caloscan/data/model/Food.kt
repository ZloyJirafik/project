package com.example.caloscan.data.model

import java.time.LocalDate

/** Where a food comes from. [id] is the value saved in the database and in backups. */
enum class FoodSource(val id: String, val label: String) {
  BUILTIN("builtin", "База продуктов"),
  CUSTOM("custom", "Мой продукт"),
  OPEN_FOOD_FACTS("off", "Open Food Facts");

  companion object {
    fun fromId(id: String): FoodSource = entries.firstOrNull { it.id == id } ?: CUSTOM
  }
}

/** A typical portion, e.g. "1 medium" = 182 g. [amount] is in the food's unit (g or ml). */
data class Serving(val label: String, val amount: Double)

/**
 * A food or packaged product.
 *
 * All nutrition values are per 100 g, or per 100 ml when [unit] is "ml".
 */
data class Food(
  val id: String,
  val name: String,
  val kcal: Double,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
  val source: FoodSource,
  val brand: String = "",
  val category: String = "",
  val serving: Serving? = null,
  val unit: String = "g",
  val emoji: String = "",
  val barcode: String = "",
  val imageUrl: String = "",
  /** Package size printed on the product, e.g. "400 g". Only for scanned products. */
  val packageSize: String = "",
  /** False when Open Food Facts knows the product but has no nutrition facts for it. */
  val hasNutrition: Boolean = true,
  /** Extra words that find this food in search (the English name of built-in foods). Not saved. */
  val searchTerms: String = "",
) {
  /** The unit as shown on screen: "г" or "мл". */
  val unitLabel: String get() = unitLabel(unit)

  /** Nutrition for [amount] grams (or ml) of this food. */
  fun nutritionFor(amount: Double): Nutrition {
    val k = amount / 100.0
    return Nutrition(kcal * k, protein * k, carbs * k, fat * k)
  }
}

/** Units are saved as "g" / "ml" and shown in Russian. */
fun unitLabel(unit: String): String = if (unit == "ml") "мл" else "г"

data class Nutrition(
  val kcal: Double = 0.0,
  val protein: Double = 0.0,
  val carbs: Double = 0.0,
  val fat: Double = 0.0,
) {
  operator fun plus(other: Nutrition) =
    Nutrition(kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)
}

/** [into] is used in "Добавить в завтрак". */
enum class Meal(val id: String, val label: String, val shortLabel: String, val into: String) {
  BREAKFAST("breakfast", "Завтрак", "Завтрак", "завтрак"),
  LUNCH("lunch", "Обед", "Обед", "обед"),
  DINNER("dinner", "Ужин", "Ужин", "ужин"),
  SNACK("snack", "Перекусы", "Перекус", "перекусы");

  companion object {
    fun fromId(id: String): Meal = entries.firstOrNull { it.id == id } ?: SNACK

    /** The meal that fits the time of day, used as the default when adding food. */
    fun forHour(hour: Int): Meal = when {
      hour < 11 -> BREAKFAST
      hour < 16 -> LUNCH
      hour < 21 -> DINNER
      else -> SNACK
    }
  }
}

/**
 * One food logged in the diary.
 *
 * [food] is a copy of the food at the time it was logged, so old days never
 * change when a food is edited or deleted later.
 */
data class DiaryEntry(
  val id: Long,
  val date: LocalDate,
  val meal: Meal,
  /** Grams or ml. */
  val amount: Double,
  val addedAt: Long,
  val food: Food,
) {
  val nutrition: Nutrition get() = food.nutritionFor(amount)
}

fun List<DiaryEntry>.total(): Nutrition = fold(Nutrition()) { sum, entry -> sum + entry.nutrition }
