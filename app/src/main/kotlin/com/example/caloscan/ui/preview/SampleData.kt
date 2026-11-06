package com.example.caloscan.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.caloscan.data.BuiltInFoods
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.model.Serving
import com.example.caloscan.domain.ActivityLevel
import com.example.caloscan.domain.CalorieCalculator
import com.example.caloscan.domain.Goal
import com.example.caloscan.domain.Profile
import com.example.caloscan.domain.ProgressCalculator
import com.example.caloscan.domain.Sex
import com.example.caloscan.ui.theme.CaloScanTheme
import java.time.LocalDate

/** Example data for the @Preview functions (Android Studio's design view). */
object SampleData {
  val today: LocalDate = LocalDate.of(2026, 11, 1)

  // Built-in foods are looked up by their English name, which stays the same
  private fun food(englishName: String) = BuiltInFoods.all.first { it.searchTerms == englishName }

  val nutella = Food(
    id = "off-3017620422003",
    name = "Nutella",
    brand = "Ferrero",
    kcal = 539.0,
    protein = 6.3,
    carbs = 57.5,
    fat = 30.9,
    serving = Serving("15 г", 15.0),
    packageSize = "400 г",
    emoji = "🏷️",
    barcode = "3017620422003",
    source = FoodSource.OPEN_FOOD_FACTS,
  )

  val profile = Profile(
    sex = Sex.MALE,
    age = 21,
    heightCm = 180.0,
    weightKg = 75.0,
    activity = ActivityLevel.MODERATE,
    goal = Goal.LOSE_FAST,
  )
  val targets = CalorieCalculator.targets(profile)
  val defaultTargets = CalorieCalculator.targets(Profile())

  private var nextId = 1L
  private fun entry(date: LocalDate, meal: Meal, food: Food, amount: Double) =
    DiaryEntry(nextId++, date, meal, amount, nextId, food)

  val todayEntries: List<DiaryEntry> = listOf(
    entry(today, Meal.BREAKFAST, food("Oats, rolled (dry)"), 60.0),
    entry(today, Meal.BREAKFAST, food("Banana"), 118.0),
    entry(today, Meal.BREAKFAST, food("Milk, 2%"), 200.0),
    entry(today, Meal.LUNCH, food("Chicken breast, cooked"), 150.0),
    entry(today, Meal.LUNCH, food("White rice, cooked"), 180.0),
    entry(today, Meal.LUNCH, food("Broccoli"), 91.0),
    entry(today, Meal.SNACK, nutella, 15.0),
  )

  /** A week with a few days logged, one of them over the goal. */
  private val weekEntries: List<DiaryEntry> = todayEntries + listOf(
    entry(today.minusDays(1), Meal.LUNCH, food("Pizza, cheese"), 535.0),
    entry(today.minusDays(1), Meal.DINNER, food("Caesar salad with chicken"), 300.0),
    entry(today.minusDays(1), Meal.SNACK, food("Cola"), 330.0),
    entry(today.minusDays(2), Meal.BREAKFAST, food("Greek yogurt, plain 0%"), 170.0),
    entry(today.minusDays(2), Meal.LUNCH, food("Spaghetti bolognese"), 450.0),
    entry(today.minusDays(2), Meal.DINNER, food("Salmon, cooked"), 150.0),
    entry(today.minusDays(2), Meal.DINNER, food("Potato, boiled"), 340.0),
    entry(today.minusDays(4), Meal.LUNCH, food("Hamburger"), 300.0),
    entry(today.minusDays(4), Meal.DINNER, food("French fries"), 234.0),
    entry(today.minusDays(4), Meal.SNACK, food("Milk chocolate"), 86.0),
    entry(today.minusDays(4), Meal.SNACK, food("Beer"), 660.0),
    entry(today.minusDays(5), Meal.LUNCH, food("Burrito, beef & bean"), 440.0),
    entry(today.minusDays(5), Meal.DINNER, food("Omelette (plain)"), 240.0),
    entry(today.minusDays(5), Meal.DINNER, food("Whole wheat bread"), 96.0),
  )

  val progress = ProgressCalculator.summarize(
    rangeDays = 7,
    today = today,
    entries = weekEntries,
    loggedDates = weekEntries.map { it.date }.toSet(),
    targets = targets,
  )

  val recentFoods = listOf(food("Banana"), food("Chicken breast, cooked"), nutella)

  val customFoods = listOf(
    Food(
      id = "c-1",
      name = "Бабушкины блины",
      kcal = 231.0,
      protein = 6.4,
      carbs = 28.0,
      fat = 9.9,
      serving = Serving("1 блин", 60.0),
      emoji = "✏️",
      source = FoodSource.CUSTOM,
    ),
  )
}

/** Theme + app background for previews of a single screen. */
@Composable
fun PreviewScreen(darkTheme: Boolean = false, content: @Composable () -> Unit) {
  CaloScanTheme(darkTheme = darkTheme) {
    Surface(color = MaterialTheme.colorScheme.background, content = content)
  }
}
