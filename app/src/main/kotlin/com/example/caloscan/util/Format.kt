package com.example.caloscan.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/* Numbers and dates in Russian style: "1 234", "0,4", "Вс, 1 нояб." */

val RUSSIAN: Locale = Locale.forLanguageTag("ru")

/** Non-breaking space, so "1 234" never breaks across two lines. */
private const val NBSP = '\u00A0'

/** Whole number with a space between thousands: 1234.4 → "1 234". */
fun fmt(n: Double): String = fmt(safe(n).roundToLong())

fun fmt(n: Int): String = fmt(n.toLong())

fun fmt(n: Long): String {
  val grouped = abs(n).toString().reversed().chunked(3).joinToString(NBSP.toString()).reversed()
  return if (n < 0) "-$grouped" else grouped
}

/** Grams with one decimal below 10 ("0,4"), whole numbers from 10 up ("12"). */
fun grams(n: Double): String {
  val v = safe(n)
  return if (v < 10) plain(v, decimals = 1) else v.roundToLong().toString()
}

/** A number without trailing zeros and with a decimal comma: 1.0 → "1", 0.50 → "0,5", 1.333 → "1,33". */
fun plain(n: Double, decimals: Int = 2): String =
  decimal(n).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')

/** Parses a number typed by the user. Accepts both "," and "." as the decimal sign. */
fun parseNumber(text: String): Double? =
  text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Parses a number and returns it only if it's between [min] and [max]. */
fun parseInRange(text: String, min: Double, max: Double): Double? =
  parseNumber(text)?.takeIf { it in min..max }

/** Rounds to one decimal: 0.35 → 0.4. */
fun round1(n: Double): Double = decimal(n).setScale(1, RoundingMode.HALF_UP).toDouble()

private fun safe(n: Double) = if (n.isFinite()) n else 0.0

// valueOf() uses the number as it's written (0.35), not its inexact binary value (0.34999…),
// so halves round up as expected
private fun decimal(n: Double): BigDecimal = BigDecimal.valueOf(safe(n))

/**
 * Russian plural form for a number:
 * plural(1, "день", "дня", "дней") = "день", 3 → "дня", 5 → "дней", 21 → "день".
 */
fun plural(n: Int, one: String, few: String, many: String): String {
  val lastTwo = abs(n) % 100
  val last = lastTwo % 10
  return when {
    lastTwo in 11..14 -> many
    last == 1 -> one
    last in 2..4 -> few
    else -> many
  }
}

private val unitWords = listOf(
  Regex("(\\d)\\s*(kg|kgs)(?!\\p{L})", RegexOption.IGNORE_CASE) to "$1 кг",
  Regex("(\\d)\\s*(g|gr|grams?)(?!\\p{L})", RegexOption.IGNORE_CASE) to "$1 г",
  Regex("(\\d)\\s*ml(?!\\p{L})", RegexOption.IGNORE_CASE) to "$1 мл",
  Regex("(\\d)\\s*l(?!\\p{L})", RegexOption.IGNORE_CASE) to "$1 л",
)

/** "15 g" → "15 г", "1.5 l" → "1.5 л". Used for package and serving sizes from Open Food Facts. */
fun russianUnits(text: String): String =
  unitWords.fold(text) { result, (regex, replacement) -> regex.replace(result, replacement) }

// ---------- Dates ----------

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM", RUSSIAN)

/** "Сегодня", "Вчера", "Завтра" or "Вс, 1 нояб.". */
fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
  today -> "Сегодня"
  today.minusDays(1) -> "Вчера"
  today.plusDays(1) -> "Завтра"
  else -> date.format(dayFormat).replaceFirstChar { it.titlecase(RUSSIAN) }
}

/** "Пн", "Вт"... for the chart. */
fun shortWeekday(day: DayOfWeek): String =
  day.getDisplayName(TextStyle.SHORT, RUSSIAN).replaceFirstChar { it.titlecase(RUSSIAN) }

// ---------- Search ----------

private val combiningMarks = Regex("\\p{Mn}+")

/** Lowercase without accents, so "Ёжик" matches "ежик" and "Café" matches "cafe". */
fun normalizeForSearch(text: String): String =
  combiningMarks.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")
