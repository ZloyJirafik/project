package com.example.caloscan.scan

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
 * Only product barcodes are read: EAN-13, EAN-8, UPC-A and UPC-E. ML Kit checks
 * their check digit, so misreads are rare.
 */

private val productFormats: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
  .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
  .build()

private fun List<Barcode>.firstProductCode(): String? =
  firstNotNullOfOrNull { barcode -> barcode.rawValue?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) } }

/** Reads a barcode from a photo. Returns null if there is none. */
object PhotoBarcodeReader {
  suspend fun read(context: Context, uri: Uri): String? {
    val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
    val scanner = BarcodeScanning.getClient(productFormats)
    try {
      return scanner.process(image).await().firstProductCode()
    } finally {
      scanner.close()
    }
  }
}

/**
 * Looks at camera frames as they arrive. Calls [onBarcode] once, after the same
 * code was seen in two frames in a row (extra safety against misreads).
 */
class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer, Closeable {
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
