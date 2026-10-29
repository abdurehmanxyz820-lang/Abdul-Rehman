package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.MedicationEntity
import com.example.data.model.PatientEntity
import com.example.data.model.TimeSlot
import com.example.ui.theme.*

@Composable
fun MedicationManagementScreen(
    patient: PatientEntity?,
    medications: List<MedicationEntity>,
    onAddMedication: (
        name: String,
        genericName: String,
        dosage: String,
        unit: String,
        route: String,
        instructions: String,
        prescriber: String,
        rxNumber: String,
        requiresFood: Boolean,
        frequencyDescription: String,
        timeSlots: List<TimeSlot>
    ) -> Unit,
    onDeleteMedication: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Medication") },
                containerColor = MedBluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_medication_fab")
            )
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Active Prescriptions & Regimens",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prescribed for ${patient?.fullName ?: "Patient"} • ${medications.size} Active Formulations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (medications.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(48.dp), tint = MedBluePrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No medications registered yet", fontWeight = FontWeight.Bold)
                            Text("Tap 'Add Medication' below to prescribe or register a regimen.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(medications, key = { it.id }) { med ->
                    MedicationCard(
                        medication = med,
                        onDelete = { onDeleteMedication(med.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            patientName = patient?.fullName ?: "Patient",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, generic, dosage, unit, route, instr, prescriber, rx, reqFood, freq, slots ->
                onAddMedication(name, generic, dosage, unit, route, instr, prescriber, rx, reqFood, freq, slots)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MedicationCard(
    medication: MedicationEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MedTealSecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Medication,
                            contentDescription = null,
                            tint = MedTealSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${medication.genericName} • ${medication.dosage} ${medication.unit} (${medication.route})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove Medication",
                        tint = StatusDanger.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Instructions & Schedule
            Text(
                text = "Instructions: ${medication.instructions}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Schedule: ${medication.frequencyDescription}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Prescriber: ${medication.prescriber} | Rx: ${medication.rxNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (medication.requiresFood) {
                    Surface(
                        color = MedAmberTertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(12.dp), tint = MedAmberTertiary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("With food", fontSize = 10.sp, color = MedAmberOnTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Archive Medication") },
            text = { Text("Are you sure you want to discontinue or archive ${medication.name}? This will remove pending scheduled doses from the daily tracker.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddMedicationDialog(
    patientName: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        genericName: String,
        dosage: String,
        unit: String,
        route: String,
        instructions: String,
        prescriber: String,
        rxNumber: String,
        requiresFood: Boolean,
        frequencyDescription: String,
        timeSlots: List<TimeSlot>
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var genericName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mg") }
    var route by remember { mutableStateOf("Oral") }
    var instructions by remember { mutableStateOf("") }
    var prescriber by remember { mutableStateOf("Dr. Sarah Jenkins, MD") }
    var rxNumber by remember { mutableStateOf("RX-${(1000000..9999999).random()}") }
    var requiresFood by remember { mutableStateOf(false) }

    val morningSelected = remember { mutableStateOf(true) }
    val afternoonSelected = remember { mutableStateOf(false) }
    val eveningSelected = remember { mutableStateOf(false) }
    val nightSelected = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Prescription for $patientName", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medication Brand Name *") },
                        placeholder = { Text("e.g. Lisinopril") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_name")
                    )
                }
                item {
                    OutlinedTextField(
                        value = genericName,
                        onValueChange = { genericName = it },
                        label = { Text("Generic Chemical Name") },
                        placeholder = { Text("e.g. Lisinopril dihydrate") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dosage *") },
                            placeholder = { Text("e.g. 10") },
                            modifier = Modifier.weight(1f).testTag("input_med_dosage")
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("mg, ml, mcg") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = route,
                        onValueChange = { route = it },
                        label = { Text("Administration Route") },
                        placeholder = { Text("Oral, Injection, Inhaler, Drops") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions for Patient *") },
                        placeholder = { Text("e.g. Take with water before breakfast") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_instructions")
                    )
                }
                item {
                    OutlinedTextField(
                        value = prescriber,
                        onValueChange = { prescriber = it },
                        label = { Text("Prescribing Physician") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requiresFood,
                            onCheckedChange = { requiresFood = it }
                        )
                        Text("Requires food / meal timing", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    Text("Daily Dose Time Slots:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = morningSelected.value, onCheckedChange = { morningSelected.value = it })
                            Text("Morning", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = afternoonSelected.value, onCheckedChange = { afternoonSelected.value = it })
                            Text("Afternoon", fontSize = 12.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = eveningSelected.value, onCheckedChange = { eveningSelected.value = it })
                            Text("Evening", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = nightSelected.value, onCheckedChange = { nightSelected.value = it })
                            Text("Night", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank()) {
                        val slots = mutableListOf<TimeSlot>()
                        if (morningSelected.value) slots.add(TimeSlot.MORNING)
                        if (afternoonSelected.value) slots.add(TimeSlot.AFTERNOON)
                        if (eveningSelected.value) slots.add(TimeSlot.EVENING)
                        if (nightSelected.value) slots.add(TimeSlot.NIGHT)
                        if (slots.isEmpty()) slots.add(TimeSlot.MORNING)

                        val freqDesc = if (slots.size == 1) "Daily at ${slots[0].typicalTime}" else "${slots.size} times daily"

                        onConfirm(
                            name,
                            if (genericName.isBlank()) name else genericName,
                            dosage,
                            unit,
                            route,
                            if (instructions.isBlank()) "Take as prescribed" else instructions,
                            prescriber,
                            rxNumber,
                            requiresFood,
                            freqDesc,
                            slots
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_med_button")
            ) {
                Text("Save Prescription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
