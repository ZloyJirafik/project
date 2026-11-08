package com.example.caloscan

import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.remote.OpenFoodFactsParser
import com.example.caloscan.domain.ProductCode
import com.example.caloscan.ui.CustomFoodDraft
import com.example.caloscan.ui.FoodFilter
import com.example.caloscan.ui.buildFoodResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reading product numbers from QR / DataMatrix codes, and adding scanned products to "My foods". */
class ScanTest {

  @Test
  fun `check digits of real barcodes`() {
    assertTrue(ProductCode.hasValidCheckDigit("3017620422003")) // Nutella
    assertTrue(ProductCode.hasValidCheckDigit("5449000000996")) // Coca-Cola
    assertTrue(ProductCode.hasValidCheckDigit("03017620422003")) // the same as GTIN-14
    assertFalse(ProductCode.hasValidCheckDigit("3017620422004"))
    assertFalse(ProductCode.hasValidCheckDigit("12345")) // too short
  }

  @Test
  fun `QR code with just the number`() {
    assertEquals("3017620422003", ProductCode.fromText("3017620422003"))
    assertEquals("3017620422003", ProductCode.fromText(" 03017620422003\n"))
    assertNull(ProductCode.fromText("3017620422004")) // wrong check digit
  }

  @Test
  fun `GS1 Digital Link web address`() {
    assertEquals("3017620422003", ProductCode.fromText("https://id.gs1.org/01/03017620422003"))
    assertEquals("5449000000996", ProductCode.fromText("https://brand.example/01/05449000000996/10/AB12?17=271231"))
    assertEquals("3017620422003", ProductCode.fromText("HTTPS://ID.GS1.ORG/01/03017620422003/21/XYZ"))
  }

  @Test
  fun `Chestny ZNAK DataMatrix and GS1 data`() {
    // "01" + GTIN-14, "21" + serial number, separator (ASCII 29), "93" + verification code
    assertEquals("5449000000996", ProductCode.fromText("0105449000000996215Abc?eF\u001D93dGVz"))
    assertEquals("5449000000996", ProductCode.fromText("]d20105449000000996215Abc"))
    assertEquals("3017620422003", ProductCode.fromText("(01)03017620422003(21)12345"))
  }

  @Test
  fun `codes without a product number are ignored`() {
    assertNull(ProductCode.fromText("https://www.nutella.com/ru/ru"))
    assertNull(ProductCode.fromText("https://example.com/01/12345678")) // not a valid barcode number
    assertNull(ProductCode.fromText("Скидка 10% на кассе"))
    assertNull(ProductCode.fromText(""))
  }

  @Test
  fun `product card loads the bigger Open Food Facts photo`() {
    assertEquals(
      "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_ru.12.400.jpg",
      OpenFoodFactsParser.largeImageUrl("https://images.openfoodfacts.org/images/products/301/762/042/2003/front_ru.12.200.jpg"),
    )
    assertEquals("https://example.com/photo.200.jpg", OpenFoodFactsParser.largeImageUrl("https://example.com/photo.200.jpg"))
    assertEquals("", OpenFoodFactsParser.largeImageUrl(""))
  }

  @Test
  fun `a product added to My foods is listed once in search`() {
    val scanned = product("off-3017620422003", FoodSource.OPEN_FOOD_FACTS)
    val mine = product("c-1", FoodSource.CUSTOM)
    val results = buildFoodResults("nutella", FoodFilter.All, listOf(mine), listOf(scanned), emptyList())
    assertEquals(listOf("c-1"), results.sections.single().foods.map { it.id })

    // Not added yet: the scanned product is still found
    val before = buildFoodResults("nutella", FoodFilter.All, emptyList(), listOf(scanned), emptyList())
    assertEquals(listOf("off-3017620422003"), before.sections.single().foods.map { it.id })
  }

  @Test
  fun `form after a scan keeps the photo but not a missing name`() {
    val noNutrition = product("off-3017620422003", FoodSource.OPEN_FOOD_FACTS)
      .copy(name = OpenFoodFactsParser.UNNAMED, hasNutrition = false, packageSize = "400 г")
    val draft = CustomFoodDraft.fromScan("3017620422003", noNutrition)
    assertEquals("", draft.name)
    assertEquals("Ferrero", draft.brand)
    assertEquals("3017620422003", draft.barcode)
    assertEquals(noNutrition.imageUrl, draft.imageUrl)
    assertEquals("400 г", draft.packageSize)
    assertTrue(draft.fromScan)

    // Editing a saved product keeps its photo too
    assertEquals(noNutrition.imageUrl, CustomFoodDraft.from(noNutrition).imageUrl)
    assertFalse(CustomFoodDraft.from(noNutrition).fromScan)
  }

  private fun product(id: String, source: FoodSource) = Food(
    id = id,
    name = "Nutella",
    brand = "Ferrero",
    kcal = 539.0,
    protein = 6.3,
    carbs = 57.5,
    fat = 30.9,
    source = source,
    barcode = "3017620422003",
    imageUrl = "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_ru.12.200.jpg",
  )
}
