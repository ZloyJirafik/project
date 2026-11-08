package com.example.caloscan.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.caloscan.R
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.model.Nutrition
import com.example.caloscan.data.model.unitLabel
import com.example.caloscan.data.remote.OpenFoodFactsParser
import com.example.caloscan.domain.AmountMode
import com.example.caloscan.domain.CustomFoodInput
import com.example.caloscan.domain.CustomFoodResult
import com.example.caloscan.domain.CustomFoodValidator
import com.example.caloscan.domain.Portions
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.CustomFoodDraft
import com.example.caloscan.ui.MissingReason
import com.example.caloscan.ui.Sheet
import com.example.caloscan.ui.components.CaloIcon
import com.example.caloscan.ui.components.ConfirmDialog
import com.example.caloscan.ui.components.FoodAvatar
import com.example.caloscan.ui.components.LoadingRow
import com.example.caloscan.ui.components.MutedText
import com.example.caloscan.ui.components.Segmented
import com.example.caloscan.ui.components.servingText
import com.example.caloscan.ui.preview.PreviewScreen
import com.example.caloscan.ui.preview.SampleData
import com.example.caloscan.ui.theme.CaloTheme
import com.example.caloscan.util.dayLabel
import com.example.caloscan.util.fmt
import com.example.caloscan.util.grams
import com.example.caloscan.util.parseNumber
import com.example.caloscan.util.plain
import java.time.LocalDate

/** Shows the bottom sheet that the ViewModel says is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(sheet: Sheet, vm: CaloScanViewModel) {
  val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = vm::closeSheet,
    sheetState = state,
    containerColor = CaloTheme.colors.card,
  ) {
    // key(): each sheet starts with fresh state
    key(sheet) {
      when (sheet) {
        is Sheet.Lookup -> LookupContent(sheet.code, vm::closeSheet)
        is Sheet.Missing -> MissingContent(
          sheet = sheet,
          onRetry = { vm.lookupBarcode(sheet.code) },
          onAdd = { vm.addMissingProduct(sheet) },
          onClose = vm::closeSheet,
        )
        is Sheet.Product -> ProductCardContent(
          food = sheet.food,
          onAdd = { vm.addToLibrary(sheet.food) },
          onAddToDiary = { vm.openFood(sheet.food) },
          onClose = vm::closeSheet,
        )
        is Sheet.AddFood -> FoodEntryContent(
          sheet = sheet,
          onSave = { amount, meal -> vm.saveEntry(sheet, amount, meal) },
          onDelete = vm::deleteEntry,
          onClose = vm::closeSheet,
        )
        is Sheet.CustomFood -> CustomFoodContent(
          draft = sheet.draft,
          onSave = { food -> vm.saveCustomFood(food, sheet.draft) },
          onDelete = vm::deleteCustomFood,
          onClose = vm::closeSheet,
        )
      }
    }
  }
}

/** Scrollable sheet body with side padding. */
@Composable
private fun SheetColumn(content: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    content = content,
  )
}

@Composable
private fun SheetHeader(
  title: String,
  subtitle: String?,
  onClose: () -> Unit,
  avatar: (@Composable () -> Unit)? = null,
) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    avatar?.invoke()
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      if (!subtitle.isNullOrBlank()) MutedText(subtitle)
    }
    IconButton(onClick = onClose) { CaloIcon(R.drawable.ic_close, "Закрыть") }
  }
}

@Composable
private fun FieldLabel(text: String) {
  Text(text, style = MaterialTheme.typography.labelLarge, color = CaloTheme.colors.muted)
}

// ---------- Barcode lookup ----------

@Composable
private fun LookupContent(code: String, onClose: () -> Unit) {
  SheetColumn {
    SheetHeader("Ищем товар…", "Штрихкод $code", onClose)
    LoadingRow("Поиск в Open Food Facts")
  }
}

@Composable
private fun MissingContent(sheet: Sheet.Missing, onRetry: () -> Unit, onAdd: () -> Unit, onClose: () -> Unit) {
  val (title, text) = when (sheet.reason) {
    MissingReason.NOT_FOUND -> "Товар не найден" to
      "Этого штрихкода пока нет в базе Open Food Facts. Добавьте товар по этикетке — в следующий раз он найдётся сразу."
    MissingReason.OFFLINE -> "Сейчас не получается найти" to
      "Похоже, нет интернета или Open Food Facts не ответил. Повторите попытку или введите товар сами."
  }
  SheetColumn {
    SheetHeader(title, "Штрихкод ${sheet.code}", onClose)
    MutedText(text)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      if (sheet.reason == MissingReason.OFFLINE) {
        OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Повторить") }
      }
      Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
        CaloIcon(R.drawable.ic_plus, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Добавить товар")
      }
    }
  }
}

