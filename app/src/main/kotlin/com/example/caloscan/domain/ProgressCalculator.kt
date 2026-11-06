package com.example.caloscan.domain

import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Nutrition
import com.example.caloscan.data.model.total
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

data class DaySummary(val date: LocalDate, val total: Nutrition, val logged: Boolean)

/** Share of calories from each macro, in percent. */
data class MacroShare(val protein: Int, val carbs: Int, val fat: Int)

data class ProgressSummary(
  val days: List<DaySummary>,
  val loggedDays: Int,
  val averageKcal: Double,
  val averageProtein: Double,
  /** Logged days within ±10% of the target. */
  val onTargetDays: Int,
  val streak: Int,
  val macroShare: MacroShare?,
  val targets: Targets,
)

object ProgressCalculator {

  fun summarize(
    rangeDays: Int,
    today: LocalDate,
    entries: List<DiaryEntry>,
    loggedDates: Set<LocalDate>,
    targets: Targets,
  ): ProgressSummary {
    val byDate = entries.groupBy { it.date }
    val days = (rangeDays - 1 downTo 0).map { back ->
      val date = today.minusDays(back.toLong())
      val list = byDate[date].orEmpty()
      DaySummary(date, list.total(), list.isNotEmpty())
    }
    val logged = days.filter { it.logged }

    fun average(field: (Nutrition) -> Double) =
      if (logged.isEmpty()) 0.0 else logged.sumOf { field(it.total) } / logged.size

    val onTarget = logged.count { abs(it.total.kcal - targets.kcal) <= targets.kcal * 0.1 }

    val proteinKcal = average { it.protein } * 4
    val carbsKcal = average { it.carbs } * 4
    val fatKcal = average { it.fat } * 9
    val sum = proteinKcal + carbsKcal + fatKcal
    val share = if (sum > 0) {
      MacroShare(
        protein = (proteinKcal / sum * 100).roundToInt(),
        carbs = (carbsKcal / sum * 100).roundToInt(),
        fat = (fatKcal / sum * 100).roundToInt(),
      )
    } else {
      null
    }

    return ProgressSummary(
      days = days,
      loggedDays = logged.size,
      averageKcal = average { it.kcal },
      averageProtein = average { it.protein },
      onTargetDays = onTarget,
      streak = streak(today, loggedDates),
      macroShare = share,
      targets = targets,
    )
  }

  /** Days logged in a row, ending today (or yesterday if nothing is logged today yet). */
  fun streak(today: LocalDate, loggedDates: Set<LocalDate>): Int {
    var day = if (today in loggedDates) today else today.minusDays(1)
    var count = 0
    while (day in loggedDates) {
      count++
      day = day.minusDays(1)
    }
    return count
  }
}
