package com.example.nssapp.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceRecognizer(context: Context) {
    private var interpreter: Interpreter? = null
    var isModelLoaded = false
        private set

    init {
        try {
            val options = Interpreter.Options()
            options.setNumThreads(4)
            
            // Core Robustness: Try loading facenet first, fallback to mobile_face_net if user didn't download it
            try {
                interpreter = Interpreter(loadModelFile(context, "facenet.tflite"), options)
                Log.d("FaceRecognizer", "Successfully loaded facenet.tflite")
            } catch (e: Exception) {
                Log.w("FaceRecognizer", "facenet.tflite not found. Falling back to mobile_face_net.tflite")
                interpreter = Interpreter(loadModelFile(context, "mobile_face_net.tflite"), options)
                Log.d("FaceRecognizer", "Successfully loaded mobile_face_net.tflite")
            }
            isModelLoaded = true
        } catch (e: Exception) {
            Log.e("FaceRecognizer", "CRITICAL ERROR: No TFLite models found in assets folder. Face recognition will be disabled.", e)
            interpreter = null
            isModelLoaded = false
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray? {
        if (interpreter == null) return null

        try {
            val inputTensor = interpreter?.getInputTensor(0) ?: return null
            val outputTensor = interpreter?.getOutputTensor(0) ?: return null
            
            // Dynamic Robustness: Read the required sizes directly from whoever the model is!
            val requiredImageSize = inputTensor.shape()[1] // Usually 112 or 160
            val outputEmbeddingSize = outputTensor.shape()[1] // Usually 192 or 512

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(requiredImageSize, requiredImageSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(127.5f, 127.5f)) // Normalize pixel values to [-1, 1]
                .build()
            
            var tensorImage = TensorImage(inputTensor.dataType())
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            val outputArray = Array(1) { FloatArray(outputEmbeddingSize) }
            interpreter?.run(tensorImage.buffer, outputArray)

            return normalizeEmbedding(outputArray[0])
        } catch (e: Exception) {
            Log.e("FaceRecognizer", "Fatal error during embedding generation.", e)
            return null
        }
    }

    fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (value in embedding) {
            sum += value * value
        }
        val norm = sqrt(sum.toDouble()).toFloat()
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] = embedding[i] / norm
            }
        }
        return embedding
    }

    fun calculateSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        var dotProduct = 0f
        // Protect against size mismatches if models were swapped between enrollment and verification
        val minSize = minOf(embedding1.size, embedding2.size)
        for (i in 0 until minSize) {
            dotProduct += embedding1[i] * embedding2[i]
        }
        return dotProduct
    }

    fun isSamePerson(embedding1: FloatArray, embedding2: FloatArray, threshold: Float = 0.80f): Boolean {
        if (embedding1.size != embedding2.size) {
            Log.e("FaceRecognizer", "Dimension mismatch! Cannot accurately compare embeddings of different sizes (e.g., enrolled on MobileFaceNet but verifying on FaceNet).")
            return false
        }
        val similarity = calculateSimilarity(embedding1, embedding2)
        Log.d("FaceRecognizer", "Computed Similarity: $similarity")
        return similarity >= threshold
    }
}
