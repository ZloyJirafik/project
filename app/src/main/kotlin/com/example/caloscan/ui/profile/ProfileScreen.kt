package com.example.caloscan.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caloscan.R
import com.example.caloscan.data.model.Food
import com.example.caloscan.domain.ActivityLevel
import com.example.caloscan.domain.Goal
import com.example.caloscan.domain.MacroSplit
import com.example.caloscan.domain.Profile
import com.example.caloscan.domain.Sex
import com.example.caloscan.domain.Targets
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.components.CaloCard
import com.example.caloscan.ui.components.CaloIcon
import com.example.caloscan.ui.components.CardTitle
import com.example.caloscan.ui.components.ConfirmDialog
import com.example.caloscan.ui.components.FoodAvatar
import com.example.caloscan.ui.components.ListRow
import com.example.caloscan.ui.components.MutedText
import com.example.caloscan.ui.components.Pill
import com.example.caloscan.ui.components.RowDivider
import com.example.caloscan.ui.components.SectionTitle
import com.example.caloscan.ui.components.Segmented
import com.example.caloscan.ui.components.SelectField
import com.example.caloscan.ui.preview.PreviewScreen
import com.example.caloscan.ui.preview.SampleData
import com.example.caloscan.ui.theme.CaloTheme
import com.example.caloscan.util.fmt
import com.example.caloscan.util.parseInRange
import com.example.caloscan.util.parseNumber
import com.example.caloscan.util.plain
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProfileRoute(vm: CaloScanViewModel) {
  val profile by vm.profile.collectAsStateWithLifecycle()
  val targets by vm.targets.collectAsStateWithLifecycle()
  val customFoods by vm.customFoods.collectAsStateWithLifecycle()
  val dataVersion by vm.dataVersion.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var confirmDelete by remember { mutableStateOf(false) }
  var pendingImport by remember { mutableStateOf<Uri?>(null) }

  // The system's "Save as" and "Open" screens (Storage Access Framework)
  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    if (uri != null) vm.exportBackup(context.contentResolver, uri)
  }
  val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    pendingImport = uri
  }

  ProfileScreen(
    profile = profile,
    formKey = dataVersion,
    targets = targets,
    customFoods = customFoods,
    onProfileChange = vm::saveProfile,
    onEditFood = vm::editCustomFood,
    onCreateFood = { vm.newCustomFood() },
    onExport = { exportLauncher.launch("mybody-backup-${LocalDate.now()}.json") },
    onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
    onDeleteAll = { confirmDelete = true },
  )

  if (confirmDelete) {
    ConfirmDialog(
      title = "Удалить все данные?",
      text = "Профиль, дневник, ваши продукты и отсканированные товары будут удалены с телефона. Отменить это нельзя.",
      confirmLabel = "Удалить",
      onConfirm = {
        confirmDelete = false
        vm.deleteAllData()
      },
      onDismiss = { confirmDelete = false },
    )
  }
  pendingImport?.let { uri ->
    ConfirmDialog(
      title = "Восстановить из копии?",
      text = "Все текущие данные на телефоне будут заменены данными из резервной копии.",
      confirmLabel = "Заменить",
      onConfirm = {
        pendingImport = null
        vm.importBackup(context.contentResolver, uri)
      },
      onDismiss = { pendingImport = null },
    )
  }
}

