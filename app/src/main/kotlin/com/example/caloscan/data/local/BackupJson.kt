package com.example.caloscan.data.local

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Everything in the database, for "Export backup" / "Import backup". */
data class BackupData(
  val profile: ProfileEntity?,
  val foods: List<FoodEntity>,
  val entries: List<DiaryEntryEntity>,
  val recent: List<RecentFoodEntity>,
)

/**
 * Backup file format (JSON):
 * {
 *   "app": "MyBody", "version": 1, "exportedAt": "2026-11-01T12:00:00Z",
 *   "profile": {...} or null, "foods": [...], "entries": [...], "recent": [...]
 * }
 */
object BackupJson {
  private const val APP = "MyBody"
  /** The app was called CaloScan before. Its backups have the same format and still work. */
  private val ACCEPTED_APPS = setOf(APP, "CaloScan")
  private const val VERSION = 1

  fun write(data: BackupData, exportedAt: String): String {
    val root = JSONObject()
      .put("app", APP)
      .put("version", VERSION)
      .put("exportedAt", exportedAt)
      .put("profile", data.profile?.let(::profileJson) ?: JSONObject.NULL)
      .put("foods", JSONArray(data.foods.map(::foodJson)))
      .put("entries", JSONArray(data.entries.map(::entryJson)))
      .put("recent", JSONArray(data.recent.map { JSONObject().put("foodId", it.foodId).put("usedAt", it.usedAt) }))
    return root.toString(2)
  }

  /** Throws [IllegalArgumentException] with a readable message if the file isn't a backup. */
  fun read(text: String): BackupData {
    val root = try {
      JSONObject(text)
    } catch (e: JSONException) {
      throw IllegalArgumentException(NOT_A_BACKUP, e)
    }
    require(root.optString("app") in ACCEPTED_APPS && root.has("entries")) { NOT_A_BACKUP }
    try {
      return BackupData(
        profile = root.optJSONObject("profile")?.let(::profileFrom),
        foods = root.optJSONArray("foods").objects().map(::foodFrom),
        entries = root.optJSONArray("entries").objects().map(::entryFrom),
        recent = root.optJSONArray("recent").objects().map { RecentFoodEntity(it.getString("foodId"), it.getLong("usedAt")) },
      )
    } catch (e: JSONException) {
      throw IllegalArgumentException("Файл резервной копии повреждён.", e)
    }
  }

  private const val NOT_A_BACKUP = "Это не резервная копия MyBody."

  private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

  // org.json leaves out null values, so optional fields are read with these helpers
  private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
  private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)
  private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
  private fun Any?.orJsonNull(): Any = this ?: JSONObject.NULL

  private fun profileJson(p: ProfileEntity) = JSONObject()
    .put("sex", p.sex.orJsonNull())
    .put("age", p.age.orJsonNull())
    .put("heightCm", p.heightCm.orJsonNull())
    .put("weightKg", p.weightKg.orJsonNull())
    .put("activity", p.activity)
    .put("goal", p.goal)
    .put("macroSplit", p.macroSplit)
    .put("customTarget", p.customTarget.orJsonNull())

  private fun profileFrom(o: JSONObject) = ProfileEntity(
    sex = o.optStringOrNull("sex"),
    age = o.optIntOrNull("age"),
    heightCm = o.optDoubleOrNull("heightCm"),
    weightKg = o.optDoubleOrNull("weightKg"),
    activity = o.getString("activity"),
    goal = o.getString("goal"),
    macroSplit = o.getString("macroSplit"),
    customTarget = o.optIntOrNull("customTarget"),
  )

  private fun foodJson(f: FoodEntity) = JSONObject()
    .put("id", f.id)
    .put("name", f.name)
    .put("brand", f.brand)
    .put("category", f.category)
    .put("kcal", f.kcal)
    .put("protein", f.protein)
    .put("carbs", f.carbs)
    .put("fat", f.fat)
    .put("servingLabel", f.servingLabel.orJsonNull())
    .put("servingAmount", f.servingAmount.orJsonNull())
    .put("unit", f.unit)
    .put("emoji", f.emoji)
    .put("source", f.source)
    .put("barcode", f.barcode)
    .put("imageUrl", f.imageUrl)
    .put("packageSize", f.packageSize)
    .put("savedAt", f.savedAt)

  private fun foodFrom(o: JSONObject) = FoodEntity(
    id = o.getString("id"),
    name = o.getString("name"),
    brand = o.optString("brand"),
    category = o.optString("category"),
    kcal = o.getDouble("kcal"),
    protein = o.getDouble("protein"),
    carbs = o.getDouble("carbs"),
    fat = o.getDouble("fat"),
    servingLabel = o.optStringOrNull("servingLabel"),
    servingAmount = o.optDoubleOrNull("servingAmount"),
    unit = o.optString("unit", "g"),
    emoji = o.optString("emoji"),
    source = o.getString("source"),
    barcode = o.optString("barcode"),
    imageUrl = o.optString("imageUrl"),
    packageSize = o.optString("packageSize"),
    savedAt = o.optLong("savedAt"),
  )

  private fun entryJson(e: DiaryEntryEntity) = JSONObject()
    .put("id", e.id)
    .put("date", e.date)
    .put("meal", e.meal)
    .put("amount", e.amount)
    .put("addedAt", e.addedAt)
    .put(
      "food",
      JSONObject()
        .put("id", e.food.id)
        .put("name", e.food.name)
        .put("brand", e.food.brand)
        .put("kcal", e.food.kcal)
        .put("protein", e.food.protein)
        .put("carbs", e.food.carbs)
        .put("fat", e.food.fat)
        .put("servingLabel", e.food.servingLabel.orJsonNull())
        .put("servingAmount", e.food.servingAmount.orJsonNull())
        .put("unit", e.food.unit)
        .put("emoji", e.food.emoji)
        .put("source", e.food.source)
        .put("barcode", e.food.barcode)
        .put("imageUrl", e.food.imageUrl),
    )

  private fun entryFrom(o: JSONObject): DiaryEntryEntity {
    val f = o.getJSONObject("food")
    return DiaryEntryEntity(
      id = o.getLong("id"),
      date = o.getString("date"),
      meal = o.getString("meal"),
      amount = o.getDouble("amount"),
      addedAt = o.getLong("addedAt"),
      food = FoodSnapshot(
        id = f.getString("id"),
        name = f.getString("name"),
        brand = f.optString("brand"),
        kcal = f.getDouble("kcal"),
        protein = f.getDouble("protein"),
        carbs = f.getDouble("carbs"),
        fat = f.getDouble("fat"),
        servingLabel = f.optStringOrNull("servingLabel"),
        servingAmount = f.optDoubleOrNull("servingAmount"),
        unit = f.optString("unit", "g"),
        emoji = f.optString("emoji"),
        source = f.getString("source"),
        barcode = f.optString("barcode"),
        imageUrl = f.optString("imageUrl"),
      ),
    )
  }
}
