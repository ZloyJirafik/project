package com.example.caloscan.ui.foods

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caloscan.R
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.Meal
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.FoodFilter
import com.example.caloscan.ui.FoodResults
import com.example.caloscan.ui.OnlineSearch
import com.example.caloscan.ui.Tab
import com.example.caloscan.ui.buildFoodResults
import com.example.caloscan.ui.components.CaloCard
import com.example.caloscan.ui.components.CaloIcon
import com.example.caloscan.ui.components.EmptyCard
import com.example.caloscan.ui.components.FoodListCard
import com.example.caloscan.ui.components.LoadingRow
import com.example.caloscan.ui.components.MutedText
import com.example.caloscan.ui.preview.PreviewScreen
import com.example.caloscan.ui.preview.SampleData
import com.example.caloscan.ui.theme.CaloTheme
import com.example.caloscan.util.dayLabel
import java.time.LocalDate

/** "Добавление в завтрак · Вчера" above the list. */
data class AddingBanner(val target: String, val dayNote: String?)

@Composable
fun FoodsRoute(vm: CaloScanViewModel) {
  val query by vm.query.collectAsStateWithLifecycle()
  val filter by vm.filter.collectAsStateWithLifecycle()
  val results by vm.foodResults.collectAsStateWithLifecycle()
  val online by vm.online.collectAsStateWithLifecycle()
  val addMeal by vm.addMeal.collectAsStateWithLifecycle()
  val date by vm.date.collectAsStateWithLifecycle()

  FoodsScreen(
    query = query,
    onQueryChange = vm::setQuery,
    filter = filter,
    onFilterChange = vm::setFilter,
    results = results,
    online = online,
    banner = addingBanner(addMeal, date),
    onSearchOnline = vm::searchOnline,
    onOpenFood = vm::openFood,
    onCreateFood = { vm.newCustomFood(query.trim()) },
    onScan = { vm.selectTab(Tab.SCAN) },
    onCancelAdding = vm::cancelAdding,
  )
}

private fun addingBanner(meal: Meal?, date: LocalDate): AddingBanner? {
  val notToday = date != LocalDate.now()
  if (meal == null && !notToday) return null
  return AddingBanner(meal?.into ?: "дневник", if (notToday) dayLabel(date) else null)
}

/** Search the built-in database, your own foods, and Open Food Facts. */
@Composable
fun FoodsScreen(
  query: String,
  onQueryChange: (String) -> Unit,
  filter: FoodFilter,
  onFilterChange: (FoodFilter) -> Unit,
  results: FoodResults,
  online: OnlineSearch,
  banner: AddingBanner?,
  onSearchOnline: () -> Unit,
  onOpenFood: (Food) -> Unit,
  onCreateFood: () -> Unit,
  onScan: () -> Unit,
  onCancelAdding: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val q = query.trim()

  Column(modifier.fillMaxSize()) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text("Поиск: яблоко, рис, йогурт…") },
        leadingIcon = { CaloIcon(R.drawable.ic_search, contentDescription = null) },
        trailingIcon = {
          Row {
            if (query.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) { CaloIcon(R.drawable.ic_close, "Очистить поиск") }
            }
            // Not in the list? Scan the package
            IconButton(onClick = onScan) { CaloIcon(R.drawable.ic_scan, "Сканировать штрихкод или QR-код") }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = CaloTheme.colors.card,
          unfocusedContainerColor = CaloTheme.colors.card,
        ),
      )
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(FoodFilter.all) { item ->
          FilterChip(selected = item == filter, onClick = { onFilterChange(item) }, label = { Text(item.label) })
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      banner?.let { item { BannerCard(it, onCancelAdding) } }
      items(results.sections, key = { it.title }) { section ->
        FoodListCard(section.title, section.foods, onOpenFood)
      }
      if (results.emptyTitle != null) {
        item { EmptyCard(results.emptyTitle, results.emptyText.orEmpty()) }
      }
      if (q.isNotEmpty()) {
        item { OnlineSection(q, online, onSearchOnline, onOpenFood) }
      }
      item {
        OutlinedButton(onClick = onCreateFood, modifier = Modifier.fillMaxWidth()) {
          CaloIcon(R.drawable.ic_plus, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Создать свой продукт")
        }
      }
    }
  }
}

@Composable
private fun BannerCard(banner: AddingBanner, onCancel: () -> Unit) {
  CaloCard {
    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(
        buildAnnotatedString {
          append("Добавление в ")
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(banner.target) }
          banner.dayNote?.let { append(" · $it") }
        },
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onCancel) { Text(if (banner.dayNote != null) "На сегодня" else "Отмена") }
    }
  }
}

@Composable
private fun OnlineSection(query: String, online: OnlineSearch, onSearch: () -> Unit, onOpenFood: (Food) -> Unit) {
  when {
    online.query != query || online is OnlineSearch.Idle ->
      OnlineCta("Ищете продукт в упаковке? В онлайн-базе их миллионы.", "Искать в Open Food Facts", onSearch)
    online is OnlineSearch.Loading ->
      CaloCard { LoadingRow("Ищем в Open Food Facts…") }
    online is OnlineSearch.Failed ->
      OnlineCta("Не удалось связаться с Open Food Facts. Проверьте подключение к интернету.", "Повторить", onSearch)
    online is OnlineSearch.Done && online.results.isEmpty() ->
      EmptyCard("В онлайн-базе ничего не найдено", "Попробуйте другое название или создайте продукт сами.")
    online is OnlineSearch.Done ->
      FoodListCard("Из Open Food Facts", online.results, onOpenFood)
  }
}

@Composable
private fun OnlineCta(text: String, button: String, onClick: () -> Unit) {
  CaloCard {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      MutedText(text)
      Spacer(Modifier.padding(4.dp))
      OutlinedButton(onClick = onClick) {
        CaloIcon(R.drawable.ic_globe, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(button)
      }
    }
  }
}

@Preview(name = "Foods – browse", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun FoodsBrowsePreview() {
  PreviewScreen {
    FoodsScreen(
      query = "",
      onQueryChange = {},
      filter = FoodFilter.All,
      onFilterChange = {},
      results = buildFoodResults("", FoodFilter.All, SampleData.customFoods, emptyList(), SampleData.recentFoods),
      online = OnlineSearch.Idle,
      banner = AddingBanner("завтрак", null),
      onSearchOnline = {},
      onOpenFood = {},
      onCreateFood = {},
      onScan = {},
      onCancelAdding = {},
    )
  }
}

@Preview(name = "Foods – search", showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun FoodsSearchPreview() {
  PreviewScreen {
    FoodsScreen(
      query = "рис",
      onQueryChange = {},
      filter = FoodFilter.All,
      onFilterChange = {},
      results = buildFoodResults("рис", FoodFilter.All, emptyList(), emptyList(), emptyList()),
      online = OnlineSearch.Idle,
      banner = null,
      onSearchOnline = {},
      onOpenFood = {},
      onCreateFood = {},
      onScan = {},
      onCancelAdding = {},
    )
  }
}
