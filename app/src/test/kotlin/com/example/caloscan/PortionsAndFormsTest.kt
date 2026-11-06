package com.example.caloscan

import com.example.caloscan.data.BuiltInFoods
import com.example.caloscan.domain.AmountMode
import com.example.caloscan.domain.CustomFoodInput
import com.example.caloscan.domain.CustomFoodResult
import com.example.caloscan.domain.CustomFoodValidator
import com.example.caloscan.domain.Portions
import com.example.caloscan.util.dayLabel
import com.example.caloscan.util.fmt
import com.example.caloscan.util.grams
import com.example.caloscan.util.parseNumber
import com.example.caloscan.util.plain
import com.example.caloscan.util.plural
import com.example.caloscan.util.round1
import com.example.caloscan.util.russianUnits
import com.example.caloscan.util.shortWeekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PortionsAndFormsTest {
  private val banana = BuiltInFoods.get("b-banana")!! // 1 шт. = 118 г

  @Test
  fun `new entry starts at one serving`() {
    val p = Portions.initial(banana, existingAmount = null)
    assertEquals(AmountMode.SERVINGS, p.mode)
    assertEquals(1.0, p.quantity, 0.0)
    assertEquals(118.0, Portions.amount(banana, p.mode, p.quantity), 0.0)
  }

  @Test
  fun `existing entry shows servings only for quarter servings`() {
    assertEquals(AmountMode.SERVINGS, Portions.initial(banana, 59.0).mode) // ½ banana
    assertEquals(AmountMode.UNITS, Portions.initial(banana, 100.0).mode)
  }

  @Test
  fun `plus and minus jump to the next step`() {
    assertEquals(1.5, Portions.step(1.0, AmountMode.SERVINGS, +1), 0.0)
    assertEquals(0.5, Portions.step(0.5, AmountMode.SERVINGS, -1), 0.0) // never 0
    assertEquals(130.0, Portions.step(123.0, AmountMode.UNITS, +1), 0.0)
    assertEquals(120.0, Portions.step(123.0, AmountMode.UNITS, -1), 0.0)
    assertEquals(5.0, Portions.step(10.0, AmountMode.UNITS, -1), 0.0)
  }

  @Test
  fun `switching units keeps the amount`() {
    assertEquals(1.5, Portions.convert(banana, 177.0, AmountMode.SERVINGS), 0.0)
    assertEquals(177.0, Portions.convert(banana, 177.2, AmountMode.UNITS), 0.0)
  }

  @Test
  fun `nutrition is per 100 g`() {
    val n = banana.nutritionFor(200.0)
    assertEquals(178.0, n.kcal, 0.001) // 89 kcal per 100 g
  }

  @Test
  fun `custom food per serving is converted to per 100 g`() {
    val result = CustomFoodValidator.validate(
      CustomFoodInput(name = "Протеиновый батончик", perServing = true, kcal = "200", protein = "20", carbs = "15", fat = "6", servingLabel = "1 батончик", servingAmount = "50"),
    )
    val food = (result as CustomFoodResult.Ok).food
    assertEquals(400.0, food.kcal, 0.0)
    assertEquals(40.0, food.protein, 0.0)
    assertEquals("1 батончик", food.serving?.label)
    assertEquals(50.0, food.serving?.amount)
  }

  @Test
  fun `custom food calories can come from macros`() {
    val result = CustomFoodValidator.validate(CustomFoodInput(name = "Mix", protein = "10", carbs = "10", fat = "10"))
    assertEquals(170.0, (result as CustomFoodResult.Ok).food.kcal, 0.0)
  }

  @Test
  fun `custom food errors`() {
    assertTrue(CustomFoodValidator.validate(CustomFoodInput(name = " ")) is CustomFoodResult.Invalid)
    assertTrue(CustomFoodValidator.validate(CustomFoodInput(name = "X")) is CustomFoodResult.Invalid)
    assertTrue(CustomFoodValidator.validate(CustomFoodInput(name = "X", kcal = "950")) is CustomFoodResult.Invalid)
    assertTrue(CustomFoodValidator.validate(CustomFoodInput(name = "X", kcal = "100", perServing = true)) is CustomFoodResult.Invalid)
  }

  @Test
  fun `number formatting`() {
    assertEquals("1\u00A0234", fmt(1234.4)) // non-breaking space between thousands
    assertEquals("12\u00A0345\u00A0678", fmt(12345678))
    assertEquals("0,4", grams(0.42))
    assertEquals("3", grams(3.0))
    assertEquals("12", grams(12.4))
    assertEquals("1,33", plain(1.3333))
    assertEquals("100", plain(100.0))
    // Halves round up, even when the computer stores them as 0.34999…
    assertEquals(0.4, round1(0.35), 0.0)
    assertEquals(1.1, round1(1.05), 0.0)
    assertEquals("0,4", grams(0.35))
    assertEquals("1,01", plain(1.005))
    assertEquals(12.5, parseNumber("12,5")!!, 0.0)
    assertEquals(12.5, parseNumber("12.5")!!, 0.0)
    assertNull(parseNumber("abc"))
    assertEquals("400 г · 1,5 л · 330 мл · 1 кг", russianUnits("400 g · 1,5 l · 330ml · 1 kg"))
    assertEquals("2 glasses", russianUnits("2 glasses"))
  }

  @Test
  fun `russian plurals`() {
    assertEquals("день", plural(1, "день", "дня", "дней"))
    assertEquals("дня", plural(3, "день", "дня", "дней"))
    assertEquals("дней", plural(5, "день", "дня", "дней"))
    assertEquals("дней", plural(11, "день", "дня", "дней"))
    assertEquals("дней", plural(14, "день", "дня", "дней"))
    assertEquals("день", plural(21, "день", "дня", "дней"))
    assertEquals("дня", plural(22, "день", "дня", "дней"))
    assertEquals("дней", plural(0, "день", "дня", "дней"))
  }

  @Test
  fun `day names in russian`() {
    val today = LocalDate.of(2026, 11, 3)
    assertEquals("Сегодня", dayLabel(today, today))
    assertEquals("Вчера", dayLabel(today.minusDays(1), today))
    assertEquals("Завтра", dayLabel(today.plusDays(1), today))
    assertEquals("Вс, 1 нояб.", dayLabel(LocalDate.of(2026, 11, 1), today))
    assertEquals("Пн", shortWeekday(DayOfWeek.MONDAY))
  }
}
