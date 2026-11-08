package com.example.caloscan.data

import androidx.room.withTransaction
import com.example.caloscan.data.local.AppDatabase
import com.example.caloscan.data.local.BackupData
import com.example.caloscan.data.local.BackupJson
import com.example.caloscan.data.local.DiaryEntryEntity
import com.example.caloscan.data.local.FoodEntity
import com.example.caloscan.data.local.RecentFoodEntity
import com.example.caloscan.data.local.toEntity
import com.example.caloscan.data.local.toEntry
import com.example.caloscan.data.local.toFood
import com.example.caloscan.data.local.toProfile
import com.example.caloscan.data.local.toSnapshot
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.remote.OpenFoodFactsApi
import com.example.caloscan.domain.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/**
 * The single place the screens get data from. It hides whether data comes from
 * the database on the phone, the built-in food list, or Open Food Facts.
 */
class FoodRepository(
  private val db: AppDatabase,
  private val api: OpenFoodFactsApi,
) {
  private val diaryDao = db.diaryDao()
  private val foodDao = db.foodDao()
  private val recentDao = db.recentDao()
  private val profileDao = db.profileDao()

  // ---------- Profile ----------

  val profile: Flow<Profile> = profileDao.observe().map { it?.toProfile() ?: Profile() }

  suspend fun saveProfile(profile: Profile) = profileDao.upsert(profile.toEntity())

  // ---------- Diary ----------

  fun entriesOn(date: LocalDate): Flow<List<DiaryEntry>> =
    diaryDao.observeDay(date.toString()).map { rows -> rows.map { it.toEntry() } }

  fun entriesBetween(from: LocalDate, to: LocalDate): Flow<List<DiaryEntry>> =
    diaryDao.observeRange(from.toString(), to.toString()).map { rows -> rows.map { it.toEntry() } }

  val loggedDates: Flow<Set<LocalDate>> =
    diaryDao.observeLoggedDates().map { dates -> dates.map(LocalDate::parse).toSet() }

  suspend fun addEntry(date: LocalDate, food: Food, amount: Double, meal: Meal): DiaryEntry {
    val now = System.currentTimeMillis()
    // Products from Open Food Facts are saved so they work offline and show up in "Recent"
    if (food.source == FoodSource.OPEN_FOOD_FACTS) cacheProduct(food)
    val row = DiaryEntryEntity(date = date.toString(), meal = meal.id, amount = amount, addedAt = now, food = food.toSnapshot())
    val id = diaryDao.insert(row)
    recentDao.upsert(RecentFoodEntity(food.id, now))
    recentDao.trim(MAX_RECENT)
    return row.copy(id = id).toEntry()
  }

  suspend fun updateEntry(entry: DiaryEntry) = diaryDao.updateAmountAndMeal(entry.id, entry.amount, entry.meal.id)

  suspend fun deleteEntry(entry: DiaryEntry) = diaryDao.delete(entry.id)

  /** Puts a deleted entry back (for "Undo"). */
  suspend fun restoreEntry(entry: DiaryEntry) {
    diaryDao.insert(entry.toEntity())
  }

  // ---------- Foods ----------

  val customFoods: Flow<List<Food>> =
    foodDao.observeBySource(FoodSource.CUSTOM.id).map { rows -> rows.map { it.toFood() } }

  /** Products scanned or added before, newest first. */
  val savedProducts: Flow<List<Food>> =
    foodDao.observeBySource(FoodSource.OPEN_FOOD_FACTS.id).map { rows -> rows.map { it.toFood() } }

  /** The last foods added to the diary, newest first. */
  val recentFoods: Flow<List<Food>> =
    combine(recentDao.observe(MAX_RECENT), foodDao.observeAll()) { recent, saved ->
      val savedById = saved.associateBy(FoodEntity::id)
      recent.mapNotNull { BuiltInFoods.get(it.foodId) ?: savedById[it.foodId]?.toFood() }
    }

  /** Saves a new or edited food to "My foods" and returns it with its id. */
  suspend fun saveCustomFood(food: Food): Food {
    val existing = if (food.id.isNotBlank()) foodDao.get(food.id) else null
    val saved = food.copy(
      id = food.id.ifBlank { newId("c-") },
      source = FoodSource.CUSTOM,
      emoji = food.emoji.ifBlank { "✏️" },
    )
    // Editing keeps the food's place in the list
    foodDao.upsert(saved.toEntity(savedAt = existing?.savedAt ?: System.currentTimeMillis()))
    return saved
  }

  /**
   * Adds a scanned product to "My foods" as the user's own copy (with its photo),
   * so it can be edited and is found first the next time it's scanned.
   * Returns the saved food, or the one already in "My foods" with the same barcode.
   */
  suspend fun addToLibrary(product: Food): Food {
    if (product.source == FoodSource.CUSTOM) return product
    if (product.barcode.isNotBlank()) {
      foodDao.findByBarcode(product.barcode)
        ?.takeIf { it.source == FoodSource.CUSTOM.id }
        ?.let { return it.toFood() }
    }
    return saveCustomFood(product.copy(id = ""))
  }

  suspend fun deleteCustomFood(id: String) {
    foodDao.delete(id)
    recentDao.delete(id)
  }

  // ---------- Barcodes ----------

  /** Looks in the user's own foods and previously scanned products. Works offline. */
  suspend fun findSavedByBarcode(code: String): Food? {
    if (code.isBlank()) return null
    val row = foodDao.findByBarcode(code) ?: if (code.length == 12) foodDao.findByBarcode("0$code") else null
    return row?.toFood()
  }

  /** Asks Open Food Facts. Throws IOException when offline. */
  suspend fun lookupOnline(code: String): Food? {
    // A 12-digit UPC-A code is the same product as the 13-digit EAN code with a leading 0
    return api.getByBarcode(code) ?: if (code.length == 12) api.getByBarcode("0$code") else null
  }

  suspend fun cacheProduct(food: Food) {
    if (food.barcode.isBlank()) return
    foodDao.upsert(food.toEntity(savedAt = System.currentTimeMillis()))
    foodDao.trimProducts(MAX_PRODUCTS)
  }

  suspend fun searchOnline(text: String): List<Food> = api.search(text)

  // ---------- Backup ----------

  suspend fun exportJson(): String = BackupJson.write(
    BackupData(profileDao.get(), foodDao.all(), diaryDao.all(), recentDao.all()),
    exportedAt = Instant.now().toString(),
  )

  /** Replaces all data with a backup. Throws IllegalArgumentException for a bad file. */
  suspend fun importJson(text: String) {
    val data = BackupJson.read(text)
    db.withTransaction {
      clearAll()
      data.profile?.let { profileDao.upsert(it) }
      foodDao.upsertAll(data.foods)
      diaryDao.insertAll(data.entries)
      recentDao.upsertAll(data.recent)
    }
  }

  suspend fun deleteAll() = db.withTransaction { clearAll() }

  private suspend fun clearAll() {
    diaryDao.clear()
    foodDao.clear()
    recentDao.clear()
    profileDao.clear()
  }

  private fun newId(prefix: String) =
    prefix + System.currentTimeMillis().toString(36) + Random.nextInt(0, 46656).toString(36)

  companion object {
    const val MAX_RECENT = 20
    const val MAX_PRODUCTS = 300
  }
}
