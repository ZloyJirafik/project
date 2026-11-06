package com.example.caloscan

import com.example.caloscan.domain.ActivityLevel
import com.example.caloscan.domain.CalorieCalculator
import com.example.caloscan.domain.Goal
import com.example.caloscan.domain.MacroSplit
import com.example.caloscan.domain.Profile
import com.example.caloscan.domain.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

  @Test
  fun `example from the README`() {
    // Man, 21 years, 180 cm, 75 kg, moderately active, losing 0.5 kg a week
    val profile = Profile(Sex.MALE, 21, 180.0, 75.0, ActivityLevel.MODERATE, Goal.LOSE_FAST)
    val t = CalorieCalculator.targets(profile)
    val b = t.breakdown!!
    assertEquals(1775, b.bmr) // 10×75 + 6.25×180 − 5×21 + 5
    assertEquals(2751, b.tdee) // 1775 × 1.55
    assertEquals(2250, t.kcal) // 2751 − 500, rounded to 10
    assertFalse(b.floored)
    assertTrue(t.isPersonal)
  }

  @Test
  fun `women get minus 161 in the BMR formula`() {
    val bmr = CalorieCalculator.bmr(Sex.FEMALE, weightKg = 60.0, heightCm = 165.0, age = 30)
    assertEquals(600 + 1031.25 - 150 - 161, bmr, 0.001)
  }

  @Test
  fun `target never goes below the safe minimum`() {
    val profile = Profile(Sex.FEMALE, 60, 150.0, 45.0, ActivityLevel.SEDENTARY, Goal.LOSE_FAST)
    val t = CalorieCalculator.targets(profile)
    assertEquals(1200, t.kcal)
    assertTrue(t.breakdown!!.floored)
  }

  @Test
  fun `incomplete profile uses the default target`() {
    val t = CalorieCalculator.targets(Profile(sex = Sex.MALE, age = 30))
    assertEquals(CalorieCalculator.DEFAULT_TARGET, t.kcal)
    assertNull(t.breakdown)
    assertFalse(t.isPersonal)
  }

  @Test
  fun `custom target wins over the calculated one`() {
    val profile = Profile(Sex.MALE, 21, 180.0, 75.0, customTarget = 3000)
    val t = CalorieCalculator.targets(profile)
    assertEquals(3000, t.kcal)
    assertTrue(t.overridden)
    assertTrue(t.breakdown!!.calculated != 3000)
  }

  @Test
  fun `macros are split by percent with 4 4 9 kcal per gram`() {
    val t = CalorieCalculator.targets(Profile(customTarget = 2000, macroSplit = MacroSplit.BALANCED))
    assertEquals(100, t.protein) // 20% of 2000 / 4
    assertEquals(250, t.carbs) // 50% of 2000 / 4
    assertEquals(67, t.fat) // 30% of 2000 / 9 = 66.7
  }

  @Test
  fun `bmi labels`() {
    assertEquals(23.1, CalorieCalculator.bmi(75.0, 180.0), 0.05)
    assertEquals("Недостаток веса", CalorieCalculator.bmiLabel(18.0))
    assertEquals("Норма", CalorieCalculator.bmiLabel(22.0))
    assertEquals("Избыточный вес", CalorieCalculator.bmiLabel(27.0))
    assertEquals("Ожирение", CalorieCalculator.bmiLabel(31.0))
  }

  @Test
  fun `kcal from macros`() {
    assertEquals(4.0 * 10 + 4 * 20 + 9 * 5, CalorieCalculator.kcalFromMacros(10.0, 20.0, 5.0), 0.001)
  }
}
