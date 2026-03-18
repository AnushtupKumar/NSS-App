package com.example.nssapp.feature.student.presentation.face

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nssapp.util.FaceAnalyzer
import com.example.nssapp.util.FaceRecognizer
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color

@Composable
fun FaceRegistrationScreen(
    studentId: String,
    onRegistrationSuccess: () -> Unit,
    faceRecognizer: FaceRecognizer,
    onSaveEmbedding: (List<Float>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var registrationMessage by remember { mutableStateOf("Please smile at the camera to register your face.") }
    
    val haptic = LocalHapticFeedback.current
    var collectedEmbeddings by remember { mutableStateOf(listOf<FloatArray>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraExecutor = Executors.newSingleThreadExecutor()
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    cameraExecutor,
                                    FaceAnalyzer(
                                        faceRecognizer = faceRecognizer,
                                        onFaceDetected = { _, embedding ->
                                            coroutineScope.launch {
                                                if (embedding == null && !isRegistering) {
                                                    registrationMessage = "Face detected, but AI failed to decode embedding. Hold still."
                                                } else if (embedding != null && !isRegistering) {
                                                    if (collectedEmbeddings.size < 10) {
                                                        collectedEmbeddings = collectedEmbeddings + embedding
                                                        registrationMessage = "Keep smiling! Capturing frame ${collectedEmbeddings.size}/10..."
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    
                                                    if (collectedEmbeddings.size >= 10 && !isRegistering) {
                                                        isRegistering = true
                                                        registrationMessage = "Face detected! Saving to profile..."
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        
                                                        val size = collectedEmbeddings[0].size
                                                        val averaged = FloatArray(size) { i ->
                                                            collectedEmbeddings.map { it[i] }.average().toFloat()
                                                        }
                                                        
                                                        val normalizedAveraged = faceRecognizer.normalizeEmbedding(averaged)
                                                        onSaveEmbedding(normalizedAveraged.toList())
                                                        
                                                        kotlinx.coroutines.delay(1000)
                                                        registrationMessage = "Registration Successful!"
                                                        onRegistrationSuccess()
                                                    }
                                                }
                                            }
                                        },
                                        onError = { errorMsg ->
                                            registrationMessage = errorMsg
                                        }
                                    )
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("FaceRegistration", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Face Circle Guide Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                 Surface(
                    color = Color.Transparent,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(340.dp)
                ) {}
            }

            // UI Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = registrationMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isRegistering) {
                    CircularProgressIndicator()
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to register your face.")
        }
    }
}
