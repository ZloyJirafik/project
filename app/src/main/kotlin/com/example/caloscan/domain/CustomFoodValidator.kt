package com.example.caloscan.domain

import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Serving
import com.example.caloscan.util.parseNumber
import com.example.caloscan.util.round1
import kotlin.math.roundToInt

/** What the user typed in the "New food" form. All numbers are still text. */
data class CustomFoodInput(
  val name: String,
  val brand: String = "",
  /** True when the values are per serving instead of per 100 g. */
  val perServing: Boolean = false,
  val kcal: String = "",
  val protein: String = "",
  val carbs: String = "",
  val fat: String = "",
  val servingLabel: String = "",
  val servingAmount: String = "",
  val unit: String = "g",
  val barcode: String = "",
)

sealed interface CustomFoodResult {
  data class Ok(val food: Food) : CustomFoodResult
  data class Invalid(val message: String) : CustomFoodResult
}

object CustomFoodValidator {

  /**
   * Checks the form and turns it into a food with values per 100 g.
   * [id] is the id of the food being edited, or null for a new food.
   */
  fun validate(input: CustomFoodInput, id: String? = null): CustomFoodResult {
    val name = input.name.trim()
    if (name.isEmpty()) return CustomFoodResult.Invalid("Введите название продукта.")

    fun number(text: String) = parseNumber(text)?.takeIf { it >= 0 }

    var protein = number(input.protein) ?: 0.0
    var carbs = number(input.carbs) ?: 0.0
    var fat = number(input.fat) ?: 0.0
    var kcal = number(input.kcal) ?: run {
      if (protein == 0.0 && carbs == 0.0 && fat == 0.0) {
        return CustomFoodResult.Invalid("Укажите калории или белки, жиры и углеводы.")
      }
      CalorieCalculator.kcalFromMacros(protein, carbs, fat)
    }

    val servingAmount = number(input.servingAmount)?.takeIf { it > 0 }
    if (input.perServing) {
      if (servingAmount == null) {
        return CustomFoodResult.Invalid("Укажите размер порции, чтобы пересчитать значения на 100 г.")
      }
      val k = 100 / servingAmount
      kcal *= k
      protein *= k
      carbs *= k
      fat *= k
    }
    // Pure fat has 900 kcal per 100 g, so anything above is a typo
    if (kcal > 900) {
      return CustomFoodResult.Invalid("Больше 900 ккал на 100 г не бывает. Проверьте значения.")
    }

    return CustomFoodResult.Ok(
      Food(
        id = id ?: "",
        name = name,
        brand = input.brand.trim(),
        barcode = input.barcode.filterNot { it.isWhitespace() },
        kcal = kcal.roundToInt().toDouble(),
        protein = round1(protein),
        carbs = round1(carbs),
        fat = round1(fat),
        unit = input.unit,
        serving = servingAmount?.let { Serving(input.servingLabel.trim().ifEmpty { "1 порция" }, round1(it)) },
        source = FoodSource.CUSTOM,
        emoji = "✏️",
      ),
    )
  }
}
