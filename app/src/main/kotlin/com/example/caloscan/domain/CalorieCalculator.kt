package com.example.caloscan.domain

import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Daily calorie target:
 *   1. BMR (Basal Metabolic Rate), Mifflin-St Jeor equation
 *        men:   10 × weight(kg) + 6.25 × height(cm) − 5 × age + 5
 *        women: 10 × weight(kg) + 6.25 × height(cm) − 5 × age − 161
 *   2. TDEE (Total Daily Energy Expenditure) = BMR × activity factor
 *   3. Target = TDEE + goal adjustment (≈7,700 kcal per kg of body fat,
 *      so −500 kcal/day ≈ −0.5 kg/week), never below a safe minimum.
 *   4. Macros: the target split by percentage, then
 *        protein & carbs: 4 kcal per gram, fat: 9 kcal per gram
 */

enum class Sex(val id: String, val label: String) {
  FEMALE("female", "Женский"),
  MALE("male", "Мужской"),
}

enum class ActivityLevel(val id: String, val label: String, val hint: String, val factor: Double) {
  SEDENTARY("sedentary", "Минимальная", "Сидячая работа, мало движения", 1.2),
  LIGHT("light", "Низкая", "Тренировки 1–3 раза в неделю", 1.375),
  MODERATE("moderate", "Средняя", "Тренировки 3–5 раз в неделю", 1.55),
  ACTIVE("active", "Высокая", "Интенсивные тренировки 6–7 раз в неделю", 1.725),
  EXTRA("extra", "Очень высокая", "Физическая работа и ежедневные тренировки", 1.9),
}

enum class Goal(val id: String, val label: String, val delta: Int) {
  LOSE_FAST("lose_fast", "Худеть на 0,5 кг в неделю", -500),
  LOSE("lose", "Худеть на 0,25 кг в неделю", -250),
  MAINTAIN("maintain", "Сохранять вес", 0),
  GAIN("gain", "Набирать 0,25 кг в неделю", 250),
  GAIN_FAST("gain_fast", "Набирать 0,5 кг в неделю", 500),
}

/** Percent of calories from protein / carbs / fat. */
enum class MacroSplit(val id: String, val label: String, val protein: Int, val carbs: Int, val fat: Int) {
  BALANCED("balanced", "Сбалансированное", 20, 50, 30),
  HIGH_PROTEIN("high_protein", "Больше белка", 30, 40, 30),
  LOW_CARB("low_carb", "Меньше углеводов", 30, 25, 45),
}

data class Profile(
  val sex: Sex? = null,
  val age: Int? = null,
  val heightCm: Double? = null,
  val weightKg: Double? = null,
  val activity: ActivityLevel = ActivityLevel.LIGHT,
  val goal: Goal = Goal.MAINTAIN,
  val macroSplit: MacroSplit = MacroSplit.BALANCED,
  /** Optional manual daily target. When set it replaces the calculated one. */
  val customTarget: Int? = null,
)

/** How the calculated target was reached. Only available when the profile is complete. */
data class TargetBreakdown(
  val bmr: Int,
  val tdee: Int,
  val activity: ActivityLevel,
  val goal: Goal,
  val calculated: Int,
  /** True when the result was raised to the safe minimum. */
  val floored: Boolean,
  val minimum: Int,
  val bmi: Double,
  val bmiLabel: String,
)

data class Targets(
  val kcal: Int,
  val protein: Int,
  val carbs: Int,
  val fat: Int,
  val split: MacroSplit,
  val breakdown: TargetBreakdown?,
  val overridden: Boolean,
) {
  /** False while the app uses the 2,000 kcal default. */
  val isPersonal: Boolean get() = breakdown != null || overridden
}

object CalorieCalculator {
  const val DEFAULT_TARGET = 2000

  fun minimumKcal(sex: Sex): Int = if (sex == Sex.MALE) 1500 else 1200

  fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
    val base = 10 * weightKg + 6.25 * heightCm - 5 * age
    return if (sex == Sex.MALE) base + 5 else base - 161
  }

  fun bmi(weightKg: Double, heightCm: Double): Double {
    val meters = heightCm / 100
    return weightKg / (meters * meters)
  }

  fun bmiLabel(bmi: Double): String = when {
    bmi < 18.5 -> "Недостаток веса"
    bmi < 25 -> "Норма"
    bmi < 30 -> "Избыточный вес"
    else -> "Ожирение"
  }

  fun targets(profile: Profile): Targets {
    val sex = profile.sex
    val age = profile.age
    val height = profile.heightCm
    val weight = profile.weightKg

    var kcal = DEFAULT_TARGET
    var breakdown: TargetBreakdown? = null

    if (sex != null && age != null && height != null && weight != null && age > 0 && height > 0 && weight > 0) {
      val bmr = bmr(sex, weight, height, age)
      val tdee = bmr * profile.activity.factor
      val minimum = minimumKcal(sex)
      val raw = tdee + profile.goal.delta
      // Round to the nearest 10 kcal
      val calculated = (max(raw, minimum.toDouble()) / 10).roundToInt() * 10
      val bmi = bmi(weight, height)
      breakdown = TargetBreakdown(
        bmr = bmr.roundToInt(),
        tdee = tdee.roundToInt(),
        activity = profile.activity,
        goal = profile.goal,
        calculated = calculated,
        floored = raw < minimum,
        minimum = minimum,
        bmi = bmi,
        bmiLabel = bmiLabel(bmi),
      )
      kcal = calculated
    }

    // A manual target always wins
    val custom = profile.customTarget
    val overridden = custom != null && custom > 0
    if (custom != null && custom > 0) kcal = custom

    val split = profile.macroSplit
    return Targets(
      kcal = kcal,
      protein = (kcal * split.protein / 100.0 / 4).roundToInt(),
      carbs = (kcal * split.carbs / 100.0 / 4).roundToInt(),
      fat = (kcal * split.fat / 100.0 / 9).roundToInt(),
      split = split,
      breakdown = breakdown,
      overridden = overridden,
    )
  }

  /** Estimates calories from macros (Atwater factors 4 / 4 / 9). */
  fun kcalFromMacros(protein: Double, carbs: Double, fat: Double): Double =
    protein * 4 + carbs * 4 + fat * 9
}
