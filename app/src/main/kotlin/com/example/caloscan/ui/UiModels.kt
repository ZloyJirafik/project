package com.example.caloscan.ui

import androidx.annotation.DrawableRes
import com.example.caloscan.R
import com.example.caloscan.data.BuiltInFoods
import com.example.caloscan.data.FoodCategory
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.remote.OpenFoodFactsParser
import com.example.caloscan.domain.FoodSearch
import com.example.caloscan.util.plain
import java.time.LocalDate

/** The five screens in the bottom bar. */
enum class Tab(val title: String, val label: String, @param:DrawableRes val icon: Int) {
  DIARY("Дневник", "Дневник", R.drawable.ic_diary),
  FOODS("Продукты", "Продукты", R.drawable.ic_search),
  SCAN("Сканер штрихкодов", "Сканер", R.drawable.ic_scan),
  PROGRESS("Прогресс", "Прогресс", R.drawable.ic_chart),
  PROFILE("Профиль", "Профиль", R.drawable.ic_user),
}

/** The bottom sheet that is open, if any. */
sealed interface Sheet {
  /** Waiting for Open Food Facts. */
  data class Lookup(val code: String) : Sheet

  /** The barcode wasn't found, or the phone is offline. */
  data class Missing(val code: String, val reason: MissingReason) : Sheet

  /**
   * The product card after a scan: photo, details, "Добавить" and "Закрыть".
   * "Добавить" saves the product to "My foods".
   */
  data class Product(val food: Food) : Sheet

  /** Add a food to the diary, or edit an entry when [entry] is set. */
  data class AddFood(val food: Food, val date: LocalDate, val meal: Meal, val entry: DiaryEntry? = null) : Sheet

  /** Create or edit one of "My foods". */
  data class CustomFood(val draft: CustomFoodDraft) : Sheet
}

enum class MissingReason { NOT_FOUND, OFFLINE }

/** Starting values for the "New food" form. */
data class CustomFoodDraft(
  /** Set when editing an existing food. */
  val editingId: String? = null,
  val name: String = "",
  val brand: String = "",
  val barcode: String = "",
  val unit: String = "g",
  val kcal: String = "",
  val protein: String = "",
  val carbs: String = "",
  val fat: String = "",
  val servingLabel: String = "",
  val servingAmount: String = "",
  /** The form has no fields for these; they are kept from a scanned product. */
  val imageUrl: String = "",
  val packageSize: String = "",
  /** Opened after a scan: saving only adds the product to "My foods", not to the diary. */
  val fromScan: Boolean = false,
) {
  companion object {
    fun from(food: Food) = CustomFoodDraft(
      editingId = food.id,
      name = food.name,
      brand = food.brand,
      barcode = food.barcode,
      unit = food.unit,
      kcal = plain(food.kcal),
      protein = plain(food.protein),
      carbs = plain(food.carbs),
      fat = plain(food.fat),
      servingLabel = food.serving?.label.orEmpty(),
      servingAmount = food.serving?.amount?.let { plain(it) }.orEmpty(),
      imageUrl = food.imageUrl,
      packageSize = food.packageSize,
    )

    /**
     * A scanned product the user types in from the label: either unknown, or known
     * to Open Food Facts ([product]) but without nutrition facts.
     */
    fun fromScan(code: String, product: Food? = null) = CustomFoodDraft(
      barcode = code,
      name = product?.name?.takeIf { it != OpenFoodFactsParser.UNNAMED }.orEmpty(),
      brand = product?.brand.orEmpty(),
      unit = product?.unit ?: "g",
      imageUrl = product?.imageUrl.orEmpty(),
      packageSize = product?.packageSize.orEmpty(),
      fromScan = true,
    )
  }
}

/** A short message at the bottom of the screen, optionally with a button like "Undo". */
data class UiMessage(val text: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

/** Search state for Open Food Facts on the Foods screen. */
sealed interface OnlineSearch {
  val query: String

  data object Idle : OnlineSearch {
    override val query = ""
  }

  data class Loading(override val query: String) : OnlineSearch
  data class Done(override val query: String, val results: List<Food>) : OnlineSearch
  data class Failed(override val query: String) : OnlineSearch
}

/** The filter chips on the Foods screen. */
sealed class FoodFilter(val label: String) {
  data object All : FoodFilter("Все")
  data object Recent : FoodFilter("Недавние")
  data object Mine : FoodFilter("Мои продукты")
  data class Category(val category: FoodCategory) : FoodFilter(category.label)

  companion object {
    val all: List<FoodFilter> by lazy { listOf(All, Recent, Mine) + FoodCategory.entries.map { Category(it) } }
  }
}

data class FoodSection(val title: String, val foods: List<Food>)

data class FoodResults(val sections: List<FoodSection>, val emptyTitle: String? = null, val emptyText: String? = null)

/** What the Foods screen lists for the current search text and filter. */
fun buildFoodResults(
  query: String,
  filter: FoodFilter,
  customFoods: List<Food>,
  savedProducts: List<Food>,
  recentFoods: List<Food>,
): FoodResults {
  val q = query.trim()

  // Nothing typed: recent foods, my foods, then the whole database by category
  if (q.isEmpty() && filter == FoodFilter.All) {
    val sections = buildList {
      if (recentFoods.isNotEmpty()) add(FoodSection("Недавние", recentFoods.take(6)))
      if (customFoods.isNotEmpty()) add(FoodSection("Мои продукты", customFoods))
      FoodCategory.entries.forEach { add(FoodSection(it.label, BuiltInFoods.inCategory(it))) }
    }
    return FoodResults(sections)
  }

  val base = when (filter) {
    FoodFilter.All -> {
      // A scanned product that was added to "My foods" is listed once, as the user's own copy
      val myBarcodes = customFoods.map { it.barcode }.filter { it.isNotBlank() }.toSet()
      (customFoods + savedProducts.filter { it.barcode !in myBarcodes } + BuiltInFoods.all).distinctBy { it.id }
    }
    FoodFilter.Recent -> recentFoods
    FoodFilter.Mine -> customFoods
    is FoodFilter.Category -> BuiltInFoods.inCategory(filter.category)
  }
  val results = if (q.isNotEmpty()) FoodSearch.search(base, q) else base

  if (results.isNotEmpty()) {
    val title = when {
      q.isEmpty() -> filter.label
      filter == FoodFilter.All -> "Результаты"
      else -> "${filter.label} · результаты"
    }
    return FoodResults(listOf(FoodSection(title, results)))
  }

  return if (q.isNotEmpty()) {
    FoodResults(
      sections = emptyList(),
      emptyTitle = "Ничего не найдено по запросу «$q»" + if (filter == FoodFilter.All) "" else " в разделе «${filter.label}»",
      emptyText = "Поищите в онлайн-базе ниже, отсканируйте штрихкод или QR-код товара или создайте продукт сами.",
    )
  } else {
    FoodResults(
      sections = emptyList(),
      emptyTitle = if (filter == FoodFilter.Recent) "Недавних продуктов пока нет" else "Вы ещё не создали ни одного продукта",
      emptyText = "Здесь появятся продукты, которые вы добавите.",
    )
  }
}
