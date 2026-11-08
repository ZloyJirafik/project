package com.example.caloscan.scan

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.caloscan.domain.ProductCode
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * Barcode reading with Google ML Kit. The model is bundled in the app, so it
 * works offline and without Google Play services.
 *
 * Reads product barcodes (EAN-13, EAN-8, UPC-A, UPC-E) and the square codes some
 * packages have: QR codes and DataMatrix (for example «Честный ЗНАК»). A square
 * code is only used when the product's barcode number is in it (see ProductCode).
 * ML Kit checks the check digit of barcodes, so misreads are rare.
 */

private val productFormats: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
  .setBarcodeFormats(
    Barcode.FORMAT_EAN_13,
    Barcode.FORMAT_EAN_8,
    Barcode.FORMAT_UPC_A,
    Barcode.FORMAT_UPC_E,
    Barcode.FORMAT_QR_CODE,
    Barcode.FORMAT_DATA_MATRIX,
  )
  .build()

/** The product's barcode number, or null for a code without one (e.g. a QR code with a website link). */
private fun Barcode.productCode(): String? {
  val raw = rawValue ?: return null
  return when (format) {
    Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX -> ProductCode.fromText(raw)
    else -> raw.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
  }
}

private fun List<Barcode>.firstProductCode(): String? = firstNotNullOfOrNull { it.productCode() }

/** What was found in a photo. */
sealed interface PhotoScan {
  data class Found(val code: String) : PhotoScan

  /** Only codes without a product barcode in them, e.g. a QR code with a website link. */
  data object OtherCode : PhotoScan

  data object NoCode : PhotoScan
}

/** Reads a barcode or QR code from a photo. */
object PhotoBarcodeReader {
  suspend fun read(context: Context, uri: Uri): PhotoScan {
    val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
    val scanner = BarcodeScanning.getClient(productFormats)
    try {
      val codes = scanner.process(image).await()
      val code = codes.firstProductCode()
      return when {
        code != null -> PhotoScan.Found(code)
        codes.isNotEmpty() -> PhotoScan.OtherCode
        else -> PhotoScan.NoCode
      }
    } finally {
      scanner.close()
    }
  }
}

/**
 * Looks at camera frames as they arrive. Calls [onBarcode] once, after the same
 * code was seen in two frames in a row (extra safety against misreads).
 * Calls [onOtherCode] while it sees only codes without a product barcode in them.
 */
class BarcodeAnalyzer(
  private val onBarcode: (String) -> Unit,
  private val onOtherCode: () -> Unit = {},
) : ImageAnalysis.Analyzer, Closeable {
  private val scanner = BarcodeScanning.getClient(productFormats)
  private var lastCode: String? = null

  @Volatile
  private var delivered = false

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(image: ImageProxy) {
    val frame = image.image
    if (frame == null || delivered) {
      image.close()
      return
    }
    val input = InputImage.fromMediaImage(frame, image.imageInfo.rotationDegrees)
    scanner.process(input)
      // Listeners run on the main thread
      .addOnSuccessListener { barcodes ->
        val code = barcodes.firstProductCode()
        if (code == null && barcodes.isNotEmpty() && !delivered) onOtherCode()
        if (code != null && code == lastCode && !delivered) {
          delivered = true
          onBarcode(code)
        }
        lastCode = code
      }
      .addOnCompleteListener { image.close() }
  }

  override fun close() = scanner.close()
}

/** Waits for a Google Play services Task without blocking. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
  addOnSuccessListener { cont.resume(it) }
  addOnFailureListener { cont.resumeWithException(it) }
  addOnCanceledListener { cont.cancel() }
}
