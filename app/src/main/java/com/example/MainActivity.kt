package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.ui.components.RoleSwitcherBar
import com.example.ui.components.SyncStatusHeader
import com.example.ui.screens.*
import com.example.ui.theme.MediSyncTheme
import com.example.ui.theme.MedBluePrimary
import com.example.ui.theme.MedTealSecondary
import com.example.ui.viewmodel.MediSyncViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MediSyncViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MediSyncTheme {
                val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
                val selectedPatientId by viewModel.selectedPatientId.collectAsStateWithLifecycle()
                val selectedPatient by viewModel.selectedPatient.collectAsStateWithLifecycle()
                val patients by viewModel.patients.collectAsStateWithLifecycle()
                val medications by viewModel.medications.collectAsStateWithLifecycle()
                val doses by viewModel.doses.collectAsStateWithLifecycle()
                val adherenceStats by viewModel.adherenceStats.collectAsStateWithLifecycle()
                val alerts by viewModel.alerts.collectAsStateWithLifecycle()
                val careNotes by viewModel.careNotes.collectAsStateWithLifecycle()
                val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
                val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
                val isDevPaymentMode by viewModel.isDevPaymentMode.collectAsStateWithLifecycle()
                val currentTier by viewModel.currentSubscriptionTier.collectAsStateWithLifecycle()
                val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
                val subscriptionPlans = remember { viewModel.getSubscriptionPlans() }

                var currentTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.HealthAndSafety,
                                            contentDescription = null,
                                            tint = MedBluePrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "MediSync AI",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = currentSession.orgName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    Surface(
                                        color = when (currentSession.role) {
                                            UserRole.PATIENT -> MedBluePrimary
                                            UserRole.CAREGIVER -> MedTealSecondary
                                            UserRole.AGENCY_ADMIN -> Color(0xFFD97706)
                                            UserRole.SYSTEM_ADMIN -> Color(0xFF475569)
                                        },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = currentSession.role.name,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            )

                            // Role Switcher Simulator Bar
                            RoleSwitcherBar(
                                currentRole = currentSession.role,
                                onRoleSelected = { role ->
                                    viewModel.setRole(role)
                                    // Adapt default screen when role changes
                                    when (role) {
                                        UserRole.PATIENT -> currentTab = 0
                                        UserRole.CAREGIVER -> currentTab = 2
                                        UserRole.AGENCY_ADMIN -> currentTab = 3
                                        UserRole.SYSTEM_ADMIN -> currentTab = 5
                                    }
                                }
                            )

                            // Cloud / Offline Sync Header
                            SyncStatusHeader(
                                isOnline = currentSession.isOnline,
                                pendingSyncCount = pendingSyncCount,
                                onToggleOnline = { viewModel.toggleOnlineStatus() },
                                onSyncNow = { viewModel.syncNow() }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Schedule") },
                                label = { Text("Schedule", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_schedule")
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Medication, contentDescription = "Meds") },
                                label = { Text("Meds", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_meds")
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            val unread = alerts.count { !it.isResolved }
                                            if (unread > 0) {
                                                Badge { Text("$unread") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                                    }
                                },
                                label = { Text("Caregiver", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_caregiver")
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.CorporateFare, contentDescription = "Agency") },
                                label = { Text("Agency", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_agency")
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI") },
                                label = { Text("AI Assistant", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_ai")
                            )
                            NavigationBarItem(
                                selected = currentTab == 5,
                                onClick = { currentTab = 5 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            0 -> PatientScheduleScreen(
                                patient = selectedPatient,
                                doses = doses,
                                stats = adherenceStats,
                                onTakeDose = { id, notes -> viewModel.takeDose(id, notes) },
                                onSkipDose = { id, reason -> viewModel.skipDose(id, reason) },
                                onSnoozeDose = { id, mins -> viewModel.snoozeDose(id, mins) },
                                onResetDose = { id -> viewModel.resetDose(id) }
                            )
                            1 -> MedicationManagementScreen(
                                patient = selectedPatient,
                                medications = medications,
                                onAddMedication = { name, gen, dose, unit, route, instr, prescriber, rx, food, freq, slots ->
                                    viewModel.addMedication(
                                        selectedPatientId, name, gen, dose, unit, route,
                                        instr, prescriber, rx, food, freq, slots
                                    )
                                },
                                onDeleteMedication = { id -> viewModel.deleteMedication(id) }
                            )
                            2 -> CaregiverAlertsScreen(
                                patients = patients,
                                selectedPatientId = selectedPatientId,
                                onSelectPatient = { id -> viewModel.selectPatient(id) },
                                alerts = alerts,
                                careNotes = careNotes,
                                onResolveAlert = { id -> viewModel.resolveAlert(id) },
                                onAddCareNote = { patientId, content, type ->
                                    viewModel.addCareNote(patientId, content, type)
                                }
                            )
                            3 -> AgencyDashboardScreen(
                                patients = patients,
                                selectedPatient = selectedPatient,
                                adherenceStats = adherenceStats,
                                onSelectPatient = { id -> viewModel.selectPatient(id) },
                                onAddPatient = { name, room, dob, emName, emPhone, allergies, conditions, notes ->
                                    viewModel.addPatient(name, room, dob, emName, emPhone, allergies, conditions, notes)
                                },
                                onExportCsv = { ctx -> viewModel.exportCsv(ctx) },
                                onExportPdf = { ctx -> viewModel.exportClinicalPdf(ctx) }
                            )
                            4 -> AIAssistantScreen(
                                messages = aiMessages,
                                onSendMessage = { text -> viewModel.sendAIMessage(text) }
                            )
                            5 -> SubscriptionAndSettingsScreen(
                                session = currentSession,
                                subscriptionPlans = subscriptionPlans,
                                currentTier = currentTier,
                                isDevPaymentMode = isDevPaymentMode,
                                pendingSyncCount = pendingSyncCount,
                                auditLogs = auditLogs,
                                onSelectTier = { tier -> viewModel.selectSubscriptionTier(tier) },
                                onToggleDevMode = { viewModel.toggleDevPaymentMode() },
                                onSyncNow = { viewModel.syncNow() }
                            )
                        }
                    }
                }
            }
        }
    }
}