// ---------- Product card (after a scan) ----------

/**
 * What a scan found: the product photo, its details, then "Закрыть" and "Добавить".
 * "Добавить" saves the product to "My foods". A product without nutrition facts
 * opens the form instead, to copy them from the label. A product that is already
 * in "My foods" can go to the diary instead.
 */
@Composable
fun ProductCardContent(
  food: Food,
  onAdd: () -> Unit,
  onAddToDiary: () -> Unit,
  onClose: () -> Unit,
) {
  val inLibrary = food.source == FoodSource.CUSTOM
  SheetColumn {
    ProductPhoto(food)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(food.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      val brand = listOf(food.brand, food.packageSize).filter { it.isNotBlank() }.joinToString(" · ")
      if (brand.isNotEmpty()) MutedText(brand)
    }

    if (food.hasNutrition) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Пищевая ценность на 100 ${food.unitLabel}")
        NutritionHero(food.nutritionFor(100.0))
      }
    } else {
      Notice("В Open Food Facts нет пищевой ценности этого товара. Нажмите «Добавить» и перепишите её с упаковки.")
    }

    Column(
      Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
      if (food.serving != null) DetailRow("Порция", servingText(food))
      if (food.barcode.isNotBlank()) DetailRow("Штрихкод", food.barcode)
      DetailRow("Источник", food.source.label)
    }

    if (inLibrary) Notice("Этот товар уже есть в «Моих продуктах».")

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(50.dp)) { Text("Закрыть") }
      Button(onClick = if (inLibrary) onAddToDiary else onAdd, modifier = Modifier.weight(1f).height(50.dp)) {
        if (!inLibrary) {
          CaloIcon(R.drawable.ic_plus, contentDescription = null)
          Spacer(Modifier.width(6.dp))
        }
        Text(if (inLibrary) "В дневник" else "Добавить")
      }
    }
  }
}

/** The product photo, large. Without a photo (or offline) the food's emoji is shown. */
@Composable
private fun ProductPhoto(food: Food) {
  var url by remember(food.imageUrl) { mutableStateOf(OpenFoodFactsParser.largeImageUrl(food.imageUrl)) }
  Box(
    Modifier
      .fillMaxWidth()
      .height(240.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    if (url.isBlank()) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(food.emoji.ifBlank { "🍽️" }, fontSize = 64.sp)
        MutedText("Нет фото")
      }
    } else {
      AsyncImage(
        model = url,
        contentDescription = "Фото: ${food.name}",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().padding(12.dp),
        // Some products have no 400 px photo: try the small one, then show the emoji
        onError = { url = if (url != food.imageUrl) food.imageUrl else "" },
      )
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = CaloTheme.colors.muted)
    Text(
      value,
      Modifier.weight(1f),
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.End,
    )
  }
}

@Composable
private fun Notice(text: String) {
  Text(
    text,
    Modifier
      .fillMaxWidth()
      .background(CaloTheme.colors.accentSoft, RoundedCornerShape(12.dp))
      .padding(12.dp),
    style = MaterialTheme.typography.bodyMedium,
  )
}

// ---------- Add / edit a diary entry ----------

