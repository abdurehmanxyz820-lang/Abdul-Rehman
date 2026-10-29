package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.DoseActionCard
import com.example.ui.components.EmergencyCallBanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PatientScheduleScreen(
    patient: PatientEntity?,
    doses: List<DoseEventEntity>,
    stats: AdherenceStats,
    onTakeDose: (String, String?) -> Unit,
    onSkipDose: (String, String) -> Unit,
    onSnoozeDose: (String, Int) -> Unit,
    onResetDose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSlot by remember { mutableStateOf<String?>("ALL") }
    val todayDateStr = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    val filteredDoses = remember(doses, selectedSlot) {
        if (selectedSlot == null || selectedSlot == "ALL") {
            doses
        } else {
            doses.filter { it.timeSlot.equals(selectedSlot, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Patient Header & Adherence Streak Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Today's Schedule",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = todayDateStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Streak flame badge
                        Surface(
                            color = MedAmberTertiary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stats.streakDays} Day Streak",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar & metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${patient?.fullName ?: "Patient"} (${patient?.roomNumber ?: "Suite 204"})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (stats.overallAdherence / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = StatusSuccess,
                                trackColor = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${stats.overallAdherence}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${stats.dosesTaken} of ${doses.size} taken",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Emergency Contact Quick Action
        item {
            EmergencyCallBanner(
                contactName = patient?.emergencyContactName ?: "David Vance (Son)",
                phoneNumber = patient?.emergencyContactPhone ?: "+1 (555) 234-5678"
            )
        }

        // Time Slot Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val slots = listOf("ALL" to "All Doses", "MORNING" to "Morning", "AFTERNOON" to "Afternoon", "EVENING" to "Evening", "NIGHT" to "Night")
                slots.forEach { (slotKey, label) ->
                    val isSelected = selectedSlot == slotKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSlot = slotKey },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Dose cards list
        if (filteredDoses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No doses scheduled for this time slot",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredDoses, key = { it.id }) { dose ->
                DoseActionCard(
                    dose = dose,
                    onTake = { notes -> onTakeDose(dose.id, notes) },
                    onSkip = { reason -> onSkipDose(dose.id, reason) },
                    onSnooze = { mins -> onSnoozeDose(dose.id, mins) },
                    onReset = { onResetDose(dose.id) }
                )
            }
        }

        // Clinical Patient Instructions & Notes
        if (patient?.notes?.isNotEmpty() == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MedBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Caregiver Guidance for ${patient.fullName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = patient.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
