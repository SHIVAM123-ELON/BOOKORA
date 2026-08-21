package com.example.presentation.scanner

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrBarcodeAnalyzer(
    private val onBarcodeDetected: (barcodeValue: String, format: Int, boundingBox: Rect?) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @Volatile
    private var lastScannedCode: String? = null

    @Volatile
    private var lastScannedTimestamp: Long = 0L

    private val cooldownMillis = 1500L

    var isScanningEnabled: Boolean = true

    fun resetCooldown() {
        lastScannedCode = null
        lastScannedTimestamp = 0L
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningEnabled) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (!isScanningEnabled) return@addOnSuccessListener

                val validBarcode = barcodes.firstOrNull { it.rawValue != null }
                if (validBarcode != null) {
                    val rawValue = validBarcode.rawValue ?: return@addOnSuccessListener
                    val currentTime = System.currentTimeMillis()

                    // Debounce if same barcode was scanned within cooldown window
                    if (rawValue != lastScannedCode || (currentTime - lastScannedTimestamp) > cooldownMillis) {
                        lastScannedCode = rawValue
                        lastScannedTimestamp = currentTime
                        onBarcodeDetected(rawValue, validBarcode.format, validBarcode.boundingBox)
                    }
                }
            }
            .addOnFailureListener {
                // Log or handle silent frame analysis errors
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
