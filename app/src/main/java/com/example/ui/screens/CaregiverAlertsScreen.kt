package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AlertCard
import com.example.ui.theme.*

@Composable
fun CaregiverAlertsScreen(
    patients: List<PatientEntity>,
    selectedPatientId: String,
    onSelectPatient: (String) -> Unit,
    alerts: List<AlertItemEntity>,
    careNotes: List<CareNoteEntity>,
    onResolveAlert: (String) -> Unit,
    onAddCareNote: (patientId: String, content: String, noteType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Alerts, 1 = Care Notes

    val currentPatient = patients.find { it.id == selectedPatientId } ?: patients.firstOrNull()
    val unreadAlertsCount = alerts.count { !it.isResolved }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddNoteDialog = true },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                    text = { Text("Add Care Note") },
                    containerColor = MedTealSecondary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_care_note_fab")
                )
            }
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Patient Selector Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Active Patient Roster:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${patients.size} Assigned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollable row of patient chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            patients.forEach { p ->
                                val isSelected = p.id == selectedPatientId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectPatient(p.id) },
                                    label = { Text(p.fullName, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (p.adherenceRate >= 90) StatusSuccess else StatusWarning)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MedTealSecondary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Tab bar: Alerts Center vs Care Notes
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Alerts Center")
                                if (unreadAlertsCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = StatusDanger) {
                                        Text("$unreadAlertsCount", color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Clinical Notes (${careNotes.size})") }
                    )
                }
            }

            if (selectedTab == 0) {
                // Alerts list
                if (alerts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("All clear! No active alerts", fontWeight = FontWeight.Bold)
                                Text("All scheduled doses are on track or acknowledged.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(alert = alert, onResolve = onResolveAlert)
                    }
                }
            } else {
                // Care Notes list
                item {
                    Text(
                        text = "Notes for ${currentPatient?.fullName ?: "Patient"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (careNotes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.NoteAlt, contentDescription = null, tint = MedTealSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No care notes recorded yet", fontWeight = FontWeight.Bold)
                                Text("Use the button below to document vitals or observations.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(careNotes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MedTealSecondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = note.noteType,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MedTealOnSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = note.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Logged by: ${note.authorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AddCareNoteDialog(
            patientName = currentPatient?.fullName ?: "Patient",
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { content, type ->
                onAddCareNote(selectedPatientId, content, type)
                showAddNoteDialog = false
            }
        )
    }
}

@Composable
fun AddCareNoteDialog(
    patientName: String,
    onDismiss: () -> Unit,
    onConfirm: (content: String, type: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Vitals") }
    val types = listOf("Vitals", "General", "Incident", "Food/Nutrition")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Care Note for $patientName") },
        text = {
            Column {
                Text("Select Category:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Clinical Note / Observation") },
                    placeholder = { Text("e.g. Blood pressure 125/80 mmHg, pulse 74 bpm. Patient in good spirits.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("input_care_note_content")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) onConfirm(content, selectedType)
                },
                modifier = Modifier.testTag("confirm_add_note_button")
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
