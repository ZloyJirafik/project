package com.example.caloscan.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caloscan.domain.DaySummary
import com.example.caloscan.domain.MacroShare
import com.example.caloscan.domain.ProgressSummary
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.components.CaloCard
import com.example.caloscan.ui.components.CardTitle
import com.example.caloscan.ui.components.ListRow
import com.example.caloscan.ui.components.MutedText
import com.example.caloscan.ui.components.RowDivider
import com.example.caloscan.ui.components.Segmented
import com.example.caloscan.ui.components.SectionTitle
import com.example.caloscan.ui.preview.PreviewScreen
import com.example.caloscan.ui.preview.SampleData
import com.example.caloscan.ui.theme.CaloTheme
import com.example.caloscan.util.dayLabel
import com.example.caloscan.util.fmt
import com.example.caloscan.util.plural
import com.example.caloscan.util.shortWeekday
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProgressRoute(vm: CaloScanViewModel) {
  val summary by vm.progress.collectAsStateWithLifecycle()
  val range by vm.range.collectAsStateWithLifecycle()
  ProgressScreen(summary, range, vm::setRange, vm::openDay)
}

/** Calories chart and statistics for the last 7 or 30 days. */
@Composable
fun ProgressScreen(
  summary: ProgressSummary?,
  range: Int,
  onRangeChange: (Int) -> Unit,
  onOpenDay: (LocalDate) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { Segmented(listOf(7, 30), range, { "За $it дней" }, onRangeChange) }
    if (summary == null) {
      item {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      }
      return@LazyColumn
    }
    item { ChartCard(summary) }
    item { StatsGrid(summary) }
    item { MacroCard(summary) }
    item { DailyLogCard(summary, onOpenDay) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartCard(summary: ProgressSummary) {
  CaloCard {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      CardTitle("Калории по дням")
      CaloriesChart(summary.days, summary.targets.kcal, Modifier.fillMaxWidth().height(190.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendItem("Съедено", CaloTheme.colors.good)
        LegendItem("Больше нормы на 10%+", CaloTheme.colors.danger)
        LegendItem("Норма ${fmt(summary.targets.kcal)}", MaterialTheme.colorScheme.primary, line = true)
      }
    }
  }
}

@Composable
private fun LegendItem(text: String, color: Color, line: Boolean = false) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier
        .size(width = if (line) 14.dp else 10.dp, height = if (line) 2.dp else 10.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(color),
    )
    Spacer(Modifier.width(6.dp))
    Text(text, style = MaterialTheme.typography.labelMedium, color = CaloTheme.colors.muted)
  }
}

/** Bar chart drawn on a Canvas: one bar per day, dashed line for the goal. */
@Composable
fun CaloriesChart(days: List<DaySummary>, goal: Int, modifier: Modifier = Modifier) {
  val measurer = rememberTextMeasurer()
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = CaloTheme.colors.muted)
  val todayStyle = labelStyle.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
  val grid = CaloTheme.colors.border
  val good = CaloTheme.colors.good
  val danger = CaloTheme.colors.danger
  val empty = CaloTheme.colors.ringTrack
  val goalColor = MaterialTheme.colorScheme.primary
  val today = LocalDate.now()

  Canvas(modifier) {
    val padLeft = 34.dp.toPx()
    val padRight = 6.dp.toPx()
    val padTop = 8.dp.toPx()
    val padBottom = 22.dp.toPx()
    val innerW = size.width - padLeft - padRight
    val innerH = size.height - padTop - padBottom

    val peak = max(goal * 1.25, days.maxOfOrNull { it.total.kcal } ?: 0.0)
    val top = max(500.0, ceil(peak / 500) * 500)
    val step = if (top <= 2500) 500 else 1000
    fun y(value: Double) = (padTop + innerH - (value / top) * innerH).toFloat()

    // Grid lines with kcal labels: 0, 500, 1000...
    var v = 0
    while (v <= top) {
      val gy = y(v.toDouble())
      drawLine(grid, Offset(padLeft, gy), Offset(size.width - padRight, gy), strokeWidth = 1.dp.toPx())
      val label = v.toString()
      val text = measurer.measure(label, labelStyle)
      drawText(text, topLeft = Offset(padLeft - 6.dp.toPx() - text.size.width, gy - text.size.height / 2f))
      v += step
    }

    val slot = innerW / days.size
    val barWidth = min(26.dp.toPx(), max(3.dp.toPx(), slot * 0.62f))
    days.forEachIndexed { i, day ->
      val x = padLeft + slot * i + (slot - barWidth) / 2
      val kcal = day.total.kcal
      val h = if (day.logged) max(2.dp.toPx(), (kcal / top * innerH).toFloat()) else 3.dp.toPx()
      val color = when {
        !day.logged -> empty
        kcal > goal * 1.1 -> danger
        else -> good
      }
      drawRoundRect(
        color = color,
        topLeft = Offset(x, padTop + innerH - h),
        size = Size(barWidth, h),
        cornerRadius = CornerRadius(min(4.dp.toPx(), barWidth / 2)),
      )
      // Weekday names for 7 days, every 5th date for 30 days
      if (days.size <= 7 || (days.size - 1 - i) % 5 == 0) {
        val label = if (days.size <= 7) {
          shortWeekday(day.date.dayOfWeek)
        } else {
          day.date.dayOfMonth.toString()
        }
        val text = measurer.measure(label, if (day.date == today) todayStyle else labelStyle)
        drawText(text, topLeft = Offset(x + barWidth / 2 - text.size.width / 2f, size.height - text.size.height))
      }
    }

    val gy = y(goal.toDouble())
    drawLine(
      color = goalColor,
      start = Offset(padLeft, gy),
      end = Offset(size.width - padRight, gy),
      strokeWidth = 1.5.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
  }
}

