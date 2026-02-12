package com.example.nssapp.feature.admin.presentation.events

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.presentation.events.components.EventFormDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    navController: NavController,
    viewModel: AdminEventDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {

                    if (uiState is EventDetailUiState.Success) {
                        val state = uiState as EventDetailUiState.Success
                        IconButton(onClick = { viewModel.deleteEvent(eventId) { navController.popBackStack() } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is EventDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventDetailUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is EventDetailUiState.Success -> {
                    val successState = state
                    var showEditDialog by remember { mutableStateOf(false) }

                    if (showEditDialog) {
                        EventFormDialog(
                            initialEvent = successState.event,
                            wings = successState.wings,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { title, type, date, startTime, endTime, posHours, negHours, mandatory, targetWings, mandatoryWings ->
                                viewModel.updateEvent(successState.event.copy(
                                    title = title,
                                    type = type,
                                    date = date,
                                    startTime = startTime,
                                    endTime = endTime,
                                    positiveHours = posHours,
                                    negativeHours = negHours,
                                    mandatory = mandatory,
                                    targetWings = targetWings,
                                    mandatoryWings = mandatoryWings
                                ))
                                showEditDialog = false
                            }
                        )
                    }

                    // Floating Edit Button to ensure visibility if TopBar is crowded or just for convenience
                    Box(modifier = Modifier.fillMaxSize()) {
                        EventDetailContent(
                            event = successState.event,
                            wings = successState.wings, // Pass wings for display names
                            onStatusChange = { newStatus -> viewModel.updateStatus(eventId, newStatus) },
                            onExportParams = { viewModel.exportAttendance(eventId) },
                            onMarkBulk = { rolls, status, bypass -> 
                                viewModel.markBulkAttendance(eventId, rolls, status, bypass) { failedRolls ->
                                    scope.launch {
                                        if (failedRolls.isEmpty()) {
                                            snackbarHostState.showSnackbar("Attendance marked successfully!")
                                        } else {
                                            snackbarHostState.showSnackbar("Marked with failures: ${failedRolls.joinToString(",")}")
                                        }
                                    }
                                }
                            },
                            onAutoPenalty = {
                                viewModel.applyPenalty(eventId) { count ->
                                    scope.launch {
                                        val msg = if (count >= 0) "Penalty applied to $count absentees!" else "Failed to apply penalty."
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        )
                        
                        FloatingActionButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 80.dp) // Adjust for Snackbar? Or just normal
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailContent(
    event: Event,
    wings: List<Wing>,
    onStatusChange: (String) -> Unit,
    onExportParams: () -> Unit,
    onMarkBulk: (String, String, Boolean) -> Unit,
    onAutoPenalty: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    var showBulkDialog by remember { mutableStateOf(false) }
    
    if (showBulkDialog) {
        BulkAttendanceDialog(
            onDismiss = { showBulkDialog = false },
            onConfirm = { rolls, status, bypass ->
                onMarkBulk(rolls, status, bypass)
                showBulkDialog = false
            }
        )
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.titleMedium)
                    StatusChip(status = event.status)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Type: ${event.type}")
                Text("Date: ${dateFormat.format(Date(event.date))}")
                if (event.startTime > 0) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text("Time: ${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Hours: +${event.positiveHours} / -${event.negativeHours}", style = MaterialTheme.typography.bodyLarge)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Target Wings:", style = MaterialTheme.typography.titleSmall)
                val targetWingNames = wings.filter { event.targetWings.contains(it.id) }.joinToString(", ") { it.name }
                Text(targetWingNames.ifEmpty { "None" })
                
                if (event.mandatory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Mandatory For (Penalty Applies):", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    val mandatoryWingNames = if (event.mandatoryWings.isNotEmpty()) {
                        wings.filter { event.mandatoryWings.contains(it.id) }.joinToString(", ") { it.name }
                    } else {
                        "All Targeted Wings"
                    }
                    Text(mandatoryWingNames)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controls
        Text("Actions", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (event.status != "COMPLETED") {
                if (event.status == "PAUSED" || event.status == "UPCOMING") {
                    Button(onClick = { onStatusChange("ACTIVE") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start / Resume")
                    }
                } else if (event.status == "ACTIVE") {
                     Button(onClick = { onStatusChange("PAUSED") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(30f, 0.8f, 0.9f))) {
                        // Icon pause? Standard icons might not have pause filled, using text mainly.
                        Text("Pause")
                    }
                }
                
                Button(onClick = { onStatusChange("COMPLETED") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("End Event")
                }
            } else {
                 Text("Event Ended", color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(onClick = onExportParams, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Attendance (CSV)")
        }
        
        Button(onClick = { showBulkDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Batch Attendance / Penalty (Manual)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        var showQrDialog by remember { mutableStateOf(false) }
        if (showQrDialog) {
            QRCodeDialog(data = event.id, onDismiss = { showQrDialog = false })
        }
        
        OutlinedButton(onClick = { showQrDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Show QR Code")
        }
        
        if (event.negativeHours > 0 && !event.isPenaltyApplied) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAutoPenalty, 
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Auto-Apply Penalty for Absentees")
            }
        } else if (event.isPenaltyApplied) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Penalty Applied to Absentees", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun BulkAttendanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var rollNumbers by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("PRESENT") }
    var bypass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Attendance") },
        text = {
            Column {
                Text("Enter Roll Numbers (comma separated):")
                OutlinedTextField(
                    value = rollNumbers,
                    onValueChange = { rollNumbers = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Status:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = status == "PRESENT", onClick = { status = "PRESENT" })
                    Text("Present")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = status == "PENALTY", onClick = { status = "PENALTY"; bypass = true }) // Auto-check bypass for penalty
                    Text("Penalty")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bypass, onCheckedChange = { bypass = it })
                    Text("Bypass Wing/Exclusion Checks")
                    // Tooltip: "Useful for explicitly adding students to negative hours even if not mandatory"
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rollNumbers, status, bypass) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QRCodeDialog(data: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Scan to Mark Attendance", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                
                val bitmap = remember(data) {
                    try {
                        val writer = MultiFormatWriter()
                        val matrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
                        val width = matrix.width
                        val height = matrix.height
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                        for (x in 0 until width) {
                            for (y in 0 until height) {
                                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            }
                        }
                        bmp
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(250.dp)
                    )
                } else {
                    Text("Failed to generate QR Code", color = MaterialTheme.colorScheme.error)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when(status) {
        "ACTIVE" -> Color.Green
        "PAUSED" -> Color.Yellow
        "COMPLETED" -> Color.Gray
        else -> Color.Blue // UPCOMING
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black
        )
    }
}
