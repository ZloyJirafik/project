package com.example.caloscan.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caloscan.R
import com.example.caloscan.data.model.Food
import com.example.caloscan.scan.PhotoBarcodeReader
import com.example.caloscan.scan.PhotoScan
import com.example.caloscan.ui.CaloScanViewModel
import com.example.caloscan.ui.components.CaloCard
import com.example.caloscan.ui.components.CaloIcon
import com.example.caloscan.ui.components.CardTitle
import com.example.caloscan.ui.components.FoodListCard
import com.example.caloscan.ui.components.MutedText
import com.example.caloscan.ui.preview.PreviewScreen
import com.example.caloscan.ui.preview.SampleData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What the camera area shows. */
enum class Viewfinder { IDLE, LIVE, PAUSED, DENIED, NO_CAMERA, ERROR }

private const val EXAMPLE_BARCODE = "3017620422003"

/** A QR code with, for example, a link to the brand's website instead of the product number. */
private const val NOT_A_PRODUCT_CODE = "В этом QR-коде нет номера товара. Отсканируйте штрихкод на упаковке."

private fun hasCameraPermission(context: Context) =
  ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
fun ScanRoute(vm: CaloScanViewModel) {
  val context = LocalContext.current
  val activity = LocalActivity.current
  val scope = rememberCoroutineScope()
  val sheet by vm.sheet.collectAsStateWithLifecycle()
  val products by vm.savedProducts.collectAsStateWithLifecycle()

  val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
  var permitted by remember { mutableStateOf(hasCameraPermission(context)) }
  // If camera access was already allowed, the camera starts by itself
  var cameraWanted by rememberSaveable { mutableStateOf(permitted) }
  var denied by rememberSaveable { mutableStateOf(false) }
  var askInSettings by rememberSaveable { mutableStateOf(false) }
  var cameraFailed by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf("") }

  // Coming back from the phone's settings with camera access allowed
  LifecycleResumeEffect(Unit) {
    if (!permitted && hasCameraPermission(context)) {
      permitted = true
      denied = false
      cameraWanted = true
    }
    onPauseOrDispose { }
  }

  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    permitted = granted
    if (granted) {
      denied = false
      cameraWanted = true
    } else {
      denied = true
      // Android stops showing the question after the user said no twice
      askInSettings = activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
  }

  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      status = "Считываем код с фото…"
      val result = try {
        PhotoBarcodeReader.read(context, uri)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        status = ""
        vm.showMessage("Не удалось открыть изображение.")
        return@launch
      }
      when (result) {
        is PhotoScan.Found -> {
          status = "Отсканировано: ${result.code}"
          vm.lookupBarcode(result.code)
        }
        PhotoScan.OtherCode -> {
          status = ""
          vm.showMessage(NOT_A_PRODUCT_CODE)
        }
        PhotoScan.NoCode -> {
          status = ""
          vm.showMessage("Код не найден. Сфотографируйте ближе и чётче, чтобы штрихкод или QR-код был ровным.")
        }
      }
    }
  }

  val viewfinder = when {
    !hasCamera -> Viewfinder.NO_CAMERA
    cameraFailed -> Viewfinder.ERROR
    !permitted && denied -> Viewfinder.DENIED
    !permitted || !cameraWanted -> Viewfinder.IDLE
    sheet != null -> Viewfinder.PAUSED // a product is open; the camera rests until it's closed
    else -> Viewfinder.LIVE
  }

  ScanScreen(
    viewfinder = viewfinder,
    askInSettings = askInSettings,
    status = status,
    recentlyScanned = products.take(5),
    onStartCamera = {
      cameraFailed = false
      if (permitted) cameraWanted = true else permissionLauncher.launch(Manifest.permission.CAMERA)
    },
    onOpenSettings = {
      context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
      )
    },
    onScanPhoto = {
      photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    },
    onLookup = vm::lookupBarcode,
    onOpenFood = vm::openFood,
    liveCamera = {
      LiveBarcodeCamera(
        onBarcode = { code ->
          status = "Отсканировано: $code"
          vm.lookupBarcode(code)
        },
        onError = { cameraFailed = true },
        onOtherCode = { status = NOT_A_PRODUCT_CODE },
      )
    },
  )
}

