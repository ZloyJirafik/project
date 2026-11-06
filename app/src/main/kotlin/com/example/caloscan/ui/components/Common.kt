package com.example.caloscan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.caloscan.R
import com.example.caloscan.data.model.Food
import com.example.caloscan.ui.theme.CaloTheme
import com.example.caloscan.util.fmt
import com.example.caloscan.util.grams
import kotlin.math.abs

/* Small building blocks shared by all screens. */

@Composable
fun CaloIcon(
  @DrawableRes id: Int,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  tint: Color = LocalContentColor.current,
) {
  Icon(painter = painterResource(id), contentDescription = contentDescription, modifier = modifier, tint = tint)
}

/** White rounded card with a thin border, like the cards of the web version. */
@Composable
fun CaloCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = CaloTheme.colors.card),
    border = BorderStroke(1.dp, CaloTheme.colors.border),
    content = content,
  )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = CaloTheme.colors.muted,
  )
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
  Text(text, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
  Text(text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = CaloTheme.colors.muted)
}

/** An emoji in a rounded square, with the product photo on top when there is one. */
@Composable
fun FoodAvatar(emoji: String, imageUrl: String, modifier: Modifier = Modifier, size: Dp = 42.dp) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size / 3.5f))
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    Text(emoji.ifBlank { "🍽️" }, fontSize = (size.value * 0.48f).sp)
    if (imageUrl.isNotBlank()) {
      AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )
    }
  }
}

/** "1 шт. (182 г)", or just "15 г" when the label already has the amount. */
fun servingText(food: Food): String {
  val serving = food.serving ?: return "на 100 ${food.unitLabel}"
  val amount = "${grams(serving.amount)} ${food.unitLabel}"
  val label = serving.label.ifBlank { "1 порция" }
  fun compact(s: String) = s.replace(" ", "").replace(",", ".").lowercase()
  // Labels from Open Food Facts often look like "15 g", so check the English unit too
  val hasAmount = compact(label).contains(compact(amount)) ||
    compact(label).contains(compact(grams(serving.amount) + food.unit))
  return if (hasAmount) label else "$label ($amount)"
}