@Composable
private fun StatsGrid(summary: ProgressSummary) {
  val logged = summary.loggedDays > 0
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      StatCard("В среднем", if (logged) fmt(summary.averageKcal) else "–", "ккал в день с записями", Modifier.weight(1f))
      StatCard("В норме", "${summary.onTargetDays} из ${summary.loggedDays}", "дней в пределах ±10%", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      StatCard(
        "Серия",
        summary.streak.toString(),
        plural(summary.streak, "день подряд", "дня подряд", "дней подряд"),
        Modifier.weight(1f),
      )
      StatCard(
        "Белков в среднем",
        if (logged) "${fmt(summary.averageProtein)} г" else "–",
        "цель ${summary.targets.protein} г",
        Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun StatCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
  CaloCard(modifier) {
    Column(Modifier.padding(14.dp)) {
      Text(label, style = MaterialTheme.typography.labelMedium, color = CaloTheme.colors.muted)
      Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(sub, style = MaterialTheme.typography.bodySmall, color = CaloTheme.colors.muted)
    }
  }
}

@Composable
private fun MacroCard(summary: ProgressSummary) {
  CaloCard {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      CardTitle("Откуда калории")
      val share = summary.macroShare
      if (share == null) {
        MutedText("Добавьте еду в дневник, чтобы увидеть, из чего складываются калории.")
      } else {
        StackBar(share)
        Row(Modifier.fillMaxWidth()) {
          MacroLegend("Белки", share.protein, summary.targets.split.protein, CaloTheme.colors.protein, Modifier.weight(1f))
          MacroLegend("Жиры", share.fat, summary.targets.split.fat, CaloTheme.colors.fat, Modifier.weight(1f))
          MacroLegend("Углеводы", share.carbs, summary.targets.split.carbs, CaloTheme.colors.carbs, Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun StackBar(share: MacroShare) {
  Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
    listOf(
      share.protein to CaloTheme.colors.protein,
      share.fat to CaloTheme.colors.fat,
      share.carbs to CaloTheme.colors.carbs,
    ).filter { it.first > 0 }.forEach { (percent, color) ->
      Box(Modifier.weight(percent.toFloat()).fillMaxHeight().background(color))
    }
  }
}

@Composable
private fun MacroLegend(label: String, percent: Int, goal: Int, color: Color, modifier: Modifier = Modifier) {
  Column(modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(8.dp).background(color, CircleShape))
      Spacer(Modifier.width(6.dp))
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
    Text("$percent%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("цель $goal%", style = MaterialTheme.typography.bodySmall, color = CaloTheme.colors.muted)
  }
}

@Composable
private fun DailyLogCard(summary: ProgressSummary, onOpenDay: (LocalDate) -> Unit) {
  val logged = summary.days.filter { it.logged }.reversed()
  CaloCard {
    SectionTitle("По дням", Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp))
    if (logged.isEmpty()) {
      MutedText("За этот период записей пока нет.", Modifier.padding(16.dp))
    }
    logged.forEachIndexed { index, day ->
      if (index > 0) RowDivider(start = 16.dp)
      val diff = day.total.kcal - summary.targets.kcal
      ListRow(
        title = dayLabel(day.date),
        subtitle = "Б ${fmt(day.total.protein)} г · Ж ${fmt(day.total.fat)} г · У ${fmt(day.total.carbs)} г",
        onClick = { onOpenDay(day.date) },
        trailing = {
          Column(horizontalAlignment = Alignment.End) {
            Text(fmt(day.total.kcal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              (if (diff > 0) "+" else "−") + fmt(abs(diff)),
              style = MaterialTheme.typography.labelMedium,
              color = if (diff > 0) CaloTheme.colors.danger else CaloTheme.colors.good,
            )
          }
        },
      )
    }
    Spacer(Modifier.height(6.dp))
  }
}

@Preview(name = "Progress", showBackground = true, widthDp = 390, heightDp = 1250)
@Composable
private fun ProgressPreview() {
  PreviewScreen {
    ProgressScreen(SampleData.progress, 7, {}, {})
  }
}