/** Scan with the camera, from a photo, or type the number. */
@Composable
fun ScanScreen(
  viewfinder: Viewfinder,
  askInSettings: Boolean,
  status: String,
  recentlyScanned: List<Food>,
  onStartCamera: () -> Unit,
  onOpenSettings: () -> Unit,
  onScanPhoto: () -> Unit,
  onLookup: (String) -> Unit,
  onOpenFood: (Food) -> Unit,
  modifier: Modifier = Modifier,
  liveCamera: @Composable () -> Unit = {},
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      CaloCard {
        Box(
          Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF151513)),
          contentAlignment = Alignment.Center,
        ) {
          when (viewfinder) {
            Viewfinder.LIVE -> liveCamera()
            Viewfinder.IDLE -> Overlay(
              R.drawable.ic_scan,
              "Наведите камеру на штрихкод или QR-код на упаковке — откроется карточка товара.",
            ) { Button(onClick = onStartCamera) { Text("Включить камеру") } }
            Viewfinder.PAUSED -> Overlay(R.drawable.ic_scan, "Камера на паузе, пока открыт товар.")
            Viewfinder.DENIED -> Overlay(
              R.drawable.ic_camera_off,
              if (askInSettings) {
                "Доступ к камере выключен. Разрешите его в настройках приложения или отсканируйте фото."
              } else {
                "Камера нужна, чтобы читать штрихкоды и QR-коды. Можно также отсканировать фото или ввести номер."
              },
            ) {
              if (askInSettings) {
                Button(onClick = onOpenSettings) { Text("Открыть настройки") }
              } else {
                Button(onClick = onStartCamera) { Text("Разрешить камеру") }
              }
            }
            Viewfinder.NO_CAMERA -> Overlay(
              R.drawable.ic_camera_off,
              "На устройстве нет камеры. Отсканируйте фото или введите номер."
            )
            Viewfinder.ERROR -> Overlay(
              R.drawable.ic_camera_off,
              "Не удалось включить камеру. Возможно, её использует другое приложение.",
            ) { Button(onClick = onStartCamera) { Text("Повторить") } }
          }
        }
        val hint = when {
          status.isNotEmpty() -> status
          viewfinder == Viewfinder.LIVE -> "Держите телефон ровно — код считается сам"
          else -> ""
        }
        if (hint.isNotEmpty()) {
          MutedText(hint, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        OutlinedButton(
          onClick = onScanPhoto,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
          CaloIcon(R.drawable.ic_photo, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Сканировать с фото")
        }
      }
    }
    item { ManualBarcodeCard(onLookup) }
    if (recentlyScanned.isNotEmpty()) {
      item { FoodListCard("Недавно отсканированные", recentlyScanned, onOpenFood) }
    }
  }
}

@Composable
private fun Overlay(@DrawableRes icon: Int, text: String, action: (@Composable () -> Unit)? = null) {
  Column(
    Modifier.fillMaxSize().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CaloIcon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.85f))
    Spacer(Modifier.height(12.dp))
    Text(text, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
    if (action != null) {
      Spacer(Modifier.height(16.dp))
      action()
    }
  }
}

@Composable
private fun ManualBarcodeCard(onLookup: (String) -> Unit) {
  var code by rememberSaveable { mutableStateOf("") }
  CaloCard {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      CardTitle("Ввести штрихкод")
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = code,
          onValueChange = { text -> code = text.filter(Char::isDigit).take(14) },
          modifier = Modifier.weight(1f),
          placeholder = { Text("Цифры под полосками") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onLookup(code) }),
        )
        Button(onClick = { onLookup(code) }, enabled = code.isNotEmpty(), modifier = Modifier.height(56.dp)) {
          Text("Найти")
        }
      }
      // A tappable line of text (a TextButton clipped the first letters when the text wrapped)
      Text(
        "Нет товара под рукой? Попробуйте $EXAMPLE_BARCODE (Nutella)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable {
            code = EXAMPLE_BARCODE
            onLookup(EXAMPLE_BARCODE)
          }
          .padding(vertical = 6.dp),
      )
    }
  }
}

@Preview(name = "Scan", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ScanPreview() {
  PreviewScreen {
    ScanScreen(
      viewfinder = Viewfinder.IDLE,
      askInSettings = false,
      status = "",
      recentlyScanned = listOf(SampleData.nutella),
      onStartCamera = {},
      onOpenSettings = {},
      onScanPhoto = {},
      onLookup = {},
      onOpenFood = {},
    )
  }
}

@Preview(name = "Scan – camera blocked", showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun ScanDeniedPreview() {
  PreviewScreen {
    ScanScreen(
      viewfinder = Viewfinder.DENIED,
      askInSettings = true,
      status = "",
      recentlyScanned = emptyList(),
      onStartCamera = {},
      onOpenSettings = {},
      onScanPhoto = {},
      onLookup = {},
      onOpenFood = {},
    )
  }
}
