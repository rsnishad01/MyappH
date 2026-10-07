package com.example.util.filter

import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import java.util.concurrent.Executors

/**
 * Real-time MediaPipe / MLKit Face Mesh analyzer for CameraX
 * Detects 468 3D face mesh landmarks for lip tint, eye brightening, and selective skin smoothing
 */
class FaceMeshHelper(
    private val onFaceLandmarksDetected: (FaceLandmarkData) -> Unit
) : ImageAnalysis.Analyzer {

    data class FaceLandmarkData(
        val hasFace: Boolean,
        val lipBounds: RectF = RectF(0f, 0f, 0f, 0f),      // Normalized 0.0 to 1.0 (xMin, yMin, xMax, yMax)
        val leftEyeBounds: RectF = RectF(0f, 0f, 0f, 0f),
        val rightEyeBounds: RectF = RectF(0f, 0f, 0f, 0f),
        val faceBounds: RectF = RectF(0f, 0f, 0f, 0f)
    )

    private val detector: FaceMeshDetector by lazy {
        val options = FaceMeshDetectorOptions.Builder()
            .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
            .build()
        FaceMeshDetection.getClient(options)
    }

    private var isProcessing = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faceMeshes ->
                if (faceMeshes.isNotEmpty()) {
                    val faceMesh = faceMeshes.first()
                    val landmarkData = extractLandmarkData(faceMesh, imageWidth.toFloat(), imageHeight.toFloat())
                    onFaceLandmarksDetected(landmarkData)
                } else {
                    onFaceLandmarksDetected(FaceLandmarkData(hasFace = false))
                }
            }
            .addOnFailureListener {
                onFaceLandmarksDetected(FaceLandmarkData(hasFace = false))
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun extractLandmarkData(faceMesh: FaceMesh, imgWidth: Float, imgHeight: Float): FaceLandmarkData {
        val allPoints = faceMesh.allPoints
        if (allPoints.isEmpty() || imgWidth <= 0f || imgHeight <= 0f) {
            return FaceLandmarkData(hasFace = false)
        }

        // Indices for key facial regions in 468 MediaPipe Face Mesh:
        // Lips: 61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 13
        val lipIndices = intArrayOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 13)
        // Left Eye: 33, 133, 160, 159, 158, 157, 173, 144, 145, 153, 154, 155
        val leftEyeIndices = intArrayOf(33, 133, 160, 159, 158, 157, 173, 144, 145, 153, 154, 155)
        // Right Eye: 362, 263, 387, 386, 385, 384, 398, 373, 374, 380, 381, 382
        val rightEyeIndices = intArrayOf(362, 263, 387, 386, 385, 384, 398, 373, 374, 380, 381, 382)

        val lipBounds = calculateNormalizedBounds(allPoints, lipIndices, imgWidth, imgHeight)
        val leftEyeBounds = calculateNormalizedBounds(allPoints, leftEyeIndices, imgWidth, imgHeight)
        val rightEyeBounds = calculateNormalizedBounds(allPoints, rightEyeIndices, imgWidth, imgHeight)

        val boundingBox = faceMesh.boundingBox
        val faceBounds = RectF(
            (boundingBox.left / imgWidth).coerceIn(0f, 1f),
            (boundingBox.top / imgHeight).coerceIn(0f, 1f),
            (boundingBox.right / imgWidth).coerceIn(0f, 1f),
            (boundingBox.bottom / imgHeight).coerceIn(0f, 1f)
        )

        return FaceLandmarkData(
            hasFace = true,
            lipBounds = lipBounds,
            leftEyeBounds = leftEyeBounds,
            rightEyeBounds = rightEyeBounds,
            faceBounds = faceBounds
        )
    }

    private fun calculateNormalizedBounds(
        allPoints: List<FaceMeshPoint>,
        indices: IntArray,
        imgWidth: Float,
        imgHeight: Float
    ): RectF {
        var minX = 1.0f
        var minY = 1.0f
        var maxX = 0.0f
        var maxY = 0.0f

        for (idx in indices) {
            if (idx < allPoints.size) {
                val pt = allPoints[idx].position
                val normX = (pt.x / imgWidth).coerceIn(0f, 1f)
                val normY = (pt.y / imgHeight).coerceIn(0f, 1f)
                if (normX < minX) minX = normX
                if (normY < minY) minY = normY
                if (normX > maxX) maxX = normX
                if (normY > maxY) maxY = normY
            }
        }

        // Add padding around landmark region for smooth edge blending
        val padX = (maxX - minX) * 0.15f
        val padY = (maxY - minY) * 0.15f

        return RectF(
            (minX - padX).coerceIn(0f, 1f),
            (minY - padY).coerceIn(0f, 1f),
            (maxX + padX).coerceIn(0f, 1f),
            (maxY + padY).coerceIn(0f, 1f)
        )
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Exception) {}
    }
}
