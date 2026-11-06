package com.example.caloscan

import com.example.caloscan.data.BuiltInFoods
import com.example.caloscan.data.local.BackupData
import com.example.caloscan.data.local.BackupJson
import com.example.caloscan.data.local.DiaryEntryEntity
import com.example.caloscan.data.local.ProfileEntity
import com.example.caloscan.data.local.RecentFoodEntity
import com.example.caloscan.data.local.toEntity
import com.example.caloscan.data.local.toSnapshot
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.remote.OpenFoodFactsParser
import com.example.caloscan.domain.CalorieCalculator
import com.example.caloscan.domain.Profile
import com.example.caloscan.domain.ProgressCalculator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DataTest {

  @Test
  fun `parses an Open Food Facts product`() {
    val json = JSONObject(
      """
      {"code":"3017620422003","product_name":"Nutella","brands":"Ferrero,Nutella",
       "quantity":"400 g ℮","serving_size":"15 g","serving_quantity":15,
       "nutriments":{"energy-kcal_100g":539,"proteins_100g":6.3,"carbohydrates_100g":57.5,"fat_100g":30.9}}
      """,
    )
    val food = OpenFoodFactsParser.toFood(json)
    assertEquals("off-3017620422003", food.id)
    assertEquals("Nutella", food.name)
    assertEquals("Ferrero", food.brand)
    assertEquals(539.0, food.kcal, 0.0)
    assertEquals("400 г", food.packageSize) // ℮ removed, unit in Russian
    assertEquals("15 г", food.serving?.label)
    assertEquals("g", food.unit)
    assertEquals(15.0, food.serving?.amount)
    assertTrue(food.hasNutrition)
  }

  @Test
  fun `kilojoules are converted and drinks use ml`() {
    val json = JSONObject("""{"code":"1","product_name":"Juice","quantity":"1 l","nutriments":{"energy-kj_100g":"188,3"}}""")
    val food = OpenFoodFactsParser.toFood(json)
    assertEquals(45.0, food.kcal, 0.0) // 188.3 kJ ÷ 4.184
    assertEquals("ml", food.unit)
  }

  @Test
  fun `russian product name is preferred and litres are ml`() {
    val json = JSONObject(
      """{"code":"2","product_name":"Kefir","product_name_ru":"Кефир 2,5%","quantity":"1 л","nutriments":{"energy-kcal_100g":53}}""",
    )
    val food = OpenFoodFactsParser.toFood(json)
    assertEquals("Кефир 2,5%", food.name)
    assertEquals("ml", food.unit)
  }

  @Test
  fun `product without nutrition facts`() {
    val food = OpenFoodFactsParser.toFood(JSONObject("""{"code":"42"}"""), fallbackCode = "42")
    assertFalse(food.hasNutrition)
    assertEquals(OpenFoodFactsParser.UNNAMED, food.name)
    assertNull(food.serving)
  }

  @Test
  fun `backup survives a round trip`() {
    val banana = BuiltInFoods.get("b-banana")!!
    val data = BackupData(
      profile = ProfileEntity(sex = "male", age = 21, heightCm = 180.0, weightKg = 75.0, activity = "moderate", goal = "lose", macroSplit = "balanced", customTarget = null),
      foods = listOf(banana.copy(id = "c-1").toEntity(savedAt = 5)),
      entries = listOf(DiaryEntryEntity(id = 7, date = "2026-11-01", meal = "lunch", amount = 118.0, addedAt = 9, food = banana.toSnapshot())),
      recent = listOf(RecentFoodEntity("b-banana", 9)),
    )
    val text = BackupJson.write(data, exportedAt = "2026-11-01T12:00:00Z")
    assertTrue(text.contains("\"app\": \"MyBody\""))
    assertEquals(data, BackupJson.read(text))
  }

  @Test
  fun `backups from before the rename to MyBody still work`() {
    val old = """{"app":"CaloScan","version":1,"profile":null,"foods":[],"entries":[],"recent":[{"foodId":"b-banana","usedAt":9}]}"""
    assertEquals(listOf(RecentFoodEntity("b-banana", 9)), BackupJson.read(old).recent)
  }

  @Test
  fun `other files are rejected with a clear message`() {
    val error = assertThrows(IllegalArgumentException::class.java) { BackupJson.read("""{"hello":1}""") }
    assertEquals("Это не резервная копия MyBody.", error.message)
    assertThrows(IllegalArgumentException::class.java) { BackupJson.read("not json") }
  }

  @Test
  fun `progress counts streak and days on target`() {
    val today = LocalDate.of(2026, 11, 1)
    val targets = CalorieCalculator.targets(Profile(customTarget = 2000))
    val rice = BuiltInFoods.get("b-white-rice-cooked")!! // 130 kcal per 100 g
    fun entry(daysAgo: Long, grams: Double) =
      DiaryEntry(daysAgo, today.minusDays(daysAgo), Meal.LUNCH, grams, 0, rice)
    val entries = listOf(entry(0, 1500.0), entry(1, 1000.0), entry(2, 1540.0), entry(5, 800.0))

    val summary = ProgressCalculator.summarize(7, today, entries, entries.map { it.date }.toSet(), targets)
    assertEquals(7, summary.days.size)
    assertEquals(4, summary.loggedDays)
    assertEquals(3, summary.streak) // today, yesterday, 2 days ago
    assertEquals(2, summary.onTargetDays) // 1950 and 2002 kcal are within ±10% of 2000
  }

  @Test
  fun `streak can end yesterday`() {
    val today = LocalDate.of(2026, 11, 1)
    val logged = setOf(today.minusDays(1), today.minusDays(2))
    assertEquals(2, ProgressCalculator.streak(today, logged))
    assertEquals(0, ProgressCalculator.streak(today, emptySet()))
  }
}
