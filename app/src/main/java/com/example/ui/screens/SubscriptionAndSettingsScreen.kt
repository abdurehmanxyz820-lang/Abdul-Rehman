package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogEntity
import com.example.data.model.SubscriptionTierInfo
import com.example.data.model.UserSession
import com.example.ui.theme.*

@Composable
fun SubscriptionAndSettingsScreen(
    session: UserSession,
    subscriptionPlans: List<SubscriptionTierInfo>,
    currentTier: String,
    isDevPaymentMode: Boolean,
    pendingSyncCount: Int,
    auditLogs: List<AuditLogEntity>,
    onSelectTier: (String) -> Unit,
    onToggleDevMode: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var biometricEnabled by remember { mutableStateOf(true) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile & Organization Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MedBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = session.orgName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MedBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Subscription Plans Section
        item {
            Text(
                text = "Subscription & Agency Licensing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Development Mode switch banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDevPaymentMode) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDevPaymentMode) "Development Mode Active" else "Production Billing Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDevPaymentMode) MedAmberOnTertiaryContainer else Color(0xFF166534)
                        )
                        Text(
                            text = if (isDevPaymentMode) "Simulates payment webhooks & plan provisioning without charging real cards" else "Connected to Stripe / App Store In-App Purchases",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDevPaymentMode) MedAmberOnTertiaryContainer.copy(alpha = 0.8f) else Color(0xFF166534).copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isDevPaymentMode,
                        onCheckedChange = { onToggleDevMode() },
                        modifier = Modifier.testTag("dev_mode_switch")
                    )
                }
            }
        }

        // Plan cards
        items(subscriptionPlans) { plan ->
            val isCurrent = plan.tier == currentTier
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_card_${plan.tier}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MedBluePrimaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (isCurrent) 2.dp else 1.dp,
                    if (isCurrent) MedBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = plan.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = plan.priceDisplay,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MedBluePrimary
                                )
                                Text(
                                    text = plan.billingCycle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                            }
                        }

                        if (isCurrent) {
                            Surface(
                                color = StatusSuccess,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ACTIVE PLAN",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    onSelectTier(plan.tier)
                                    Toast.makeText(context, "Switched to ${plan.name} (Dev Mode Active)", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Select Plan", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    plan.features.forEach { feat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Security & Privacy Controls
        item {
            Text(
                text = "Security & Data Governance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric / PIN App Lock", fontWeight = FontWeight.SemiBold)
                            Text("Require authentication when switching between apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it })
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Local Room DB Storage", fontWeight = FontWeight.SemiBold)
                            Text("AES-256 encrypted SQLite cache", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                            Text("Encrypted", color = StatusSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Exporting patient encrypted archive...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Data", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Account", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // System Admin Audit Trail Stream
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "System Audit Stream (${auditLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSyncNow) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", fontSize = 12.sp)
                }
            }
        }

        items(auditLogs.take(6)) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MedBluePrimary
                        )
                        Text(
                            text = log.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${log.actorName} • ${log.details}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete MediSync Account?") },
            text = { Text("This will permanently remove your user credentials and purge local biometric cache. In compliance with HIPAA and healthcare privacy regulations, clinical audit logs will be archived.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        Toast.makeText(context, "Account deletion request submitted for audit approval.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}
