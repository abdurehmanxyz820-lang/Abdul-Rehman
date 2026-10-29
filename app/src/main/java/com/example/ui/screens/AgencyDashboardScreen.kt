package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdherenceStats
import com.example.data.model.PatientEntity
import com.example.ui.theme.*

@Composable
fun AgencyDashboardScreen(
    patients: List<PatientEntity>,
    selectedPatient: PatientEntity?,
    adherenceStats: AdherenceStats,
    onSelectPatient: (String) -> Unit,
    onAddPatient: (
        name: String,
        room: String,
        dob: String,
        emName: String,
        emPhone: String,
        allergies: String,
        conditions: String,
        notes: String
    ) -> Unit,
    onExportCsv: (Context) -> Unit,
    onExportPdf: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAdmitDialog by remember { mutableStateOf(false) }

    val filteredPatients = remember(patients, searchQuery) {
        if (searchQuery.isBlank()) patients
        else patients.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.roomNumber.contains(searchQuery, ignoreCase = true) ||
            it.medicalConditions.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Agency Overview Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ST. JUDE HOME-CARE AGENCY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MedBluePrimary
                            )
                            Text(
                                text = "Agency Clinical Operations",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = MedTealSecondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "License HHA-994820",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MedTealOnSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Metric cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(title = "Patients", value = "${patients.size + 44}", subtitle = "Active Roster", modifier = Modifier.weight(1f))
                        MetricCard(title = "Caregivers", value = "12", subtitle = "On Duty", modifier = Modifier.weight(1f))
                        MetricCard(title = "Adherence", value = "94.2%", subtitle = "30-Day Avg", isSuccess = true, modifier = Modifier.weight(1f))
                        MetricCard(title = "Alerts", value = "2", subtitle = "Open Flag", isWarning = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Compliance Reports & Export Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Clinical Audit & Adherence Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate and share certified records for ${selectedPatient?.fullName ?: "Active Patient"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onExportCsv(context) },
                            modifier = Modifier.weight(1f).testTag("export_csv_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MedBluePrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        FilledTonalButton(
                            onClick = { onExportPdf(context) },
                            modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MedTealSecondaryContainer, contentColor = MedTealOnSecondaryContainer)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clinical PDF")
                        }
                    }
                }
            }
        }

        // Weekly Adherence Visual Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Adherence Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: >90%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Bar visualization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        adherenceStats.weeklyTrends.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.height(110.dp)
                            ) {
                                Text(
                                    text = "${day.adherencePercent}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.adherencePercent >= 90) StatusSuccess else StatusWarning
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val barHeight = (day.adherencePercent * 0.7).coerceIn(10.0, 70.0).dp
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (day.adherencePercent >= 90) MedTealSecondary else MedAmberTertiary)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.dayLabel,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Patient Directory Header & Search Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Patient Directory (${filteredPatients.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAdmitDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("admit_patient_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Admit Patient", fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, room, or condition...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null) } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Patient Cards List
        items(filteredPatients, key = { it.id }) { p ->
            val isSelected = p.id == selectedPatient?.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPatient(p.id) }
                    .testTag("patient_row_${p.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MedBluePrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) MedBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MedBluePrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.fullName.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""),
                            fontWeight = FontWeight.Bold,
                            color = MedBluePrimary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = p.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (p.adherenceRate >= 90) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${p.adherenceRate}% Adherence",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (p.adherenceRate >= 90) StatusSuccess else MedAmberTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${p.roomNumber} • DOB: ${p.dob}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Conditions: ${p.medicalConditions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showAdmitDialog) {
        AdmitPatientDialog(
            onDismiss = { showAdmitDialog = false },
            onConfirm = { name, room, dob, emName, emPhone, allergies, conditions, notes ->
                onAddPatient(name, room, dob, emName, emPhone, allergies, conditions, notes)
                showAdmitDialog = false
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false,
    isWarning: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSuccess -> StatusSuccess
                    isWarning -> StatusDanger
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdmitPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        room: String,
        dob: String,
        emName: String,
        emPhone: String,
        allergies: String,
        conditions: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("Suite 101") }
    var dob by remember { mutableStateOf("1955-04-12") }
    var emName by remember { mutableStateOf("") }
    var emPhone by remember { mutableStateOf("+1 (555) 000-0000") }
    var allergies by remember { mutableStateOf("None reported") }
    var conditions by remember { mutableStateOf("Hypertension") }
    var notes by remember { mutableStateOf("Newly admitted patient.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admit New Patient") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Legal Name *") },
                        placeholder = { Text("e.g. Thomas Wayne") },
                        modifier = Modifier.fillMaxWidth().testTag("input_patient_name")
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Room / Suite") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("DOB") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = emName,
                        onValueChange = { emName = it },
                        label = { Text("Emergency Contact Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = emPhone,
                        onValueChange = { emPhone = it },
                        label = { Text("Emergency Contact Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Known Allergies") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = conditions,
                        onValueChange = { conditions = it },
                        label = { Text("Primary Diagnoses") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, room, dob, emName, emPhone, allergies, conditions, notes)
                    }
                },
                modifier = Modifier.testTag("confirm_admit_patient_button")
            ) {
                Text("Admit Patient")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
