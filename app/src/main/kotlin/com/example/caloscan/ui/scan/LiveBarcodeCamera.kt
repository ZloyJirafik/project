package com.example.caloscan.ui.scan

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.caloscan.R
import com.example.caloscan.scan.BarcodeAnalyzer
import com.example.caloscan.ui.components.CaloIcon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.Executors

/**
 * Camera preview that reads barcodes and QR codes (CameraX + ML Kit).
 * The camera turns on when this is shown and off when it's removed or the app
 * goes to the background.
 * [onOtherCode] is called while the camera sees a code without a product barcode in it.
 */
@Composable
fun LiveBarcodeCamera(
  onBarcode: (String) -> Unit,
  onError: (Throwable) -> Unit,
  modifier: Modifier = Modifier,
  onOtherCode: () -> Unit = {},
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnBarcode by rememberUpdatedState(onBarcode)
  val currentOnOtherCode by rememberUpdatedState(onOtherCode)
  val currentOnError by rememberUpdatedState(onError)
  val previewView = remember {
    PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
  }
  var camera by remember { mutableStateOf<Camera?>(null) }
  var torchOn by remember { mutableStateOf(false) }

  LaunchedEffect(lifecycleOwner) {
    val executor = Executors.newSingleThreadExecutor()
    val analyzer = BarcodeAnalyzer(
      onBarcode = { code -> currentOnBarcode(code) },
      onOtherCode = { currentOnOtherCode() },
    )
    var provider: ProcessCameraProvider? = null
    try {
      val cameraProvider = ProcessCameraProvider.awaitInstance(context)
      provider = cameraProvider
      val preview = Preview.Builder().build()
      preview.setSurfaceProvider(previewView.surfaceProvider)
      val analysis = ImageAnalysis.Builder()
        // 1280×720 is enough to read small barcodes and still fast
        .setResolutionSelector(
          ResolutionSelector.Builder()
            .setResolutionStrategy(
              ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
            )
            .build(),
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
      analysis.setAnalyzer(executor, analyzer)
      cameraProvider.unbindAll()
      camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
      awaitCancellation()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      currentOnError(e)
    } finally {
      provider?.unbindAll()
      camera = null
      analyzer.close()
      executor.shutdown()
    }
  }

  // Flashlight
  LaunchedEffect(camera, torchOn) {
    camera?.cameraControl?.enableTorch(torchOn)
  }

  Box(modifier.fillMaxSize()) {
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    ScanFrame(Modifier.fillMaxSize())
    if (camera?.cameraInfo?.hasFlashUnit() == true) {
      FilledIconToggleButton(
        checked = torchOn,
        onCheckedChange = { torchOn = it },
        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
          containerColor = Color.Black.copy(alpha = 0.45f),
          contentColor = Color.White,
        ),
      ) {
        CaloIcon(R.drawable.ic_flash, contentDescription = if (torchOn) "Выключить фонарик" else "Включить фонарик")
      }
    }
  }
}

/** The white frame with a moving red line that shows where to hold the barcode. */
@Composable
fun ScanFrame(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "laser")
  val laser by transition.animateFloat(
    initialValue = 0.15f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
    label = "laser",
  )
  Canvas(modifier) {
    val w = size.width * 0.78f
    val h = size.height * 0.42f
    val left = (size.width - w) / 2
    val top = (size.height - h) / 2
    drawRoundRect(
      color = Color.White.copy(alpha = 0.9f),
      topLeft = Offset(left, top),
      size = GeometrySize(w, h),
      cornerRadius = CornerRadius(16.dp.toPx()),
      style = Stroke(width = 2.5.dp.toPx()),
    )
    val y = top + h * laser
    drawLine(
      color = Color(0xFFFF5A36),
      start = Offset(left + 12.dp.toPx(), y),
      end = Offset(left + w - 12.dp.toPx(), y),
      strokeWidth = 2.dp.toPx(),
    )
  }
}
