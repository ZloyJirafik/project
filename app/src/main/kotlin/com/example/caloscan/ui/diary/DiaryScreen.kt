package com.example.caloscan.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caloscan.R
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.model.Nutrition
import com.example.caloscan.data.model.total
import com.example.caloscan.domain.CalorieCalculator
import com.example.caloscan.domain.Targets
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.Tab
import com.example.caloscan.ui.components.CaloCard
import com.example.caloscan.ui.components.CaloIcon
import com.example.caloscan.ui.components.CalorieRing
import com.example.caloscan.ui.components.CardTitle
import com.example.caloscan.ui.components.FoodAvatar
import com.example.caloscan.ui.components.ListRow
import com.example.caloscan.ui.components.MacroBar
import com.example.caloscan.ui.components.MutedText
import com.example.caloscan.ui.preview.SampleData
import com.example.caloscan.ui.preview.PreviewScreen
import com.example.caloscan.ui.theme.CaloTheme
import com.example.caloscan.util.fmt
import com.example.caloscan.util.grams
import kotlin.math.roundToInt

@Composable
fun DiaryRoute(vm: CaloScanViewModel) {
  val entries by vm.dayEntries.collectAsStateWithLifecycle()
  val targets by vm.targets.collectAsStateWithLifecycle()
  DiaryScreen(
    entries = entries,
    targets = targets,
    onAddToMeal = vm::startAdding,
    onOpenEntry = vm::openEntry,
    onSetUp = { vm.selectTab(Tab.PROFILE) },
  )
}

/** Today's calories, macros and meals. */
@Composable
fun DiaryScreen(
  entries: List<DiaryEntry>,
  targets: Targets,
  onAddToMeal: (Meal) -> Unit,
  onOpenEntry: (DiaryEntry) -> Unit,
  onSetUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val total = entries.total()
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { SummaryCard(total, targets) }
    if (!targets.isPersonal) item { SetupCard(onSetUp) }
    items(Meal.entries.toList(), key = { it.id }) { meal ->
      MealCard(
        meal = meal,
        entries = entries.filter { it.meal == meal },
        onAdd = { onAddToMeal(meal) },
        onOpenEntry = onOpenEntry,
      )
    }
  }
}

@Composable
private fun SummaryCard(total: Nutrition, targets: Targets) {
  val percent = if (targets.kcal > 0) (total.kcal / targets.kcal * 100).roundToInt() else 0
  CaloCard {
    Row(
      Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      CalorieRing(eaten = total.kcal, goal = targets.kcal)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("Цель на день", fmt(targets.kcal))
        Stat("Съедено", fmt(total.kcal))
        HorizontalDivider(color = CaloTheme.colors.border)
        Stat("От цели", "$percent%")
      }
    }
    Row(
      Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MacroBar("Белки", CaloTheme.colors.protein, total.protein, targets.protein, Modifier.weight(1f))
      MacroBar("Жиры", CaloTheme.colors.fat, total.fat, targets.fat, Modifier.weight(1f))
      MacroBar("Углеводы", CaloTheme.colors.carbs, total.carbs, targets.carbs, Modifier.weight(1f))
    }
  }
}

@Composable
private fun Stat(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium, color = CaloTheme.colors.muted)
    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun SetupCard(onSetUp: () -> Unit) {
  CaloCard {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f).padding(end = 12.dp)) {
        CardTitle("Рассчитайте свою норму")
        MutedText("Пока используется ${fmt(CalorieCalculator.DEFAULT_TARGET)} ккал. Укажите свои данные в профиле.")
      }
      Button(onClick = onSetUp) { Text("Указать") }
    }
  }
}

@Composable
private fun MealCard(
  meal: Meal,
  entries: List<DiaryEntry>,
  onAdd: () -> Unit,
  onOpenEntry: (DiaryEntry) -> Unit,
) {
  CaloCard {
    Row(
      Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        CardTitle(meal.label)
        MutedText(if (entries.isEmpty()) "Ничего не добавлено" else "${fmt(entries.total().kcal)} ккал")
      }
      FilledTonalIconButton(onClick = onAdd) {
        CaloIcon(R.drawable.ic_plus, contentDescription = "Добавить продукт в ${meal.into}")
      }
    }
    entries.forEach { entry ->
      HorizontalDivider(color = CaloTheme.colors.border)
      EntryRow(entry) { onOpenEntry(entry) }
    }
  }
}

@Composable
private fun EntryRow(entry: DiaryEntry, onClick: () -> Unit) {
  val n = entry.nutrition
  ListRow(
    title = entry.food.name,
    subtitle = "${grams(entry.amount)} ${entry.food.unitLabel} · Б ${grams(n.protein)} · Ж ${grams(n.fat)} · У ${grams(n.carbs)}",
    onClick = onClick,
    leading = { FoodAvatar(entry.food.emoji, entry.food.imageUrl) },
    trailing = { Text(fmt(n.kcal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
  )
}

@Preview(name = "Diary", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun DiaryPreview() {
  PreviewScreen {
    DiaryScreen(SampleData.todayEntries, SampleData.targets, {}, {}, {})
  }
}

@Preview(name = "Diary – new user", showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun DiaryEmptyPreview() {
  PreviewScreen {
    DiaryScreen(emptyList(), SampleData.defaultTargets, {}, {}, {})
  }
}
