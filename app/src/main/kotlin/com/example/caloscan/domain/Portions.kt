package com.example.caloscan.domain

import com.example.caloscan.data.model.Food
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Whether the amount field is in servings ("1.5 servings") or in grams / ml. */
enum class AmountMode { SERVINGS, UNITS }

data class Portion(val mode: AmountMode, val quantity: Double)

/** Logic behind the amount field of the "add food" sheet. */
object Portions {

  /**
   * The starting amount when the sheet opens.
   * New entry: 1 serving if the food has one, otherwise 100 g.
   * Existing entry: in servings if it's a whole number of quarter servings.
   */
  fun initial(food: Food, existingAmount: Double?): Portion {
    val serving = food.serving?.amount?.takeIf { it > 0 }
    if (existingAmount != null) {
      if (serving != null) {
        val servings = existingAmount / serving
        if (abs(servings * 4 - (servings * 4).roundToInt()) < 0.001) {
          return Portion(AmountMode.SERVINGS, servings)
        }
      }
      return Portion(AmountMode.UNITS, existingAmount)
    }
    return if (serving != null) Portion(AmountMode.SERVINGS, 1.0) else Portion(AmountMode.UNITS, 100.0)
  }

  /** Grams (or ml) for a quantity in the given mode. */
  fun amount(food: Food, mode: AmountMode, quantity: Double): Double {
    val serving = food.serving?.amount
    return if (mode == AmountMode.SERVINGS && serving != null) quantity * serving else quantity
  }

  /** The quantity to show after switching between servings and grams. */
  fun convert(food: Food, amount: Double, to: AmountMode): Double {
    val serving = food.serving?.amount
    return if (to == AmountMode.SERVINGS && serving != null && serving > 0) {
      (amount / serving * 100).roundToInt() / 100.0
    } else {
      amount.roundToInt().toDouble()
    }
  }

  /**
   * The − and + buttons: jump to the next multiple of 0.5 servings or 10 g.
   * Never goes to 0 or below.
   */
  fun step(value: Double, mode: AmountMode, direction: Int): Double {
    val step = if (mode == AmountMode.SERVINGS) 0.5 else 10.0
    var next = if (direction > 0) {
      floor(value / step + 1e-9) * step + step
    } else {
      ceil(value / step - 1e-9) * step - step
    }
    if (next <= 0) {
      next = if (mode == AmountMode.SERVINGS) 0.5 else min(value, 5.0).takeIf { it > 0 } ?: 5.0
    }
    return (next * 100).roundToInt() / 100.0
  }
}