@Composable
fun FoodEntryContent(
  sheet: Sheet.AddFood,
  onSave: (amount: Double, meal: Meal) -> Unit,
  onDelete: (DiaryEntry) -> Unit,
  onClose: () -> Unit,
) {
  val food = sheet.food
  val hasServing = (food.serving?.amount ?: 0.0) > 0
  val start = remember { Portions.initial(food, sheet.entry?.amount) }
  var mode by remember { mutableStateOf(start.mode) }
  var quantityText by remember { mutableStateOf(plain(start.quantity)) }
  var meal by remember { mutableStateOf(sheet.meal) }

  val quantity = parseNumber(quantityText)?.takeIf { it > 0 } ?: 0.0
  val amount = Portions.amount(food, mode, quantity)
  val nutrition = food.nutritionFor(amount)
  val unitName = if (food.unit == "ml") "мл" else "граммы"

  SheetColumn {
    SheetHeader(
      title = food.name,
      subtitle = listOf(food.brand, food.packageSize, food.source.label).filter { it.isNotBlank() }.joinToString(" · "),
      onClose = onClose,
      avatar = { FoodAvatar(food.emoji, food.imageUrl, size = 52.dp) },
    )
    NutritionHero(nutrition)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FieldLabel("Количество (" + (if (mode == AmountMode.SERVINGS) "порции" else unitName) + ")")
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton("−", "Меньше") { quantityText = plain(Portions.step(quantity, mode, -1)) }
        OutlinedTextField(
          value = quantityText,
          onValueChange = { text -> quantityText = text.filter { it.isDigit() || it == '.' || it == ',' }.take(7) },
          modifier = Modifier.weight(1f),
          textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        StepButton("+", "Больше") { quantityText = plain(Portions.step(quantity, mode, 1)) }
      }
      if (hasServing) {
        Segmented(
          options = listOf(AmountMode.SERVINGS, AmountMode.UNITS),
          selected = mode,
          label = { if (it == AmountMode.SERVINGS) "Порции" else if (food.unit == "ml") "Миллилитры (мл)" else "Граммы (г)" },
          onSelect = { newMode ->
            if (newMode != mode) {
              quantityText = plain(Portions.convert(food, amount, newMode))
              mode = newMode
            }
          },
        )
        if (mode == AmountMode.SERVINGS) {
          Text(
            "1 порция = ${servingText(food)} · всего ${grams(amount)} ${food.unitLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = CaloTheme.colors.muted,
          )
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FieldLabel("Приём пищи")
      Segmented(Meal.entries.toList(), meal, { it.shortLabel }, { meal = it })
    }

    Text(
      "На 100 ${food.unitLabel}: ${fmt(food.kcal)} ккал · белки ${grams(food.protein)} г · " +
        "жиры ${grams(food.fat)} г · углеводы ${grams(food.carbs)} г",
      style = MaterialTheme.typography.bodySmall,
      color = CaloTheme.colors.muted,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      val entry = sheet.entry
      if (entry != null) {
        OutlinedButton(
          onClick = { onDelete(entry) },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = CaloTheme.colors.danger),
        ) { Text("Удалить") }
      }
      val dayNote = if (sheet.date != LocalDate.now()) " · ${dayLabel(sheet.date)}" else ""
      Button(
        onClick = { onSave(amount, meal) },
        enabled = amount > 0,
        modifier = Modifier.weight(1f).height(50.dp),
      ) {
        Text(if (entry != null) "Сохранить" else "Добавить в ${meal.into}$dayNote")
      }
    }
  }
}

@Composable
private fun StepButton(text: String, description: String, onClick: () -> Unit) {
  FilledTonalButton(
    onClick = onClick,
    modifier = Modifier.size(56.dp).semantics { contentDescription = description },
    contentPadding = ButtonDefaults.TextButtonContentPadding,
  ) {
    Text(text, fontSize = 24.sp, modifier = Modifier.padding(bottom = 2.dp))
  }
}

