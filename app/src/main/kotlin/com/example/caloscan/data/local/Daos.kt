package com.example.caloscan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/*
 * Database queries. Functions that return Flow update automatically
 * whenever the table changes, so the screens always show current data.
 */

@Dao
interface DiaryDao {
  @Query("SELECT * FROM diary_entries WHERE date = :date ORDER BY addedAt")
  fun observeDay(date: String): Flow<List<DiaryEntryEntity>>

  @Query("SELECT * FROM diary_entries WHERE date BETWEEN :from AND :to ORDER BY date, addedAt")
  fun observeRange(from: String, to: String): Flow<List<DiaryEntryEntity>>

  @Query("SELECT DISTINCT date FROM diary_entries")
  fun observeLoggedDates(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: DiaryEntryEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(entries: List<DiaryEntryEntity>)

  @Query("UPDATE diary_entries SET amount = :amount, meal = :meal WHERE id = :id")
  suspend fun updateAmountAndMeal(id: Long, amount: Double, meal: String)

  @Query("DELETE FROM diary_entries WHERE id = :id")
  suspend fun delete(id: Long)

  @Query("SELECT * FROM diary_entries ORDER BY date, addedAt")
  suspend fun all(): List<DiaryEntryEntity>

  @Query("DELETE FROM diary_entries")
  suspend fun clear()
}

@Dao
interface FoodDao {
  @Query("SELECT * FROM foods WHERE source = :source ORDER BY savedAt DESC")
  fun observeBySource(source: String): Flow<List<FoodEntity>>

  @Query("SELECT * FROM foods")
  fun observeAll(): Flow<List<FoodEntity>>

  /** The user's own food wins over a cached Open Food Facts product with the same barcode. */
  @Query(
    "SELECT * FROM foods WHERE barcode = :code " +
      "ORDER BY CASE source WHEN 'custom' THEN 0 ELSE 1 END, savedAt DESC LIMIT 1",
  )
  suspend fun findByBarcode(code: String): FoodEntity?

  @Query("SELECT * FROM foods WHERE id = :id")
  suspend fun get(id: String): FoodEntity?

  @Upsert
  suspend fun upsert(food: FoodEntity)

  @Upsert
  suspend fun upsertAll(foods: List<FoodEntity>)

  @Query("DELETE FROM foods WHERE id = :id")
  suspend fun delete(id: String)

  /** Keeps only the newest [keep] scanned products. */
  @Query(
    "DELETE FROM foods WHERE source = 'off' AND id NOT IN " +
      "(SELECT id FROM foods WHERE source = 'off' ORDER BY savedAt DESC LIMIT :keep)",
  )
  suspend fun trimProducts(keep: Int)

  @Query("SELECT * FROM foods")
  suspend fun all(): List<FoodEntity>

  @Query("DELETE FROM foods")
  suspend fun clear()
}

@Dao
interface RecentDao {
  @Query("SELECT * FROM recent_foods ORDER BY usedAt DESC LIMIT :limit")
  fun observe(limit: Int): Flow<List<RecentFoodEntity>>

  @Upsert
  suspend fun upsert(recent: RecentFoodEntity)

  @Upsert
  suspend fun upsertAll(recent: List<RecentFoodEntity>)

  @Query("DELETE FROM recent_foods WHERE foodId = :foodId")
  suspend fun delete(foodId: String)

  @Query(
    "DELETE FROM recent_foods WHERE foodId NOT IN " +
      "(SELECT foodId FROM recent_foods ORDER BY usedAt DESC LIMIT :keep)",
  )
  suspend fun trim(keep: Int)

  @Query("SELECT * FROM recent_foods ORDER BY usedAt DESC")
  suspend fun all(): List<RecentFoodEntity>

  @Query("DELETE FROM recent_foods")
  suspend fun clear()
}

@Dao
interface ProfileDao {
  @Query("SELECT * FROM profile WHERE id = 1")
  fun observe(): Flow<ProfileEntity?>

  @Query("SELECT * FROM profile WHERE id = 1")
  suspend fun get(): ProfileEntity?

  @Upsert
  suspend fun upsert(profile: ProfileEntity)

  @Query("DELETE FROM profile")
  suspend fun clear()
}
