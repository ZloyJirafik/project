package com.example.caloscan.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caloscan.CaloScanApplication
import com.example.caloscan.data.FoodRepository
import com.example.caloscan.data.model.DiaryEntry
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.Meal
import com.example.caloscan.data.remote.OpenFoodFactsParser
import com.example.caloscan.domain.CalorieCalculator
import com.example.caloscan.domain.Profile
import com.example.caloscan.domain.ProgressCalculator
import com.example.caloscan.domain.ProgressSummary
import com.example.caloscan.domain.Targets
import com.example.caloscan.util.dayLabel
import com.example.caloscan.util.fmt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToLong

/**
 * Holds the app's state and handles everything the user does.
 * The screens only display this state and call these functions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaloScanViewModel(private val repo: FoodRepository) : ViewModel() {

  private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
    stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

  // ---------- Navigation ----------

  private val _tab = MutableStateFlow(Tab.DIARY)
  val tab: StateFlow<Tab> = _tab.asStateFlow()

  /** Today's date. Updated by [refreshToday] when the app comes back after midnight. */
  private val _today = MutableStateFlow(LocalDate.now())

  /** The day shown on the Diary screen. New food is added to this day. */
  private val _date = MutableStateFlow(_today.value)
  val date: StateFlow<LocalDate> = _date.asStateFlow()

  /**
   * Called every time the app is shown. If the date changed while the app was in
   * the background, the Diary moves on to the new day (unless another day was picked)
   * and the Progress screen counts up to the new day.
   */
  fun refreshToday() {
    val now = LocalDate.now()
    val oldToday = _today.value
    if (now == oldToday) return
    if (_date.value == oldToday) _date.value = now
    _today.value = now
  }

  /** Set when the user tapped "+" on a meal; the Foods screen then adds to that meal. */
  private val _addMeal = MutableStateFlow<Meal?>(null)
  val addMeal: StateFlow<Meal?> = _addMeal.asStateFlow()

  private val _sheet = MutableStateFlow<Sheet?>(null)
  val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

  private val _messages = Channel<UiMessage>(Channel.BUFFERED)
  val messages: Flow<UiMessage> = _messages.receiveAsFlow()

  /** Goes up when all data is replaced (import / delete), so forms reload. */
  private val _dataVersion = MutableStateFlow(0)
  val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

  fun selectTab(tab: Tab) {
    _addMeal.value = null
    _tab.value = tab
  }

  fun startAdding(meal: Meal) {
    _addMeal.value = meal
    _tab.value = Tab.FOODS
  }

  fun cancelAdding() {
    _addMeal.value = null
    _date.value = LocalDate.now()
  }

  fun changeDay(days: Long) {
    _date.value = _date.value.plusDays(days)
  }

  fun goToToday() {
    _date.value = LocalDate.now()
  }

  fun openDay(date: LocalDate) {
    _date.value = date
    _tab.value = Tab.DIARY
  }

  fun showMessage(text: String) {
    _messages.trySend(UiMessage(text))
  }

  // ---------- Data from the database ----------

  /** Null until the profile has been loaded. */
  val profile: StateFlow<Profile?> = repo.profile.state(null)

  val targets: StateFlow<Targets> =
    repo.profile.map { CalorieCalculator.targets(it) }.state(CalorieCalculator.targets(Profile()))

  val dayEntries: StateFlow<List<DiaryEntry>> = _date.flatMapLatest { repo.entriesOn(it) }.state(emptyList())

  val customFoods: StateFlow<List<Food>> = repo.customFoods.state(emptyList())
  val savedProducts: StateFlow<List<Food>> = repo.savedProducts.state(emptyList())
  val recentFoods: StateFlow<List<Food>> = repo.recentFoods.state(emptyList())

  fun saveProfile(profile: Profile) {
    viewModelScope.launch { repo.saveProfile(profile) }
  }

  // ---------- Foods screen ----------

  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  private val _filter = MutableStateFlow<FoodFilter>(FoodFilter.All)
  val filter: StateFlow<FoodFilter> = _filter.asStateFlow()

  val foodResults: StateFlow<FoodResults> =
    combine(_query, _filter, customFoods, savedProducts, recentFoods) { q, f, custom, products, recent ->
      buildFoodResults(q, f, custom, products, recent)
    }.state(buildFoodResults("", FoodFilter.All, emptyList(), emptyList(), emptyList()))

  private val _online = MutableStateFlow<OnlineSearch>(OnlineSearch.Idle)
  val online: StateFlow<OnlineSearch> = _online.asStateFlow()
  private var onlineJob: Job? = null

  fun setQuery(text: String) {
    _query.value = text
  }

  fun setFilter(filter: FoodFilter) {
    _filter.value = filter
  }

  fun searchOnline() {
    val q = _query.value.trim()
    if (q.isEmpty()) return
    onlineJob?.cancel()
    _online.value = OnlineSearch.Loading(q)
    onlineJob = viewModelScope.launch {
      _online.value = try {
        OnlineSearch.Done(q, repo.searchOnline(q))
      } catch (e: IOException) {
        OnlineSearch.Failed(q)
      }
    }
  }

  // ---------- Progress screen ----------

  private val _range = MutableStateFlow(7)
  val range: StateFlow<Int> = _range.asStateFlow()

  fun setRange(days: Int) {
    _range.value = days
  }

  val progress: StateFlow<ProgressSummary?> = combine(
    combine(_range, _today) { days, today -> days to today }.flatMapLatest { (days, today) ->
      repo.entriesBetween(today.minusDays(days - 1L), today).map { entries -> Triple(days, today, entries) }
    },
    repo.loggedDates,
    targets,
  ) { (days, today, entries), loggedDates, t ->
    ProgressCalculator.summarize(days, today, entries, loggedDates, t)
  }.state(null)

  // ---------- Bottom sheets ----------

  private var lookupJob: Job? = null

  fun closeSheet() {
    lookupJob?.cancel()
    _sheet.value = null
  }

  private fun addFoodSheet(food: Food) =
    Sheet.AddFood(food, _date.value, _addMeal.value ?: Meal.forHour(LocalTime.now().hour))

  fun openFood(food: Food) {
    _sheet.value = addFoodSheet(food)
  }

  fun openEntry(entry: DiaryEntry) {
    _sheet.value = Sheet.AddFood(entry.food, entry.date, entry.meal, entry)
  }

  fun saveEntry(sheet: Sheet.AddFood, amount: Double, meal: Meal) {
    val rounded = (amount * 10).roundToLong() / 10.0
    if (rounded <= 0) return
    viewModelScope.launch {
      val entry = sheet.entry
      if (entry != null) {
        repo.updateEntry(entry.copy(amount = rounded, meal = meal))
        showMessage("Запись изменена")
      } else {
        val added = repo.addEntry(sheet.date, sheet.food, rounded, meal)
        val day = if (sheet.date != LocalDate.now()) " (${dayLabel(sheet.date).lowercase()})" else ""
        showMessage("Добавлено ${fmt(added.nutrition.kcal)} ккал в ${meal.into}$day")
      }
      // After "+" on a meal, go back to the diary
      val backToDiary = entry == null && _addMeal.value != null && _tab.value == Tab.FOODS
      _sheet.value = null
      if (backToDiary) {
        _addMeal.value = null
        _tab.value = Tab.DIARY
      }
    }
  }

  fun deleteEntry(entry: DiaryEntry) {
    viewModelScope.launch {
      repo.deleteEntry(entry)
      _sheet.value = null
      _messages.send(
        UiMessage("Удалено: ${entry.food.name}", actionLabel = "Отменить") {
          viewModelScope.launch { repo.restoreEntry(entry) }
        },
      )
    }
  }

  // ---------- Barcodes ----------

  /** Finds a product: own foods → products scanned before → Open Food Facts. */
  fun lookupBarcode(raw: String) {
    val code = raw.filterNot { it.isWhitespace() || it == '-' }
    if (!BARCODE.matches(code)) {
      showMessage("Штрихкод товара — это от 6 до 14 цифр (обычно 13).")
      return
    }
    lookupJob?.cancel()
    lookupJob = viewModelScope.launch {
      val saved = repo.findSavedByBarcode(code)
      if (saved != null) {
        _sheet.value = addFoodSheet(saved)
        return@launch
      }

      val loading = Sheet.Lookup(code)
      _sheet.value = loading
      var failed = false
      val food = try {
        repo.lookupOnline(code)
      } catch (e: IOException) {
        failed = true
        null
      }
      if (_sheet.value != loading) return@launch // the user closed the sheet

      _sheet.value = if (food != null && food.hasNutrition) {
        repo.cacheProduct(food)
        addFoodSheet(food)
      } else {
        val reason = when {
          failed -> MissingReason.OFFLINE
          food != null -> MissingReason.NO_NUTRITION
          else -> MissingReason.NOT_FOUND
        }
        Sheet.Missing(code, reason, food)
      }
    }
  }

  // ---------- My foods ----------

  fun newCustomFood(name: String = "") {
    _sheet.value = Sheet.CustomFood(CustomFoodDraft(name = name))
  }

  /** "Add this product" after a barcode wasn't found. */
  fun addMissingProduct(sheet: Sheet.Missing) {
    val partial = sheet.partial
    _sheet.value = Sheet.CustomFood(
      CustomFoodDraft(
        barcode = sheet.code,
        name = partial?.name?.takeIf { it != OpenFoodFactsParser.UNNAMED }.orEmpty(),
        brand = partial?.brand.orEmpty(),
        unit = partial?.unit ?: "g",
      ),
    )
  }

  fun editCustomFood(food: Food) {
    _sheet.value = Sheet.CustomFood(CustomFoodDraft.from(food))
  }

  /** Saves the food. A new food is then opened so it can be added to the diary. */
  fun saveCustomFood(food: Food, editing: Boolean) {
    viewModelScope.launch {
      val saved = repo.saveCustomFood(food)
      if (editing) {
        _sheet.value = null
        showMessage("Сохранено: ${saved.name}")
      } else {
        _sheet.value = addFoodSheet(saved)
      }
    }
  }

  fun deleteCustomFood(id: String, name: String) {
    viewModelScope.launch {
      repo.deleteCustomFood(id)
      _sheet.value = null
      showMessage("Удалено: $name")
    }
  }

  // ---------- Backup ----------

  fun exportBackup(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch {
      try {
        val json = repo.exportJson()
        withContext(Dispatchers.IO) {
          val stream = resolver.openOutputStream(uri) ?: throw IOException("Can't open the file")
          stream.use { it.write(json.toByteArray()) }
        }
        showMessage("Резервная копия сохранена")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        showMessage("Не удалось сохранить резервную копию.")
      }
    }
  }

  fun importBackup(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch {
      try {
        val text = withContext(Dispatchers.IO) {
          val stream = resolver.openInputStream(uri) ?: throw IOException("Can't open the file")
          stream.bufferedReader().use { it.readText() }
        }
        repo.importJson(text)
        _dataVersion.value++
        showMessage("Данные восстановлены из резервной копии")
      } catch (e: CancellationException) {
        throw e
      } catch (e: IllegalArgumentException) {
        showMessage(e.message ?: "Это не резервная копия MyBody.")
      } catch (e: Exception) {
        showMessage("Не удалось прочитать файл.")
      }
    }
  }

  fun deleteAllData() {
    viewModelScope.launch {
      repo.deleteAll()
      _date.value = LocalDate.now()
      _dataVersion.value++
      showMessage("Все данные удалены")
    }
  }

  companion object {
    private val BARCODE = Regex("^[0-9]{6,14}$")

    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app = this[APPLICATION_KEY] as CaloScanApplication
        CaloScanViewModel(app.repository)
      }
    }
  }
}
