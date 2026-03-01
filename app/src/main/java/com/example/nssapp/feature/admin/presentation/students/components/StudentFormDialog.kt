package com.example.nssapp.feature.admin.presentation.students.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nssapp.core.domain.model.Student
import com.example.nssapp.core.domain.model.Wing

@Composable
fun StudentFormDialog(
    initialStudent: Student? = null,
    wings: List<Wing>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<String>, String) -> Unit
) {
    var name by remember { mutableStateOf(initialStudent?.name ?: "") }
    var email by remember { mutableStateOf(initialStudent?.email ?: "") }
    var roll by remember { mutableStateOf(initialStudent?.roll ?: "") }
    var password by remember { mutableStateOf(initialStudent?.password ?: "") }
    var showError by remember { mutableStateOf(false) }
    
    val selectedWings = remember { mutableStateListOf<String>().apply {
        addAll(initialStudent?.enrolledWings ?: emptyList())
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialStudent != null) "Edit Student" else "Add Student") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = roll, onValueChange = { roll = it }, label = { Text("Roll No") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Wings:", style = MaterialTheme.typography.titleSmall)
                    wings.forEach { wing ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                             if (selectedWings.contains(wing.id)) selectedWings.remove(wing.id) else selectedWings.add(wing.id)
                        }) {
                            Checkbox(
                                checked = selectedWings.contains(wing.id),
                                onCheckedChange = { checked ->
                                    if (checked) selectedWings.add(wing.id) else selectedWings.remove(wing.id)
                                }
                            )
                            Text(wing.name)
                        }
                    }
                    
                    if (showError && selectedWings.isEmpty()) {
                        Text(
                            text = "Please select at least one wing", 
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && email.isNotBlank() && roll.isNotBlank() && selectedWings.isNotEmpty() && password.isNotBlank()) {
                         showError = false
                         onConfirm(name, email, roll, selectedWings.toList(), password)
                    } else {
                         showError = true
                    }
                }
            ) {
                Text(if (initialStudent != null) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