/** Calorie calculator, my foods and backups. */
@Composable
fun ProfileScreen(
  profile: Profile?,
  formKey: Int,
  targets: Targets,
  customFoods: List<Food>,
  onProfileChange: (Profile) -> Unit,
  onEditFood: (Food) -> Unit,
  onCreateFood: () -> Unit,
  onExport: () -> Unit,
  onImport: () -> Unit,
  onDeleteAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { TargetCard(targets) }
    // The form waits until the saved profile is loaded, so it starts with the right values
    if (profile != null) item(key = "form-$formKey") { ProfileForm(profile, onProfileChange) }
    item { ExplanationCard() }
    item { MyFoodsCard(customFoods, onEditFood, onCreateFood) }
    item { DataCard(onExport, onImport, onDeleteAll) }
    item {
      MutedText(
        "Значения встроенной базы основаны на USDA FoodData Central. Товары в упаковке — из " +
          "Open Food Facts (ODbL). Штрихкоды читает Google ML Kit. Шрифт Inter (SIL OFL).",
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetCard(t: Targets) {
  CaloCard {
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionTitle("ВАША НОРМА НА ДЕНЬ")
      Row(verticalAlignment = Alignment.Bottom) {
        Text(fmt(t.kcal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(
          if (t.isPersonal) "ккал в день" else "ккал (по умолчанию)",
          style = MaterialTheme.typography.bodyLarge,
          color = CaloTheme.colors.muted,
          modifier = Modifier.padding(bottom = 6.dp),
        )
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("Белки ${t.protein} г", CaloTheme.colors.protein)
        Pill("Жиры ${t.fat} г", CaloTheme.colors.fat)
        Pill("Углеводы ${t.carbs} г", CaloTheme.colors.carbs)
      }

      val b = t.breakdown
      if (!t.isPersonal) {
        Note("Укажите пол, возраст, рост и вес ниже, чтобы рассчитать личную норму.")
      }
      if (b != null) {
        val delta = b.goal.delta
        Column(
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
          Step("Базовый обмен (BMR) — расход в покое", "${fmt(b.bmr)} ккал")
          Step("× ${plain(b.activity.factor, 3)} — активность «${b.activity.label.lowercase()}»", "${fmt(b.tdee)} ккал")
          Step(b.goal.label, (if (delta > 0) "+" else if (delta < 0) "−" else "±") + "${fmt(abs(delta))} ккал")
          Step("ИМТ", plain(b.bmi, 1) + " · " + b.bmiLabel)
        }
        if (b.floored && !t.overridden) Note("Повышено до безопасного минимума — ${fmt(b.minimum)} ккал в день.")
      }
      if (t.overridden) {
        Note("Используется ваша норма" + (b?.let { " (расчётная — ${fmt(it.calculated)} ккал)" } ?: "") + ".")
      }
    }
  }
}

@Composable
private fun Step(label: String, value: String) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = CaloTheme.colors.muted)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun Note(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = CaloTheme.colors.muted)
}

/**
 * The calculator form. Every change is saved right away.
 * Numbers outside a sensible range are shown in red and not used.
 */
@Composable
private fun ProfileForm(initial: Profile, onChange: (Profile) -> Unit) {
  var draft by remember { mutableStateOf(initial) }
  var age by rememberSaveable { mutableStateOf(initial.age?.toString().orEmpty()) }
  var height by rememberSaveable { mutableStateOf(initial.heightCm?.let { plain(it, 1) }.orEmpty()) }
  var weight by rememberSaveable { mutableStateOf(initial.weightKg?.let { plain(it, 1) }.orEmpty()) }
  var custom by rememberSaveable { mutableStateOf(initial.customTarget?.toString().orEmpty()) }

  fun update(profile: Profile) {
    draft = profile
    onChange(profile)
  }

  CaloCard {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      CardTitle("О вас")
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Пол", style = MaterialTheme.typography.labelMedium, color = CaloTheme.colors.muted)
        Segmented(Sex.entries.toList(), draft.sex, { it.label }, { update(draft.copy(sex = it)) })
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("Возраст", "лет", age, 13.0..100.0, KeyboardType.Number, Modifier.weight(1f)) {
          age = it
          update(draft.copy(age = parseInRange(it, 13.0, 100.0)?.roundToInt()))
        }
        NumberField("Рост", "см", height, 120.0..230.0, KeyboardType.Decimal, Modifier.weight(1f)) {
          height = it
          update(draft.copy(heightCm = parseInRange(it, 120.0, 230.0)))
        }
        NumberField("Вес", "кг", weight, 30.0..300.0, KeyboardType.Decimal, Modifier.weight(1f)) {
          weight = it
          update(draft.copy(weightKg = parseInRange(it, 30.0, 300.0)))
        }
      }
      SelectField(
        label = "Активность",
        options = ActivityLevel.entries.toList(),
        selected = draft.activity,
        optionText = { it.label },
        optionHint = { it.hint },
        onSelect = { update(draft.copy(activity = it)) },
      )
      SelectField(
        label = "Цель",
        options = Goal.entries.toList(),
        selected = draft.goal,
        optionText = { it.label },
        onSelect = { update(draft.copy(goal = it)) },
      )
      SelectField(
        label = "Соотношение БЖУ",
        options = MacroSplit.entries.toList(),
        selected = draft.macroSplit,
        optionText = { it.label },
        optionHint = { "Б ${it.protein}% · Ж ${it.fat}% · У ${it.carbs}%" },
        onSelect = { update(draft.copy(macroSplit = it)) },
      )
      NumberField(
        label = "Своя норма (необязательно)",
        unit = "ккал",
        value = custom,
        range = 800.0..6000.0,
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Пусто — расчётная норма",
      ) {
        custom = it
        update(draft.copy(customTarget = parseInRange(it, 800.0, 6000.0)?.roundToInt()))
      }
    }
  }
}

