package com.example.nssapp.feature.admin.presentation.events

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nssapp.core.domain.model.AttendanceStatus
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.EventStatus
import com.example.nssapp.core.domain.model.Wing
import com.example.nssapp.feature.admin.presentation.events.components.EventFormDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

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

    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is EventDetailUiState.Success) {
                        val event = (uiState as EventDetailUiState.Success).event
                        IconButton(onClick = { viewModel.exportAttendance(eventId) }) {
                            Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (event.status != EventStatus.ACTIVE.value) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(scrollState)
        ) {
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
                                    Icon(Icons.Default.Close, contentDescription = "Close")
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
                is EventDetailUiState.Loading -> CircularProgressIndicator()
                is EventDetailUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
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
                        
                        if (successState.event.status != EventStatus.ACTIVE.value) {
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
                onMarkBulk(rolls, AttendanceStatus.PRESENT.value, bypass)
                showBulkDialog = false
            }
        )
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    StatusChip(status = event.status)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Description:", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = event.description, 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Date:", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(event.date)), 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (event.startTime > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Time:", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Hours: +${event.positiveHours} / -${event.negativeHours}", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Target Wings:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val targetWingNames = wings.filter { event.targetWings.contains(it.id) }.joinToString(", ") { it.name }
                Text(
                    text = targetWingNames.ifEmpty { "None" }, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (event.mandatory) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Mandatory For (Penalty Applies):", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.error
                    )
                    val mandatoryWingNames = if (event.mandatoryWings.isNotEmpty()) {
                        wings.filter { event.mandatoryWings.contains(it.id) }.joinToString(", ") { it.name }
                    } else {
                        "All Targeted Wings"
                    }
                    Text(
                        text = mandatoryWingNames, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controls
        Text("Actions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (event.status == EventStatus.IDLE.value) {
                 Button(onClick = { onStatusChange(EventStatus.ACTIVE.value) }, modifier = Modifier.weight(1f)) {
                     Icon(Icons.Default.PlayArrow, contentDescription = null)
                     Spacer(modifier = Modifier.width(4.dp))
                     Text("Start Event")
                 }
            } else if (event.status == EventStatus.ACTIVE.value) {
                 Button(
                     onClick = { onStatusChange(EventStatus.IDLE.value) }, 
                     modifier = Modifier.weight(1f), 
                     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                 ) {
                    Text("End Event")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        var showQrDialog by remember { mutableStateOf(false) }

        if (event.status == EventStatus.ACTIVE.value) {
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
        
        val canApplyPenalty = event.mandatory && event.negativeHours > 0 && (event.status == EventStatus.ACTIVE.value || event.status == EventStatus.IDLE.value) && (!event.isPenaltyApplied)
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
            Button(onClick = { onConfirm(rollNumbers, AttendanceStatus.PRESENT.value, bypass) }) {
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attendee.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Roll: ${attendee.roll} • ${attendee.wings.joinToString(", ").ifEmpty { "No matching wings" }}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val isPenalty = attendee.status == AttendanceStatus.PENALTY.value
            Surface(
                color = if (isPenalty) 
                           MaterialTheme.colorScheme.errorContainer 
                        else 
                           MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = attendee.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (isPenalty) 
                               MaterialTheme.colorScheme.onErrorContainer 
                            else 
                               MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                        val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
                        for (x in 0 until width) {
                            for (y in 0 until height) {
                                bmp[x, y] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
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
        EventStatus.ACTIVE.value -> Color.Green
        else -> Color.Blue // idle
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
