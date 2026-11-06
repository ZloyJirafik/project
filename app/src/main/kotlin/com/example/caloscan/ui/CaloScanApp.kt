package com.example.caloscan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caloscan.R
import com.example.caloscan.ui.components.CaloIcon
import com.example.caloscan.ui.diary.DiaryRoute
import com.example.caloscan.ui.diary.DiaryScreen
import com.example.caloscan.ui.foods.FoodsRoute
import com.example.caloscan.ui.preview.SampleData
import com.example.caloscan.ui.profile.ProfileRoute
import com.example.caloscan.ui.progress.ProgressRoute
import com.example.caloscan.ui.scan.ScanRoute
import com.example.caloscan.ui.sheets.SheetHost
import com.example.caloscan.ui.theme.CaloScanTheme
import com.example.caloscan.util.dayLabel
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The whole app: top bar, the current screen, bottom tabs, sheets and messages. */
@Composable
fun CaloScanApp(vm: CaloScanViewModel) {
  val tab by vm.tab.collectAsStateWithLifecycle()
  val date by vm.date.collectAsStateWithLifecycle()
  val sheet by vm.sheet.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }

  // Show messages from the ViewModel. A new message replaces the current one.
  LaunchedEffect(vm) {
    vm.messages.collect { message ->
      snackbar.currentSnackbarData?.dismiss()
      launch {
        val result = snackbar.showSnackbar(
          message = message.text,
          actionLabel = message.actionLabel,
          duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
      }
    }
  }

  // The app may stay in the background overnight, so check the date each time it's shown
  LifecycleResumeEffect(vm) {
    vm.refreshToday()
    onPauseOrDispose { }
  }

  // Back from another tab goes to the Diary first, then leaves the app
  BackHandler(enabled = tab != Tab.DIARY) { vm.selectTab(Tab.DIARY) }

  AppScaffold(
    tab = tab,
    date = date,
    snackbar = snackbar,
    onSelectTab = vm::selectTab,
    onChangeDay = vm::changeDay,
    onToday = vm::goToToday,
  ) { padding ->
    Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
      when (tab) {
        Tab.DIARY -> DiaryRoute(vm)
        Tab.FOODS -> FoodsRoute(vm)
        Tab.SCAN -> ScanRoute(vm)
        Tab.PROGRESS -> ProgressRoute(vm)
        Tab.PROFILE -> ProfileRoute(vm)
      }
    }
  }

  sheet?.let { SheetHost(it, vm) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
  tab: Tab,
  date: LocalDate,
  snackbar: SnackbarHostState,
  onSelectTab: (Tab) -> Unit,
  onChangeDay: (Long) -> Unit,
  onToday: () -> Unit,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(tab.title, fontWeight = FontWeight.Bold) },
        actions = { if (tab == Tab.DIARY) DateNav(date, onChangeDay, onToday) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
    bottomBar = {
      NavigationBar {
        Tab.entries.forEach { item ->
          NavigationBarItem(
            selected = item == tab,
            onClick = { onSelectTab(item) },
            icon = { CaloIcon(item.icon, contentDescription = null) },
            label = { Text(item.label) },
          )
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbar) },
    containerColor = MaterialTheme.colorScheme.background,
    content = content,
  )
}

/** ‹ Сегодня › in the top bar of the Diary screen. */
@Composable
private fun DateNav(date: LocalDate, onChangeDay: (Long) -> Unit, onToday: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = { onChangeDay(-1) }) { CaloIcon(R.drawable.ic_left, "Предыдущий день") }
    TextButton(onClick = onToday) { Text(dayLabel(date), fontWeight = FontWeight.SemiBold) }
    IconButton(onClick = { onChangeDay(1) }) { CaloIcon(R.drawable.ic_right, "Следующий день") }
  }
}

@Preview(name = "App – Diary", showBackground = true, widthDp = 390, heightDp = 860)
@Composable
private fun AppPreview() {
  CaloScanTheme {
    AppScaffold(Tab.DIARY, SampleData.today, remember { SnackbarHostState() }, {}, {}, {}) { padding ->
      Box(Modifier.fillMaxSize().padding(padding)) {
        DiaryScreen(SampleData.todayEntries, SampleData.targets, {}, {}, {})
      }
    }
  }
}

@Preview(name = "App – Diary (dark)", showBackground = true, widthDp = 390, heightDp = 860, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppDarkPreview() {
  CaloScanTheme(darkTheme = true) {
    AppScaffold(Tab.DIARY, SampleData.today, remember { SnackbarHostState() }, {}, {}, {}) { padding ->
      Box(Modifier.fillMaxSize().padding(padding)) {
        DiaryScreen(SampleData.todayEntries, SampleData.targets, {}, {}, {})
      }
    }
  }
}