/** A tappable list row: picture, title + subtitle, something on the right. */
@Composable
fun ListRow(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leading: (@Composable () -> Unit)? = null,
  trailing: @Composable () -> Unit = {},
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    leading?.invoke()
    Column(Modifier.weight(1f)) {
      Text(
        title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitle.isNotEmpty()) {
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = CaloTheme.colors.muted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    trailing()
  }
}

@Composable
fun KcalText(kcal: Double) {
  Column(horizontalAlignment = Alignment.End) {
    Text(fmt(kcal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text("ккал", style = MaterialTheme.typography.labelSmall, color = CaloTheme.colors.muted)
  }
}

/** A food in a list: calories for one serving, or for 100 g when there's no serving. */
@Composable
fun FoodRow(food: Food, onClick: () -> Unit) {
  val basis = food.serving?.amount ?: 100.0
  val subtitle = listOf(food.brand, servingText(food)).filter { it.isNotBlank() }.joinToString(" · ")
  ListRow(
    title = food.name,
    subtitle = subtitle,
    onClick = onClick,
    leading = { FoodAvatar(food.emoji, food.imageUrl) },
    trailing = { KcalText(food.kcal * basis / 100) },
  )
}

/** Line between list rows. The default indent lines up with the text next to a food picture. */
@Composable
fun RowDivider(start: Dp = 70.dp) {
  HorizontalDivider(Modifier.padding(start = start), color = CaloTheme.colors.border)
}

/** A card with a title and a list of foods. */
@Composable
fun FoodListCard(title: String, foods: List<Food>, onOpen: (Food) -> Unit, modifier: Modifier = Modifier) {
  CaloCard(modifier) {
    SectionTitle(title, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp))
    foods.forEachIndexed { index, food ->
      if (index > 0) RowDivider()
      FoodRow(food) { onOpen(food) }
    }
    Spacer(Modifier.height(6.dp))
  }
}

@Composable
fun EmptyCard(title: String, text: String, modifier: Modifier = Modifier) {
  CaloCard(modifier) {
    Column(
      Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      MutedText(text)
    }
  }
}

@Composable
fun LoadingRow(text: String, modifier: Modifier = Modifier) {
  Row(
    modifier.fillMaxWidth().padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    MutedText(text)
  }
}

/** The calories ring on the Diary screen. Turns red when over the goal. */
@Composable
fun CalorieRing(eaten: Double, goal: Int, modifier: Modifier = Modifier, size: Dp = 136.dp) {
  val left = goal - eaten
  val over = left < -0.5
  val target = if (goal > 0) (eaten / goal).coerceIn(0.0, 1.0).toFloat() else 0f
  val progress by animateFloatAsState(target, label = "ring")
  val track = CaloTheme.colors.ringTrack
  val color = if (over) CaloTheme.colors.danger else MaterialTheme.colorScheme.primary

  Box(modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 12.dp.toPx()
      val topLeft = Offset(stroke / 2, stroke / 2)
      val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
      drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
      if (progress > 0f) {
        drawArc(
          color = color,
          startAngle = -90f,
          sweepAngle = 360f * progress,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(stroke, cap = StrokeCap.Round),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(fmt(abs(left)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text(
        if (over) "ккал сверх нормы" else "ккал осталось",
        style = MaterialTheme.typography.labelMedium,
        color = CaloTheme.colors.muted,
        textAlign = TextAlign.Center,
      )
    }
  }
}

/** "Белки ▬▬▬── 54 / 112 г" */
@Composable
fun MacroBar(label: String, color: Color, value: Double, target: Int, modifier: Modifier = Modifier) {
  val fraction = if (target > 0) (value / target).coerceIn(0.0, 1.0).toFloat() else 0f
  Column(modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(8.dp).background(color, CircleShape))
      Spacer(Modifier.width(6.dp))
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(6.dp))
    Box(
      Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(CircleShape)
        .background(CaloTheme.colors.ringTrack),
    ) {
      if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color, CircleShape))
    }
    Spacer(Modifier.height(4.dp))
    Text(
      buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append(fmt(value)) }
        append(" / ${fmt(target)} г")
      },
      style = MaterialTheme.typography.bodySmall,
      color = CaloTheme.colors.muted,
    )
  }
}

/** A row of buttons where exactly one is selected (Material 3 segmented buttons). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Segmented(
  options: List<T>,
  selected: T?,
  label: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
    options.forEachIndexed { index, option ->
      SegmentedButton(
        selected = option == selected,
        onClick = { onSelect(option) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
        icon = {},
        label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
      )
    }
  }
}

/** A dropdown field: shows the current choice and opens a menu with all choices. */
@Composable
fun <T> SelectField(
  label: String,
  options: List<T>,
  selected: T,
  optionText: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  optionHint: ((T) -> String)? = null,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier.fillMaxWidth()) {
    Surface(
      onClick = { expanded = true },
      shape = RoundedCornerShape(12.dp),
      color = CaloTheme.colors.card,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(label, style = MaterialTheme.typography.labelMedium, color = CaloTheme.colors.muted)
          Text(optionText(selected), style = MaterialTheme.typography.bodyLarge)
          optionHint?.let { Text(it(selected), style = MaterialTheme.typography.bodySmall, color = CaloTheme.colors.muted) }
        }
        CaloIcon(R.drawable.ic_expand, contentDescription = null, tint = CaloTheme.colors.muted)
      }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Column {
              Text(optionText(option), fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal)
              optionHint?.let { Text(it(option), style = MaterialTheme.typography.bodySmall, color = CaloTheme.colors.muted) }
            }
          },
          onClick = {
            onSelect(option)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
fun ConfirmDialog(
  title: String,
  text: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(confirmLabel, color = CaloTheme.colors.danger) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
  )
}

/** Small colored "Белки 112 г" label. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
  Row(
    modifier
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
    Spacer(Modifier.width(6.dp))
    Text(text, style = MaterialTheme.typography.labelLarge)
  }
}