@Composable
private fun NumberField(
  label: String,
  unit: String,
  value: String,
  range: ClosedFloatingPointRange<Double>,
  keyboardType: KeyboardType,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  onValueChange: (String) -> Unit,
) {
  val number = parseNumber(value)
  val invalid = value.isNotBlank() && (number == null || number !in range)
  OutlinedTextField(
    value = value,
    onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
    modifier = modifier,
    label = { Text(label, maxLines = 1) },
    suffix = { Text(unit) },
    placeholder = placeholder?.let { { Text(it, maxLines = 1) } },
    isError = invalid,
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
  )
}

@Composable
private fun ExplanationCard() {
  var open by rememberSaveable { mutableStateOf(false) }
  CaloCard(Modifier.animateContentSize()) {
    Row(
      Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CardTitle("Как считается норма?", Modifier.weight(1f))
      CaloIcon(R.drawable.ic_expand, contentDescription = null, modifier = Modifier.rotate(if (open) 180f else 0f))
    }
    if (open) {
      Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Базовый обмен (BMR) — сколько калорий тело тратит в покое. Формула Миффлина — Сан Жеора (вес в кг, рост в см):")
        Text(
          "Мужчины: 10 × вес + 6,25 × рост − 5 × возраст + 5\nЖенщины: 10 × вес + 6,25 × рост − 5 × возраст − 161",
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp),
        )
        Text("2. Суточный расход (TDEE) — BMR × коэффициент активности: от 1,2 (минимальная) до 1,9 (очень высокая).")
        Text(
          "3. Цель — 1 кг жира ≈ 7 700 ккал, поэтому дефицит 500 ккал в день даёт примерно −0,5 кг в неделю. " +
            "Норма не опускается ниже 1 200 ккал для женщин и 1 500 ккал для мужчин.",
        )
        Text("4. БЖУ — норма делится по процентам. В 1 г белков и углеводов 4 ккал, в 1 г жиров — 9 ккал.")
        MutedText("Это оценка для здоровых взрослых, а не медицинская рекомендация.")
      }
    }
  }
}

@Composable
private fun MyFoodsCard(foods: List<Food>, onEdit: (Food) -> Unit, onCreate: () -> Unit) {
  CaloCard {
    SectionTitle("Мои продукты (${foods.size})", Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp))
    if (foods.isEmpty()) {
      MutedText("Здесь появятся продукты, которые вы создадите.", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    foods.forEachIndexed { index, food ->
      if (index > 0) RowDivider()
      ListRow(
        title = food.name,
        subtitle = "${fmt(food.kcal)} ккал на 100 ${food.unitLabel}" + if (food.barcode.isNotBlank()) " · ${food.barcode}" else "",
        onClick = { onEdit(food) },
        leading = { FoodAvatar(food.emoji, food.imageUrl) },
      )
    }
    OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      CaloIcon(R.drawable.ic_plus, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Создать продукт")
    }
  }
}

@Composable
private fun DataCard(onExport: () -> Unit, onImport: () -> Unit, onDeleteAll: () -> Unit) {
  CaloCard {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      CardTitle("Ваши данные")
      MutedText("Всё хранится только на этом телефоне.")
      OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
        CaloIcon(R.drawable.ic_download, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Сохранить резервную копию")
      }
      OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
        CaloIcon(R.drawable.ic_upload, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Восстановить из копии")
      }
      OutlinedButton(
        onClick = onDeleteAll,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CaloTheme.colors.danger),
      ) {
        Text("Удалить все данные")
      }
    }
  }
}

@Preview(name = "Profile", showBackground = true, widthDp = 390, heightDp = 1500)
@Composable
private fun ProfilePreview() {
  PreviewScreen {
    ProfileScreen(
      profile = SampleData.profile,
      formKey = 0,
      targets = SampleData.targets,
      customFoods = SampleData.customFoods,
      onProfileChange = {},
      onEditFood = {},
      onCreateFood = {},
      onExport = {},
      onImport = {},
      onDeleteAll = {},
    )
  }
}
