package com.example.nssapp.feature.student.presentation.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState is ScanUiState.RequiresFaceScan) "Verify Identity" else "Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (hasCameraPermission) {
                
                val isVerifyingFace = uiState is ScanUiState.RequiresFaceScan
                val isCameraActive = uiState is ScanUiState.Idle || uiState is ScanUiState.RequiresFaceScan
                val targetEmbedding = (uiState as? ScanUiState.RequiresFaceScan)?.targetEmbedding
                val faceRecognizer = remember { com.example.nssapp.util.FaceRecognizer(context) }

                val previewView = remember {
                    PreviewView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                }

                LaunchedEffect(isVerifyingFace, targetEmbedding, isCameraActive) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        cameraProvider.unbindAll()
                        if (!isCameraActive) return@addListener
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        
                        if (isVerifyingFace && targetEmbedding != null) {
                            imageAnalysis.setAnalyzer(
                                Executors.newSingleThreadExecutor(),
                                com.example.nssapp.util.FaceAnalyzer(
                                    faceRecognizer = faceRecognizer,
                                    targetEmbedding = targetEmbedding,
                                    onFaceDetected = { _, _ -> },
                                    onVerificationResult = { isMatch ->
                                        viewModel.onFaceVerificationResult(isMatch)
                                    }
                                )
                            )
                        } else {
                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                                processImageProxy(imageProxy) { barcodes ->
                                    barcodes.forEach { barcode ->
                                        barcode.rawValue?.let { code ->
                                            viewModel.onCodeScanned(code)
                                        }
                                    }
                                }
                            }
                        }
                        
                        val cameraSelector = if (isVerifyingFace) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            Log.e("ScanScreen", "Use case binding failed", exc)
                        }
                        
                    }, ContextCompat.getMainExecutor(context))
                }

                if (isCameraActive) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { previewView }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Keep it blank while showing dialogs
                    }
                }
                
                // Overlay for scanning area or instructions
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isVerifyingFace) {
                        // QR Code Scanner Box
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 100.dp, bottom = 100.dp, start = 40.dp, end = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = Color.Transparent,
                                shape = MaterialTheme.shapes.extraLarge,
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(250.dp)
                            ) {}
                        }
                    } else {
                        // Face Verification Box (Circle)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                             Surface(
                                color = Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.size(280.dp)
                            ) {}
                        }
                    }
                    
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.BottomCenter) {
                         Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.large,
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                text = if (isVerifyingFace) "Smile at the camera to verify your identity" else "Align QR code within the frame",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
                
                // Result Handling
                val haptic = LocalHapticFeedback.current
                LaunchedEffect(uiState) {
                    if (uiState is ScanUiState.Success) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        kotlinx.coroutines.delay(1500)
                        navController.popBackStack()
                    } else if (uiState is ScanUiState.Error) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                
                when (val state = uiState) {
                    is ScanUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ScanUiState.Success -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.large,
                                shadowElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    is ScanUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.large,
                                shadowElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                        Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }

            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required to scan QR codes.")
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    onSuccess: (List<com.google.mlkit.vision.barcode.common.Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                onSuccess(barcodes)
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
