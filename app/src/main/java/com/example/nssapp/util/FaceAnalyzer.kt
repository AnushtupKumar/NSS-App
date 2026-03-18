package com.example.nssapp.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
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

enum class LivenessChallenge(val message: String) {
    SMILE("Please SMILE broadly..."),
    BLINK("Please BLINK your eyes..."),
    TURN_HEAD("Turn your head slightly to either side...")
}

private enum class LivenessState {
    WAITING_FOR_NEUTRAL,
    WAITING_FOR_CHALLENGE,
    PASSED
}

class FaceAnalyzer(
    private val faceRecognizer: FaceRecognizer,
    private val targetEmbedding: FloatArray? = null,
    private val onFaceDetected: (Bitmap, FloatArray?) -> Unit,
    private val onVerificationResult: ((Boolean) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    // --- Active Anti-Video Spoofing State Machine ---
    private var livenessState = LivenessState.WAITING_FOR_NEUTRAL
    private var activeChallenge: LivenessChallenge? = null
    private var blinkStateClosed = false
    
    // Pre-Liveness Identity Lock 
    private var isCurrentFaceMatched: Boolean? = null
    private var currentLiveFaceId: Int? = null

    // High accuracy needed to properly crop the face out
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Fix lag by dropping heavy Accurate mode
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Required for probabilities
            .enableTracking() // Highly efficient ML Kit tracking lock
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!faceRecognizer.isModelLoaded) {
            onError?.invoke("AI Model failed to load. Please check file assets.")
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        // Always grab the largest/closest face to prevent background faces from stealing the lock
                        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        
                        if (face != null) {
                            val trackingId = face.trackingId
                            val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
                            val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
                            val smileProb = face.smilingProbability ?: 0f
                            val eulerY = face.headEulerAngleY // Turn left/right
                            
                            // If we have ALREADY authenticated the face, but the tracking ID drops or changes 
                            // (e.g., someone swapped places), instantly reset everything to force re-authentication!
                            if (isCurrentFaceMatched == true && trackingId != null && trackingId != currentLiveFaceId) {
                                isCurrentFaceMatched = null
                                livenessState = LivenessState.WAITING_FOR_NEUTRAL
                                activeChallenge = null
                                blinkStateClosed = false
                            }
                            
                            // --- Step 1: Pre-Liveness Identity Verification ---
                            // Instantly reject strangers before asking them to perform silly liveness challenges.
                            // If they fail, allow them to keep trying to center their face!
                            if (targetEmbedding != null && isCurrentFaceMatched != true) {
                                if (leftEyeOpen > 0.6f && rightEyeOpen > 0.6f && eulerY in -15f..15f) { // Need eyes open and face straight for clear mapping
                                    val bitmap = imageProxy.toRotatedBitmap()
                                    if (bitmap != null) {
                                        val croppedFace = cropFace(bitmap, face.boundingBox)
                                        val embedding = faceRecognizer.getEmbedding(croppedFace)
                                        
                                        if (embedding != null) {
                                            isCurrentFaceMatched = faceRecognizer.isSamePerson(embedding, targetEmbedding)
                                            if (isCurrentFaceMatched == true) {
                                                // Lock the tracking ID onto this successfully authenticated face!
                                                currentLiveFaceId = trackingId
                                            }
                                        }
                                    }
                                } else {
                                    onError?.invoke("Keep eyes open and look straight for initial scan...")
                                    imageProxy.close()
                                    return@addOnSuccessListener
                                }
                            }
                            
                            // If they continuously fail the pre-liveness check, keep spamming them until they align or leave.
                            if (isCurrentFaceMatched == false) {
                                onError?.invoke("Identity Mismatch! Keep looking straight to retry...")
                                imageProxy.close()
                                return@addOnSuccessListener
                            }


                            // --- Step 2: Active Liveness Detection (Anti-Video Spoofing) ---
                            // Only reached if registering a new face (targetEmbedding == null) OR they passed Step 1!
                            if (livenessState != LivenessState.PASSED) {
                                when (livenessState) {
                                    LivenessState.WAITING_FOR_NEUTRAL -> {
                                        if (eulerY in -15f..15f && leftEyeOpen > 0.8f && rightEyeOpen > 0.8f && smileProb < 0.2f) {
                                            // Assign a random challenge that a video can't predict
                                            activeChallenge = listOf(LivenessChallenge.SMILE, LivenessChallenge.BLINK, LivenessChallenge.TURN_HEAD).random()
                                            livenessState = LivenessState.WAITING_FOR_CHALLENGE
                                        } else {
                                            onError?.invoke("Look straight, keep neutral face, open eyes.")
                                        }
                                    }
                                    LivenessState.WAITING_FOR_CHALLENGE -> {
                                        onError?.invoke(activeChallenge?.message ?: "")
                                        var challengePassed = false
                                        
                                        when (activeChallenge) {
                                            LivenessChallenge.SMILE -> {
                                                if (smileProb > 0.7f) challengePassed = true
                                            }
                                            LivenessChallenge.BLINK -> {
                                                if (leftEyeOpen < 0.2f && rightEyeOpen < 0.2f) {
                                                    blinkStateClosed = true
                                                } else if (blinkStateClosed && leftEyeOpen > 0.8f && rightEyeOpen > 0.8f) {
                                                    challengePassed = true
                                                }
                                            }
                                            LivenessChallenge.TURN_HEAD -> {
                                                if (eulerY < -25f || eulerY > 25f) challengePassed = true
                                            }
                                            null -> {}
                                        }
                                        
                                        if (challengePassed) {
                                            onError?.invoke("Liveness confirmed! Look straight and hold still...")
                                            livenessState = LivenessState.PASSED
                                        }
                                    }
                                    LivenessState.PASSED -> {} // Fallthrough
                                }
                                
                                imageProxy.close()
                                return@addOnSuccessListener
                            }
                            
                            // --- Step 3: Final Face Extraction ---
                            // Ensure they look back at the camera before taking the final embedding.
                            // This mathematically guarantees that if they swapped faces midway through liveness, Step 3 instantly catches the imposter!
                            if (eulerY in -15f..15f && leftEyeOpen > 0.6f && rightEyeOpen > 0.6f) {
                                val bitmap = imageProxy.toRotatedBitmap()
                                if (bitmap != null) {
                                    val croppedFace = cropFace(bitmap, face.boundingBox)
                                    val embedding = faceRecognizer.getEmbedding(croppedFace)
                                    
                                    onFaceDetected(croppedFace, embedding)

                                    if (targetEmbedding != null && embedding != null) {
                                        val isMatch = faceRecognizer.isSamePerson(embedding, targetEmbedding)
                                        // Final mathematical lock: Verify the identity of the person who just completed the Liveness prompt!
                                        onVerificationResult?.invoke(isMatch)
                                    } else if (targetEmbedding != null && embedding == null) {
                                        onError?.invoke("Face detected, but AI failed to decode embedding.")
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    onError?.invoke("ML Kit Face detection crashed.")
                    Log.e("FaceAnalyzer", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun ImageProxy.toRotatedBitmap(): Bitmap? {
        val rawBitmap = this.toBitmap()
        val rotationDegrees = this.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return rawBitmap
        
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }

    private fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        // Use 5% padding. Too much padding introduces background noise which confuses FaceNet models.
        val paddingX = (boundingBox.width() * 0.05f).toInt()
        val paddingY = (boundingBox.height() * 0.05f).toInt()

        val left = (boundingBox.left - paddingX).coerceAtLeast(0)
        val top = (boundingBox.top - paddingY).coerceAtLeast(0)
        val right = (boundingBox.right + paddingX).coerceAtMost(bitmap.width)
        val bottom = (boundingBox.bottom + paddingY).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return bitmap
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
