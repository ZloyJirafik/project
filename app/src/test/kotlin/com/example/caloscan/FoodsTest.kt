package com.example.caloscan

import com.example.caloscan.data.BuiltInFoods
import com.example.caloscan.data.FoodCategory
import com.example.caloscan.data.slug
import com.example.caloscan.domain.FoodSearch
import com.example.caloscan.ui.FoodFilter
import com.example.caloscan.ui.buildFoodResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodsTest {

  @Test
  fun `database has 149 foods with unique ids and names`() {
    assertEquals(149, BuiltInFoods.all.size)
    assertEquals(BuiltInFoods.all.size, BuiltInFoods.all.map { it.id }.toSet().size)
    assertEquals(BuiltInFoods.all.size, BuiltInFoods.all.map { it.name }.toSet().size)
  }

  @Test
  fun `every category has foods and drinks are in ml`() {
    FoodCategory.entries.forEach { assertTrue(it.label, BuiltInFoods.inCategory(it).isNotEmpty()) }
    assertTrue(BuiltInFoods.inCategory(FoodCategory.DRINK).all { it.unit == "ml" })
    assertEquals("мл", BuiltInFoods.get("b-milk-2")!!.unitLabel)
    assertEquals("г", BuiltInFoods.get("b-apple")!!.unitLabel)
  }

  @Test
  fun `no food has more calories than pure fat`() {
    BuiltInFoods.all.forEach { assertTrue(it.name, it.kcal <= 900) }
  }

  @Test
  fun `ids come from the English name and stay the same`() {
    assertEquals("oats-rolled-dry", slug("Oats, rolled (dry)"))
    assertEquals("dark-chocolate-70-85", slug("Dark chocolate (70–85%)"))
    assertEquals("Банан", BuiltInFoods.get("b-banana")!!.name)
  }

  @Test
  fun `best match comes first`() {
    val results = FoodSearch.search(BuiltInFoods.all, "яблок")
    assertEquals("Яблоко", results.first().name)
  }

  @Test
  fun `English names also work`() {
    val results = FoodSearch.search(BuiltInFoods.all, "apple")
    assertEquals("Яблоко", results.first().name)
    assertTrue(results.any { it.name == "Сок яблочный" })
  }

  @Test
  fun `all search words must match, in any order`() {
    val results = FoodSearch.search(BuiltInFoods.all, "варёная грудка")
    assertEquals(listOf("Куриная грудка варёная"), results.map { it.name })
  }

  @Test
  fun `search ignores case and the dots of ё`() {
    assertEquals("Банан", FoodSearch.search(BuiltInFoods.all, "БАНАН").first().name)
    assertEquals("Тёмный шоколад (70–85%)", FoodSearch.search(BuiltInFoods.all, "темный шоколад").first().name)
    assertEquals("Куриная грудка варёная", FoodSearch.search(BuiltInFoods.all, "грудка вареная").first().name)
    assertTrue(FoodSearch.search(BuiltInFoods.all, "xyzzy").isEmpty())
  }

  @Test
  fun `browse mode lists every category`() {
    val results = buildFoodResults("", FoodFilter.All, emptyList(), emptyList(), emptyList())
    assertEquals(FoodCategory.entries.size, results.sections.size)
  }

  @Test
  fun `no match explains what to do`() {
    val results = buildFoodResults("xyzzy", FoodFilter.All, emptyList(), emptyList(), emptyList())
    assertTrue(results.sections.isEmpty())
    assertEquals("Ничего не найдено по запросу «xyzzy»", results.emptyTitle)
  }
}
