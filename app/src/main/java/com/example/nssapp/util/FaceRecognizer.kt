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
    // MobileFaceNet usually outputs a 192-dimensional embedding
    private val embeddingSize = 192 
    private val imageSize = 112 // MobileFaceNet standard input size

    init {
        try {
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(loadModelFile(context, "mobile_face_net.tflite"), options)
        } catch (e: Exception) {
            Log.e("FaceRecognizer", "Error interpreting model. Ensure mobile_face_net.tflite is in assets.", e)
            interpreter = null
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

        val inputTensor = interpreter?.getInputTensor(0) ?: return null
        val outputTensor = interpreter?.getOutputTensor(0) ?: return null

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(imageSize, imageSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f)) // Normalize to [-1, 1]
            .build()
        
        var tensorImage = TensorImage(inputTensor.dataType())
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputArray = Array(1) { FloatArray(embeddingSize) }
        interpreter?.run(tensorImage.buffer, outputArray)

        return normalizeEmbedding(outputArray[0])
    }

    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (value in embedding) {
            sum += value * value
        }
        val norm = sqrt(sum.toDouble()).toFloat()
        for (i in embedding.indices) {
            embedding[i] = embedding[i] / norm
        }
        return embedding
    }

    /**
     * Calculates Cosine Similarity between two normalized embeddings.
     * Returns a value between -1.0 and 1.0. 
     * Closer to 1.0 means more similar.
     */
    fun calculateSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        var dotProduct = 0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
        }
        return dotProduct
    }

    fun isSamePerson(embedding1: FloatArray, embedding2: FloatArray, threshold: Float = 0.75f): Boolean {
        val similarity = calculateSimilarity(embedding1, embedding2)
        Log.d("FaceRecognizer", "Computed Similarity: $similarity")
        return similarity >= threshold
    }
}
