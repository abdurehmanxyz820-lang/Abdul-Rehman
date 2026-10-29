package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItemEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.DoseEventEntity
import com.example.data.model.DoseStatus
import com.example.data.model.UserRole
import com.example.ui.theme.*

@Composable
fun RoleSwitcherBar(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Role View:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Switch perspective to test role capabilities",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UserRole.values().forEach { role ->
                    val isSelected = currentRole == role
                    val (roleName, roleColor) = when (role) {
                        UserRole.PATIENT -> "Patient" to MedBluePrimary
                        UserRole.CAREGIVER -> "Caregiver" to MedTealSecondary
                        UserRole.AGENCY_ADMIN -> "Agency" to MedAmberTertiary
                        UserRole.SYSTEM_ADMIN -> "Admin" to Color(0xFF475569)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onRoleSelected(role) },
                        label = {
                            Text(
                                roleName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = roleColor.copy(alpha = 0.2f),
                            selectedLabelColor = roleColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_chip_${role.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun SyncStatusHeader(
    isOnline: Boolean,
    pendingSyncCount: Int,
    onToggleOnline: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) StatusSuccess else StatusWarning)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isOnline) "Cloud Sync: Online (Supabase)" else "Offline Cache Mode (Room DB)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) Color(0xFF166534) else Color(0xFF854D0E)
                    )
                    if (pendingSyncCount > 0) {
                        Text(
                            text = "$pendingSyncCount pending mutations in local queue",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF854D0E)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (pendingSyncCount > 0 && isOnline) {
                    FilledTonalButton(
                        onClick = onSyncNow,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Now", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = onToggleOnline,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Toggle Network Mode",
                        tint = if (isOnline) Color(0xFF166534) else Color(0xFF854D0E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DoseActionCard(
    dose: DoseEventEntity,
    onTake: (notes: String?) -> Unit,
    onSkip: (reason: String) -> Unit,
    onSnooze: (minutes: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var skipReason by remember { mutableStateOf("Nausea / stomach upset") }

    val statusBg = when (dose.status) {
        DoseStatus.PENDING -> MaterialTheme.colorScheme.surface
        DoseStatus.TAKEN -> Color(0xFFECFDF5)
        DoseStatus.SKIPPED -> Color(0xFFFEF2F2)
        DoseStatus.MISSED -> Color(0xFFFFF1F2)
        DoseStatus.SNOOZED -> Color(0xFFF5F3FF)
    }

    val statusBorder = when (dose.status) {
        DoseStatus.PENDING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        DoseStatus.TAKEN -> StatusSuccess.copy(alpha = 0.5f)
        DoseStatus.SKIPPED -> StatusDanger.copy(alpha = 0.4f)
        DoseStatus.MISSED -> StatusDanger
        DoseStatus.SNOOZED -> StatusSnooze.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dose_card_${dose.id}"),
        colors = CardDefaults.cardColors(containerColor = statusBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Time and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dose.scheduledTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = dose.timeSlot,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                StatusBadge(status = dose.status, snoozeUntil = dose.snoozeUntil)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Medication info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MedBluePrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        tint = MedBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dose.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dosage: ${dose.dosage}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Food & water badges
            if (dose.requiresFood) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MedAmberTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Take with meal / food",
                        style = MaterialTheme.typography.labelSmall,
                        color = MedAmberTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action record details if taken/skipped
            if (dose.status == DoseStatus.TAKEN && dose.recordedAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Taken at ${dose.recordedAt} by ${dose.recordedBy ?: "Patient"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Medium
                )
            } else if (dose.status == DoseStatus.SKIPPED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✕ Skipped: ${dose.notes ?: "Reason not provided"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusDanger,
                    fontWeight = FontWeight.Medium
                )
            } else if (dose.status == DoseStatus.SNOOZED && dose.snoozeUntil != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⏰ Reminder snoozed until ${dose.snoozeUntil}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSnooze,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (dose.status == DoseStatus.PENDING || dose.status == DoseStatus.SNOOZED || dose.status == DoseStatus.MISSED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onTake(null) },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("take_button_${dose.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Now", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showSnoozeDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("snooze_button_${dose.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSnooze)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze")
                    }

                    OutlinedButton(
                        onClick = { showSkipDialog = true },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("skip_button_${dose.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger)
                    ) {
                        Text("Skip")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Status", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Snooze Dialog
    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Snooze Dose Reminder") },
            text = {
                Column {
                    Text("Select how long to snooze the reminder for ${dose.medicationName}:")
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(15 to "15 Minutes", 30 to "30 Minutes", 60 to "1 Hour").forEach { (mins, label) ->
                        ElevatedButton(
                            onClick = {
                                onSnooze(mins)
                                showSnoozeDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Skip Dialog
    if (showSkipDialog) {
        val reasons = listOf(
            "Nausea / stomach upset",
            "Doctor / nurse instructed to hold",
            "Patient refused dose",
            "Fasting for lab work / procedure",
            "Already took earlier"
        )
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Reason for Skipping Dose") },
            text = {
                Column {
                    Text("Select clinical or patient reason for audit trail:")
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { skipReason = r }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = skipReason == r,
                                onClick = { skipReason = r }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(r, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSkip(skipReason)
                        showSkipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("Confirm Skip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusBadge(status: DoseStatus, snoozeUntil: String? = null) {
    val (label, bg, fg) = when (status) {
        DoseStatus.PENDING -> Triple("PENDING", Color(0xFFF1F5F9), Color(0xFF475569))
        DoseStatus.TAKEN -> Triple("TAKEN", Color(0xFFDCFCE7), StatusSuccess)
        DoseStatus.SKIPPED -> Triple("SKIPPED", Color(0xFFFEE2E2), StatusDanger)
        DoseStatus.MISSED -> Triple("MISSED", Color(0xFFFFE4E6), StatusDanger)
        DoseStatus.SNOOZED -> Triple("SNOOZED ${snoozeUntil ?: ""}", Color(0xFFEDE9FE), StatusSnooze)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun AlertCard(
    alert: AlertItemEntity,
    onResolve: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, iconColor, icon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Triple(Color(0xFFFEF2F2), StatusDanger, Icons.Default.Warning)
        AlertSeverity.WARNING -> Triple(Color(0xFFFFFBEB), MedAmberTertiary, Icons.Default.ErrorOutline)
        AlertSeverity.INFO -> Triple(Color(0xFFEFF6FF), MedBluePrimary, Icons.Default.Info)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = alert.severity.name,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${alert.patientName} • ${alert.type}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = alert.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!alert.isResolved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onResolve(alert.id) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Acknowledge & Resolve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Resolved by Caregiver",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencyCallBanner(
    contactName: String,
    phoneNumber: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFEF2F2),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = StatusDanger,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Primary Caregiver: $contactName",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${phoneNumber.replace("[^0-9+]".toRegex(), "")}")
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = StatusDanger,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Call Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
