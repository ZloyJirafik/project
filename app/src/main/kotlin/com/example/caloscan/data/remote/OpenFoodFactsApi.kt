package com.example.caloscan.data.remote

import com.example.caloscan.data.model.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

/**
 * Client for Open Food Facts (https://world.openfoodfacts.org), a free, open
 * database of millions of packaged foods. No API key is needed.
 *
 * Both functions return foods in the app's own format (values per 100 g / 100 ml)
 * and throw [IOException] when the phone is offline or the server fails.
 */
class OpenFoodFactsApi(
  private val baseUrl: String = "https://world.openfoodfacts.org",
  // Open Food Facts asks apps to identify themselves
  private val userAgent: String = "MyBody-Android/1.0 (college project)",
) {

  /** The product with this barcode, or null if it isn't in the database. */
  suspend fun getByBarcode(code: String): Food? = withContext(Dispatchers.IO) {
    val url = "$baseUrl/api/v2/product/${encode(code)}.json?fields=$FIELDS"
    val json = getJson(url, timeoutMs = 12_000) ?: return@withContext null
    val product = json.optJSONObject("product")
    if (json.optInt("status") != 1 || product == null) return@withContext null
    OpenFoodFactsParser.toFood(product, fallbackCode = code)
  }

  /**
   * Products whose name matches [text]. Only products with nutrition facts are returned.
   * The search server is sometimes busy (HTTP 503), so it tries up to 3 times.
   */
  suspend fun search(text: String): List<Food> = withContext(Dispatchers.IO) {
    // lc=ru: search and return product names in Russian where available
    val url = "$baseUrl/cgi/search.pl?search_simple=1&action=process&json=1&page_size=30&lc=ru" +
      "&search_terms=${encode(text)}&fields=$FIELDS"
    val json = retry(times = 3) { getJson(url, timeoutMs = 20_000) }
    val products = json?.optJSONArray("products") ?: return@withContext emptyList()
    (0 until products.length())
      .mapNotNull { products.optJSONObject(it) }
      .filter { it.optString("code").isNotBlank() }
      .map { OpenFoodFactsParser.toFood(it) }
      .filter { it.hasNutrition && it.name != OpenFoodFactsParser.UNNAMED }
  }

  private suspend fun <T> retry(times: Int, block: () -> T): T {
    for (attempt in 1..times) {
      try {
        return block()
      } catch (e: IOException) {
        // A timeout means the server is too slow; trying again won't help
        if (attempt == times || e is SocketTimeoutException) throw e
        delay(1500L * attempt)
      }
    }
    error("unreachable")
  }

  /** GETs a URL and parses the JSON. Returns null for HTTP 404 (not found). */
  private fun getJson(url: String, timeoutMs: Int): JSONObject? {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = timeoutMs
      connection.readTimeout = timeoutMs
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestProperty("User-Agent", userAgent)
      val status = connection.responseCode
      if (status == HttpURLConnection.HTTP_NOT_FOUND) return null
      if (status !in 200..299) throw IOException("Server error $status")
      val body = connection.inputStream.bufferedReader().use { it.readText() }
      return JSONObject(body)
    } catch (e: JSONException) {
      throw IOException("Unexpected response from Open Food Facts", e)
    } finally {
      connection.disconnect()
    }
  }

  private fun encode(text: String) = URLEncoder.encode(text, "UTF-8")

  companion object {
    private const val FIELDS = "code,product_name,product_name_ru,product_name_en,generic_name,generic_name_ru," +
      "brands,nutriments,serving_size,serving_quantity,quantity,image_front_small_url"
  }
}
