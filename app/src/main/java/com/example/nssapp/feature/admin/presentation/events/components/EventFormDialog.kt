package com.example.nssapp.feature.admin.presentation.events.components

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nssapp.core.domain.model.Event
import com.example.nssapp.core.domain.model.Wing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDialog(
    initialEvent: Event? = null,
    wings: List<Wing>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, Long, Double, Double, Boolean, List<String>, List<String>, List<String>) -> Unit
) {
    val visibleWings = remember(wings, initialEvent) {
        wings.filter { wing ->
            !wing.isDeleted || 
            initialEvent?.targetWings?.contains(wing.id) == true || 
            initialEvent?.mandatoryWings?.contains(wing.id) == true
        }
    }

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var dateParams by remember { mutableStateOf(initialEvent?.date ?: System.currentTimeMillis()) }
    var positiveHours by remember { mutableStateOf(initialEvent?.positiveHours?.toString() ?: "0.0") }
    var negativeHours by remember { mutableStateOf(initialEvent?.negativeHours?.toString() ?: "0.0") }
    var mandatory by remember { mutableStateOf(initialEvent?.mandatory ?: false) }
    
    val selectedTargetWings = remember { mutableStateListOf<String>().apply { 
        addAll(initialEvent?.targetWings ?: emptyList()) 
    } }
    val selectedMandatoryWings = remember { mutableStateListOf<String>().apply { 
        addAll(initialEvent?.mandatoryWings ?: emptyList()) 
    } }
    
    val studentsExcluded = remember { mutableStateListOf<String>().apply {
        addAll(initialEvent?.studentsExcluded ?: emptyList())
    } }
    var exclusionInput by remember { mutableStateOf("") }
    
    // Time State
    val calendar = Calendar.getInstance()
    
    // Initialize time pickers with event times if available, otherwise current/next hour
    var startHour by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.startTime > 0) {
            calendar.timeInMillis = initialEvent.startTime
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    ) }
    var startMinute by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.startTime > 0) {
            calendar.timeInMillis = initialEvent.startTime
            calendar.get(Calendar.MINUTE)
        } else {
            Calendar.getInstance().get(Calendar.MINUTE)
        }
    ) }
    
    var endHour by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.endTime > 0) {
            calendar.timeInMillis = initialEvent.endTime
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
        }
    ) }
    var endMinute by remember { mutableIntStateOf(
        if (initialEvent != null && initialEvent.endTime > 0) {
            calendar.timeInMillis = initialEvent.endTime
            calendar.get(Calendar.MINUTE)
        } else {
            Calendar.getInstance().get(Calendar.MINUTE)
        }
    ) }

    // Date Picker State
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateParams)
    var showDatePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val startTimePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> startHour = hour; startMinute = minute },
        startHour, startMinute, false
    )

    val endTimePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> endHour = hour; endMinute = minute },
        endHour, endMinute, false
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateParams = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(1f),
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent != null) "Edit Event" else "Create Event") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Basic Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Event Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateParams)),
                        onValueChange = {},
                        label = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         OutlinedButton(onClick = { startTimePickerDialog.show() }, modifier = Modifier.weight(1f)) {
                             Text("Start: ${String.format("%02d:%02d", startHour, startMinute)}")
                         }
                         OutlinedButton(onClick = { endTimePickerDialog.show() }, modifier = Modifier.weight(1f)) {
                             Text("End: ${String.format("%02d:%02d", endHour, endMinute)}")
                         }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }

                item {
                    Text("Select wings that can attend this event:", style = MaterialTheme.typography.bodySmall)
                    
                    val allTargetSelected = visibleWings.isNotEmpty() && selectedTargetWings.size == visibleWings.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allTargetSelected,
                            onCheckedChange = { checked ->
                                selectedTargetWings.clear()
                                if (checked) {
                                    selectedTargetWings.addAll(visibleWings.map { it.id })
                                } else {
                                    selectedMandatoryWings.clear()
                                }
                            }
                        )
                        Text("All Wings", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                    }

                    visibleWings.forEach { wing ->
                        val isInitiallySelected = initialEvent?.targetWings?.contains(wing.id) == true
                        val isDeleted = wing.isDeleted

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedTargetWings.contains(wing.id),
                                onCheckedChange = { checked ->
                                    // Prevent deselecting a deleted wing that was already there
                                    if (!checked && isDeleted && isInitiallySelected) return@Checkbox

                                    if (checked) selectedTargetWings.add(wing.id) else {
                                        selectedTargetWings.remove(wing.id)
                                        selectedMandatoryWings.remove(wing.id)
                                    }
                                },
                                enabled = !(isDeleted && isInitiallySelected) // Visual hint: Cannot deselect history
                            )
                            Text(
                                text = if (wing.isDeleted) "${wing.name} (Deleted)" else wing.name,
                                color = if (wing.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }

                item {
                    Text("Attendance Policy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = mandatory, 
                            onCheckedChange = { 
                                mandatory = it
                                if (!it) negativeHours = "0.0"
                            }
                        )
                        Column {
                            Text("Mandatory Event", style = MaterialTheme.typography.bodyLarge)
                            Text("Students from mandatory wings will get penalty if absent.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         OutlinedTextField(
                            value = positiveHours, 
                            onValueChange = { positiveHours = it }, 
                            label = { Text("+ Hours (Present)") },
                            modifier = Modifier.weight(1f)
                        )
                        if (mandatory) {
                             OutlinedTextField(
                                value = negativeHours, 
                                onValueChange = { negativeHours = it }, 
                                label = { Text("- Hours (Penalty)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (mandatory) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Mandatory For (Specific Wings):", style = MaterialTheme.typography.titleSmall)
                        Text("Only select if the penalty should be limited to specific wings. If empty, all targeted wings are mandatory.", style = MaterialTheme.typography.bodySmall)
                        
                        val availableOptions = visibleWings.filter { selectedTargetWings.contains(it.id) }
                        if (availableOptions.isEmpty()) {
                            Text("Please select Target Wings first.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        } else {
                            val allMandatorySelected = selectedMandatoryWings.size == availableOptions.size
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allMandatorySelected,
                                    onCheckedChange = { checked ->
                                        selectedMandatoryWings.clear()
                                        if (checked) {
                                            selectedMandatoryWings.addAll(availableOptions.map { it.id })
                                        }
                                    }
                                )
                                Text("All Targeted Wings", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                            }

                            availableOptions.forEach { wing ->
                                 val isInitiallyMandatory = initialEvent?.mandatoryWings?.contains(wing.id) == true
                                 val isDeleted = wing.isDeleted

                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedMandatoryWings.contains(wing.id),
                                        onCheckedChange = { checked ->
                                            if (!checked && isDeleted && isInitiallyMandatory) return@Checkbox

                                            if (checked) {
                                                selectedMandatoryWings.add(wing.id)
                                            } else {
                                                selectedMandatoryWings.remove(wing.id)
                                            }
                                        },
                                        enabled = !(isDeleted && isInitiallyMandatory)
                                    )
                                    Text(
                                        text = if (wing.isDeleted) "${wing.name} (Deleted)" else wing.name,
                                        color = if (wing.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Excluded Students (Penalty won't apply):", style = MaterialTheme.typography.titleSmall)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = exclusionInput,
                                onValueChange = { exclusionInput = it },
                                placeholder = { Text("Roll/ID") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    if (exclusionInput.isNotBlank() && !studentsExcluded.contains(exclusionInput.trim())) {
                                        studentsExcluded.add(exclusionInput.trim())
                                        exclusionInput = ""
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }

                        if (studentsExcluded.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                studentsExcluded.forEach { studentId ->
                                    InputChip(
                                        selected = false,
                                        onClick = { },
                                        label = { Text(studentId) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { studentsExcluded.remove(studentId) }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (initialEvent?.isPenaltyApplied == true) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Note: A penalty has been applied. Updating target/mandatory settings will reset it.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                         // Calculate timestamps
                         val startCal = Calendar.getInstance().apply { timeInMillis = dateParams; set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, startMinute) }
                         val endCal = Calendar.getInstance().apply { timeInMillis = dateParams; set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMinute) }
                         
                         onConfirm(
                             title, 
                             description, 
                             dateParams,
                             startCal.timeInMillis,
                             endCal.timeInMillis,
                             positiveHours.toDoubleOrNull() ?: 0.0,
                             negativeHours.toDoubleOrNull() ?: 0.0,
                             mandatory, 
                             selectedTargetWings.toList(),
                             if (mandatory) selectedMandatoryWings.toList() else emptyList(),
                             if (mandatory) studentsExcluded.toList() else emptyList()
                         )
                    }
                }
            ) {
                Text(if (initialEvent != null) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
