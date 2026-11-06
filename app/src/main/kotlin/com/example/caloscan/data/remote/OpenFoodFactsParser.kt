package com.example.caloscan.data.remote

import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Serving
import com.example.caloscan.util.round1
import com.example.caloscan.util.russianUnits
import org.json.JSONObject
import kotlin.math.roundToInt

/** Converts Open Food Facts product JSON into the app's [Food]. */
object OpenFoodFactsParser {
  const val UNNAMED = "Без названия"

  // "400 g ℮" → "400 g" (℮ is the EU "estimated quantity" sign, sometimes typed as "e")
  private val estimatedSign = Regex("\\s*(℮|\\be)\\s*$", RegexOption.IGNORE_CASE)
  // "1 l", "500 ml", "1 л", "500 мл"
  private val liquid = Regex("\\d\\s*(ml|cl|dl|l|мл|л)(?!\\p{L})", RegexOption.IGNORE_CASE)
  private val leadingNumber = Regex("^\\s*-?\\d+(\\.\\d+)?")

  fun toFood(product: JSONObject, fallbackCode: String = ""): Food {
    val n = product.optJSONObject("nutriments") ?: JSONObject()

    // Calories per 100 g. Some products only have kilojoules (1 kcal = 4.184 kJ).
    val kcal = number(n, "energy-kcal_100g")
      ?: number(n, "energy-kj_100g")?.div(4.184)
      ?: number(n, "energy_100g")?.div(4.184) // energy_100g is in kJ

    val quantity = estimatedSign.replace(text(product, "quantity"), "").trim()
    val servingAmount = number(product, "serving_quantity")
    // The Russian name first, if the product has one
    val name = listOf("product_name_ru", "product_name", "product_name_en", "generic_name_ru", "generic_name")
      .map { text(product, it).trim() }
      .firstOrNull { it.isNotEmpty() }
    val code = text(product, "code").ifBlank { fallbackCode }

    return Food(
      id = "off-$code",
      barcode = code,
      name = name ?: UNNAMED,
      brand = text(product, "brands").split(",").first().trim(),
      kcal = kcal?.roundToInt()?.toDouble() ?: 0.0,
      protein = round1(number(n, "proteins_100g") ?: 0.0),
      carbs = round1(number(n, "carbohydrates_100g") ?: 0.0),
      fat = round1(number(n, "fat_100g") ?: 0.0),
      hasNutrition = kcal != null,
      serving = servingAmount?.takeIf { it > 0 }?.let {
        Serving(russianUnits(text(product, "serving_size").trim()).ifEmpty { "1 порция" }, round1(it))
      },
      unit = if (liquid.containsMatchIn(quantity)) "ml" else "g",
      packageSize = russianUnits(quantity),
      emoji = "🏷️",
      imageUrl = text(product, "image_front_small_url"),
      source = FoodSource.OPEN_FOOD_FACTS,
    )
  }

  /** A number that may be stored as a JSON number or as text like "12,5". */
  private fun number(json: JSONObject, key: String): Double? {
    if (!json.has(key) || json.isNull(key)) return null
    val value = when (val raw = json.opt(key)) {
      is Number -> raw.toDouble()
      is String -> leadingNumber.find(raw.replace(',', '.'))?.value?.toDoubleOrNull()
      else -> null
    }
    return value?.takeIf { it.isFinite() }
  }

  private fun text(json: JSONObject, key: String): String =
    if (!json.has(key) || json.isNull(key)) "" else json.opt(key)?.toString().orEmpty()
}
