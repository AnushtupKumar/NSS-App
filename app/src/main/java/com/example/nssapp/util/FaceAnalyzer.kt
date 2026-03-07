package com.example.nssapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.nssapp.core.domain.model.Student
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class FaceAnalyzer(
    private val faceRecognizer: FaceRecognizer,
    private val targetEmbedding: FloatArray? = null,
    private val onFaceDetected: (Bitmap, FloatArray?) -> Unit,
    private val onVerificationResult: ((Boolean) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    // High accuracy needed to properly crop the face out
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // For liveness (smile/eyes open)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces.first()
                        
                        // Basic Liveness Check (Must be smiling to prevent some photo spoofs)
                        val smilingProbability = face.smilingProbability ?: 0f
                        if (smilingProbability > 0.4f) {
                            val bitmap = imageProxy.toBitmap()
                            if (bitmap != null) {
                                val croppedFace = cropFace(bitmap, face.boundingBox)
                                val embedding = faceRecognizer.getEmbedding(croppedFace)
                                
                                onFaceDetected(croppedFace, embedding)

                                if (targetEmbedding != null && embedding != null) {
                                    val isMatch = faceRecognizer.isSamePerson(embedding, targetEmbedding)
                                    onVerificationResult?.invoke(isMatch)
                                }
                            }
                        } else {
                            Log.d("FaceAnalyzer", "Face detected, but user is not smiling (liveness check).")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        // Ensure bounds are inside the image
        val x = boundingBox.left.coerceAtLeast(0)
        val y = boundingBox.top.coerceAtLeast(0)
        // Ensure width and height don't exceed the bitmap bounds
        val width = boundingBox.width().coerceAtMost(bitmap.width - x)
        val height = boundingBox.height().coerceAtMost(bitmap.height - y)

        if (width <= 0 || height <= 0) return bitmap

        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }
}