@Composable
private fun NutritionHero(n: Nutrition) {
  Row(
    Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column {
      Text(fmt(n.kcal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
      MutedText("ккал")
    }
    Spacer(Modifier.weight(1f))
    HeroMacro("Белки", n.protein, CaloTheme.colors.protein)
    HeroMacro("Жиры", n.fat, CaloTheme.colors.fat)
    HeroMacro("Углеводы", n.carbs, CaloTheme.colors.carbs)
  }
}

@Composable
private fun HeroMacro(label: String, value: Double, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    Text("${grams(value)} г", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
  }
}

// ---------- Create / edit one of "My foods" ----------

@Composable
fun CustomFoodContent(
  draft: CustomFoodDraft,
  onSave: (food: Food) -> Unit,
  onDelete: (id: String, name: String) -> Unit,
  onClose: () -> Unit,
) {
  val editingId = draft.editingId
  var name by remember { mutableStateOf(draft.name) }
  var brand by remember { mutableStateOf(draft.brand) }
  var perServing by remember { mutableStateOf(false) }
  var kcal by remember { mutableStateOf(draft.kcal) }
  var protein by remember { mutableStateOf(draft.protein) }
  var carbs by remember { mutableStateOf(draft.carbs) }
  var fat by remember { mutableStateOf(draft.fat) }
  var servingLabel by remember { mutableStateOf(draft.servingLabel) }
  var servingAmount by remember { mutableStateOf(draft.servingAmount) }
  var unit by remember { mutableStateOf(draft.unit) }
  var barcode by remember { mutableStateOf(draft.barcode) }
  var error by remember { mutableStateOf<String?>(null) }
  var confirmDelete by remember { mutableStateOf(false) }

  SheetColumn {
    SheetHeader(
      title = if (editingId != null) "Изменить продукт" else "Новый продукт",
      subtitle = if (draft.barcode.isNotBlank()) "Штрихкод ${draft.barcode}" else "Сохранится в «Мои продукты»",
      onClose = onClose,
      // The photo of a scanned product
      avatar = draft.imageUrl.takeIf { it.isNotBlank() }?.let { url -> { FoodAvatar("🏷️", url, size = 52.dp) } },
    )
    TextInput("Название", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "Например, домашние блины")
    TextInput("Бренд (необязательно)", brand, { brand = it }, Modifier.fillMaxWidth())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FieldLabel("Значения указаны")
      Segmented(listOf(false, true), perServing, { if (it) "На порцию" else "На 100 г / мл" }, { perServing = it })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      TextInput("Калории (ккал)", kcal, { kcal = it }, Modifier.weight(1f), number = true)
      TextInput("Белки (г)", protein, { protein = it }, Modifier.weight(1f), number = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      TextInput("Жиры (г)", fat, { fat = it }, Modifier.weight(1f), number = true)
      TextInput("Углеводы (г)", carbs, { carbs = it }, Modifier.weight(1f), number = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      TextInput("Порция", servingLabel, { servingLabel = it }, Modifier.weight(1f), placeholder = "1 батончик")
      TextInput("Вес порции", servingAmount, { servingAmount = it }, Modifier.weight(1f), number = true, placeholder = "Например, 45", suffix = unitLabel(unit))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FieldLabel("Единицы")
      Segmented(listOf("g", "ml"), unit, { if (it == "g") "Граммы (г)" else "Миллилитры (мл)" }, { unit = it })
    }
    TextInput("Штрихкод (необязательно)", barcode, { text -> barcode = text.filter(Char::isDigit).take(14) }, Modifier.fillMaxWidth(), number = true)

    error?.let { Text(it, color = CaloTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      if (editingId != null) {
        OutlinedButton(
          onClick = { confirmDelete = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = CaloTheme.colors.danger),
        ) { Text("Удалить") }
      }
      Button(
        onClick = {
          val input = CustomFoodInput(name, brand, perServing, kcal, protein, carbs, fat, servingLabel, servingAmount, unit, barcode)
          when (val result = CustomFoodValidator.validate(input, editingId)) {
            is CustomFoodResult.Ok -> onSave(result.food)
            is CustomFoodResult.Invalid -> error = result.message
          }
        },
        modifier = Modifier.weight(1f).height(50.dp),
      ) {
        Text(
          when {
            editingId != null -> "Сохранить"
            draft.fromScan -> "Добавить" // to "My foods", like on the product card
            else -> "Сохранить и добавить" // and then add it to the diary
          },
        )
      }
    }
  }

  if (confirmDelete && editingId != null) {
    ConfirmDialog(
      title = "Удалить «${draft.name}»?",
      text = "Продукт пропадёт из «Моих продуктов». Записи в дневнике останутся.",
      confirmLabel = "Удалить",
      onConfirm = {
        confirmDelete = false
        onDelete(editingId, draft.name)
      },
      onDismiss = { confirmDelete = false },
    )
  }
}

@Composable
private fun TextInput(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  number: Boolean = false,
  placeholder: String? = null,
  suffix: String? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = { Text(label, maxLines = 1) },
    placeholder = placeholder?.let { { Text(it, maxLines = 1) } },
    suffix = suffix?.let { { Text(it) } },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
  )
}

// ---------- Previews (the sheet contents without the sheet itself) ----------

@Preview(name = "Sheet – add food", showBackground = true, widthDp = 390, heightDp = 720)
@Composable
private fun FoodEntryPreview() {
  PreviewScreen {
    Column(Modifier.padding(top = 16.dp)) {
      FoodEntryContent(Sheet.AddFood(SampleData.nutella, SampleData.today, Meal.SNACK), { _, _ -> }, {}, {})
    }
  }
}

@Preview(name = "Sheet – new food", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun CustomFoodPreview() {
  PreviewScreen {
    Column(Modifier.padding(top = 16.dp)) {
      CustomFoodContent(CustomFoodDraft.fromScan("2000000012348"), {}, { _, _ -> }, {})
    }
  }
}

@Preview(name = "Sheet – not found", showBackground = true, widthDp = 390, heightDp = 320)
@Composable
private fun MissingPreview() {
  PreviewScreen {
    Column(Modifier.padding(top = 16.dp)) {
      MissingContent(Sheet.Missing("2000000012348", MissingReason.NOT_FOUND), {}, {}, {})
    }
  }
}

@Preview(name = "Sheet – product card", showBackground = true, widthDp = 390, heightDp = 860)
@Composable
private fun ProductCardPreview() {
  PreviewScreen {
    Column(Modifier.padding(top = 16.dp)) {
      ProductCardContent(SampleData.nutella, {}, {}, {})
    }
  }
}

@Preview(name = "Sheet – product card (dark)", showBackground = true, widthDp = 390, heightDp = 860)
@Composable
private fun ProductCardDarkPreview() {
  PreviewScreen(darkTheme = true) {
    Column(Modifier.padding(top = 16.dp)) {
      ProductCardContent(SampleData.nutella.copy(source = FoodSource.CUSTOM), {}, {}, {})
    }
  }
}
