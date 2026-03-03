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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
    val attendees by viewModel.attendees.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAttendees by remember { mutableStateOf(false) }

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
                        val event = (uiState as EventDetailUiState.Success).event
                        IconButton(onClick = { viewModel.exportAttendance(eventId) }) {
                            Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (event.status != "ACTIVE") {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showDeleteConfirm) {
                 AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Event") },
                    text = { Text("Are you sure you want to delete this event? This will also permanently erase all attendance records associated with it.") },
                    confirmButton = {
                        Button(
                            onClick = { 
                                showDeleteConfirm = false
                                viewModel.deleteEvent(eventId) { navController.popBackStack() } 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showAttendees) {
                Dialog(onDismissRequest = { showAttendees = false }) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.8f)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Event Attendees", 
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        "${attendees.size} students found", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = { showAttendees = false }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (attendees.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No records found", style = MaterialTheme.typography.bodyLarge)
                                }
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(attendees.size) { index ->
                                        AttendeeItem(attendees[index])
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAttendees = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
            
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
                            onConfirm = { title, description, date, startTime, endTime, posHours, negHours, mandatory, targetWings, mandatoryWings, studentsExcluded ->
                                val currentEvent = successState.event
                                // Wise update: reset penalty if wings or mandatory status changes
                                val shouldResetPenalty = currentEvent.mandatory != mandatory ||
                                        currentEvent.targetWings.toSet() != targetWings.toSet() ||
                                        currentEvent.mandatoryWings.toSet() != mandatoryWings.toSet() ||
                                        currentEvent.studentsExcluded.toSet() != studentsExcluded.toSet() ||
                                        currentEvent.negativeHours != negHours ||
                                        currentEvent.positiveHours != posHours
                                
                                viewModel.updateEvent(currentEvent.copy(
                                    title = title,
                                    description = description,
                                    date = date,
                                    startTime = startTime,
                                    endTime = endTime,
                                    positiveHours = posHours,
                                    negativeHours = negHours,
                                    mandatory = mandatory,
                                    targetWings = targetWings,
                                    mandatoryWings = mandatoryWings,
                                    studentsExcluded = studentsExcluded,
                                    isPenaltyApplied = if (shouldResetPenalty) false else currentEvent.isPenaltyApplied
                                ), resetPenalty = shouldResetPenalty)
                                showEditDialog = false
                            }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        EventDetailContent(
                            event = successState.event,
                            wings = successState.wings,
                            onStatusChange = { newStatus -> viewModel.updateStatus(eventId, newStatus) },
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
                            },
                            onViewAttendees = {
                                viewModel.loadAttendees(eventId)
                                showAttendees = true
                            }
                        )
                        
                        if (successState.event.status != "ACTIVE") {
                            FloatingActionButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 80.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                            }
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
    onMarkBulk: (String, String, Boolean) -> Unit,
    onAutoPenalty: () -> Unit,
    onViewAttendees: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    var showBulkDialog by remember { mutableStateOf(false) }
    
    if (showBulkDialog) {
        BulkAttendanceDialog(
            onDismiss = { showBulkDialog = false },
            onConfirm = { rolls, _, bypass ->
                onMarkBulk(rolls, "PRESENT", bypass)
                showBulkDialog = false
            }
        )
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.titleMedium)
                    StatusChip(status = event.status)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Description: ${event.description}", style = MaterialTheme.typography.bodyLarge)
                Text("Date: ${dateFormat.format(Date(event.date))}", style = MaterialTheme.typography.bodyLarge)
                if (event.startTime > 0) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text("Time: ${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}", style = MaterialTheme.typography.bodyLarge)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Hours: +${event.positiveHours} / -${event.negativeHours}", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Target Wings:", style = MaterialTheme.typography.labelMedium)
                val targetWingNames = wings.filter { event.targetWings.contains(it.id) }.joinToString(", ") { it.name }
                Text(targetWingNames.ifEmpty { "None" }, style = MaterialTheme.typography.bodyMedium)
                
                if (event.mandatory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mandatory For (Penalty Applies):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    val mandatoryWingNames = if (event.mandatoryWings.isNotEmpty()) {
                        wings.filter { event.mandatoryWings.contains(it.id) }.joinToString(", ") { it.name }
                    } else {
                        "All Targeted Wings"
                    }
                    Text(mandatoryWingNames, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controls
        Text("Actions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (event.status != "COMPLETED") {
                if (event.status == "PAUSED" || event.status == "UPCOMING") {
                    Button(onClick = { onStatusChange("ACTIVE") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start / Resume")
                    }
                } else if (event.status == "ACTIVE") {
                     Button(
                         onClick = { onStatusChange("PAUSED") }, 
                         modifier = Modifier.weight(1f), 
                         colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                     ) {
                        Text("Pause Event")
                    }
                }
            } else {
                 Text("Event is Completed", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        var showQrDialog by remember { mutableStateOf(false) }

        if (event.status == "ACTIVE") {
            Button(onClick = { showBulkDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Manual Batch Attendance")
            }
    
            Spacer(modifier = Modifier.height(8.dp))
            
            if (showQrDialog) {
                QRCodeDialog(data = event.id, onDismiss = { showQrDialog = false })
            }
            
            OutlinedButton(onClick = { showQrDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Show Event QR Code")
            }
    
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onViewAttendees, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Attendees")
        }
        
        val canApplyPenalty = event.mandatory && event.negativeHours > 0 && (event.status == "ACTIVE" || event.status == "COMPLETED") && (!event.isPenaltyApplied)
        if (canApplyPenalty) {
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
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Penalty Already Applied", 
                    color = MaterialTheme.colorScheme.onErrorContainer, 
                    style = MaterialTheme.typography.labelMedium, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
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
                    placeholder = { Text("e.g. 2101, 2102, 2105") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bypass, onCheckedChange = { bypass = it })
                    Column {
                        Text("Bypass Target Wing Check")
                        Text(
                            text = "Forces attendance marking even if student's wing is not targeted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rollNumbers, "PRESENT", bypass) }) {
                Text("Mark Present")
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
fun AttendeeItem(attendee: Attendee) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attendee.name, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Text(
                    text = "Roll: ${attendee.roll} • ${attendee.wing}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val isPenalty = attendee.status == "PENALTY"
            Surface(
                color = if (isPenalty) 
                           MaterialTheme.colorScheme.errorContainer 
                        else 
                           MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = attendee.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (isPenalty) 
                               MaterialTheme.colorScheme.onErrorContainer 
                            else 
                               MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
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
