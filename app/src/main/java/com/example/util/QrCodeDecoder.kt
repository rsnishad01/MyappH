package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

object QrCodeDecoder {

    /**
     * Decodes a QR code from a content Uri (Gallery image, file, etc.).
     */
    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                decodeQrFromBitmap(bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a QR code from a Bitmap, attempting multiple binarizers and scale optimizations.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        // Attempt 1: Full resolution bitmap
        decodeBitmapInternal(bitmap)?.let { return it }

        // Attempt 2: If image is high resolution, downscale for better QR contrast / edge detection
        val maxDimension = 1200
        if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            if (scaledWidth > 0 && scaledHeight > 0) {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                decodeBitmapInternal(scaledBitmap)?.let { return it }
            }
        }

        // Attempt 3: Try slightly smaller scale (e.g. 600px) for screenshots with dense pixels
        val minDimension = 600
        if (bitmap.width > minDimension || bitmap.height > minDimension) {
            val scale = minDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            if (scaledWidth > 0 && scaledHeight > 0) {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                decodeBitmapInternal(scaledBitmap)?.let { return it }
            }
        }

        return null
    }

    private fun decodeBitmapInternal(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )

        // Pass 1: HybridBinarizer (adaptive binarization)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {}

        // Pass 2: GlobalHistogramBinarizer (high contrast images / flat screenshots)
        try {
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {}

        // Pass 3: Inverted luminance (dark mode / inverted QR codes)
        try {
            val invertedSource = source.invert()
            val binaryBitmap = BinaryBitmap(HybridBinarizer(invertedSource))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {}

        // Pass 4: Inverted with GlobalHistogramBinarizer
        try {
            val invertedSource = source.invert()
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(invertedSource))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {}

        return null
    }
}
