package com.example.data.repository

import android.content.Context
import com.example.data.local.MediSyncDao
import com.example.data.local.MediSyncDatabase
import com.example.data.model.AdherenceStats
import com.example.data.model.AlertItemEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CareNoteEntity
import com.example.data.model.DayAdherence
import com.example.data.model.DoseEventEntity
import com.example.data.model.DoseStatus
import com.example.data.model.MedicationEntity
import com.example.data.model.PatientEntity
import com.example.data.model.SubscriptionTierInfo
import com.example.data.model.SyncQueueEntity
import com.example.data.model.TimeSlot
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MediSyncRepository(private val context: Context) {

    private val db: MediSyncDatabase = MediSyncDatabase.getDatabase(context)
    private val dao: MediSyncDao = db.dao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentSession = MutableStateFlow(
        UserSession(
            id = "usr-RN-101",
            email = "elena.care@stjude-agency.org",
            fullName = "Elena Rostova, RN",
            role = UserRole.PATIENT, // default view can be toggled via TopBar / Role Switcher
            orgName = "St. Jude Home Health Care Agency",
            isOnline = true
        )
    )
    val currentSession: StateFlow<UserSession> = _currentSession.asStateFlow()

    private val _selectedPatientId = MutableStateFlow("patient-eleanor")
    val selectedPatientId: StateFlow<String> = _selectedPatientId.asStateFlow()

    private val _isDevPaymentMode = MutableStateFlow(true)
    val isDevPaymentMode: StateFlow<Boolean> = _isDevPaymentMode.asStateFlow()

    private val _currentSubscriptionTier = MutableStateFlow("agency_pro")
    val currentSubscriptionTier: StateFlow<String> = _currentSubscriptionTier.asStateFlow()

    val pendingSyncCount: Flow<Int> = dao.getPendingSyncCountFlow()

    init {
        scope.launch {
            seedInitialDataIfEmpty()
        }
    }

    fun setRole(role: UserRole) {
        _currentSession.value = _currentSession.value.copy(role = role)
        logAudit("ROLE_SWITCHED", "Elena Rostova", "UserSession", "Role changed to ${role.name}")
    }

    fun setSelectedPatient(patientId: String) {
        _selectedPatientId.value = patientId
    }

    fun toggleOnlineStatus() {
        val newStatus = !_currentSession.value.isOnline
        _currentSession.value = _currentSession.value.copy(isOnline = newStatus)
        logAudit("NETWORK_TOGGLED", "Elena Rostova", "System", "Online mode: $newStatus")
    }

    fun toggleDevPaymentMode() {
        _isDevPaymentMode.value = !_isDevPaymentMode.value
    }

    fun setSubscriptionTier(tier: String) {
        _currentSubscriptionTier.value = tier
        logAudit("SUBSCRIPTION_UPDATED", "Elena Rostova", "Billing", "Tier changed to $tier")
    }

    // Flows
    fun getPatientsFlow(): Flow<List<PatientEntity>> = dao.getAllPatientsFlow()

    fun getMedicationsForPatient(patientId: String): Flow<List<MedicationEntity>> =
        dao.getMedicationsForPatientFlow(patientId)

    fun getDosesForPatient(patientId: String): Flow<List<DoseEventEntity>> =
        dao.getDosesForPatientFlow(patientId)

    fun getAllAlerts(): Flow<List<AlertItemEntity>> = dao.getAllAlertsFlow()

    fun getCareNotes(patientId: String): Flow<List<CareNoteEntity>> =
        dao.getCareNotesFlow(patientId)

    fun getAuditLogs(): Flow<List<AuditLogEntity>> = dao.getAuditLogsFlow()

    // Dose Actions
    fun takeDose(doseId: String, notes: String? = null) {
        scope.launch {
            val nowStr = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()).format(Date())
            val actor = _currentSession.value.fullName
            dao.updateDoseStatus(
                doseId = doseId,
                status = DoseStatus.TAKEN,
                recordedAt = nowStr,
                recordedBy = actor,
                notes = notes
            )
            enqueueSync("dose_events", "UPDATE", """{"doseId":"$doseId","status":"TAKEN"}""")
            logAudit("DOSE_TAKEN", actor, "DoseEvent", "Marked dose $doseId as TAKEN at $nowStr")
        }
    }

    fun skipDose(doseId: String, reason: String) {
        scope.launch {
            val nowStr = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()).format(Date())
            val actor = _currentSession.value.fullName
            dao.updateDoseStatus(
                doseId = doseId,
                status = DoseStatus.SKIPPED,
                recordedAt = nowStr,
                recordedBy = actor,
                notes = reason
            )
            enqueueSync("dose_events", "UPDATE", """{"doseId":"$doseId","status":"SKIPPED","reason":"$reason"}""")
            logAudit("DOSE_SKIPPED", actor, "DoseEvent", "Marked dose $doseId as SKIPPED. Reason: $reason")
        }
    }

    fun snoozeDose(doseId: String, minutes: Int) {
        scope.launch {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, minutes)
            val snoozeUntilStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
            dao.snoozeDose(
                doseId = doseId,
                status = DoseStatus.SNOOZED,
                snoozeUntil = snoozeUntilStr,
                notes = "Snoozed for $minutes minutes until $snoozeUntilStr"
            )
            enqueueSync("dose_events", "UPDATE", """{"doseId":"$doseId","snoozeMinutes":$minutes}""")
            logAudit("DOSE_SNOOZED", _currentSession.value.fullName, "DoseEvent", "Snoozed dose $doseId until $snoozeUntilStr")
        }
    }

    fun resetDose(doseId: String) {
        scope.launch {
            dao.updateDoseStatus(
                doseId = doseId,
                status = DoseStatus.PENDING,
                recordedAt = "",
                recordedBy = "",
                notes = null
            )
        }
    }

    fun resolveAlert(alertId: String) {
        scope.launch {
            dao.resolveAlert(alertId)
            logAudit("ALERT_RESOLVED", _currentSession.value.fullName, "Alert", "Alert $alertId marked as resolved")
        }
    }

    fun addCareNote(patientId: String, content: String, noteType: String = "General") {
        scope.launch {
            val nowStr = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()).format(Date())
            val note = CareNoteEntity(
                id = UUID.randomUUID().toString(),
                patientId = patientId,
                authorName = _currentSession.value.fullName,
                noteType = noteType,
                content = content,
                timestamp = nowStr
            )
            dao.insertCareNote(note)
            enqueueSync("care_notes", "INSERT", """{"patientId":"$patientId","content":"$content"}""")
            logAudit("CARE_NOTE_ADDED", _currentSession.value.fullName, "CareNote", "Added $noteType care note")
        }
    }

    fun addMedication(
        patientId: String,
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
    ) {
        scope.launch {
            val medId = "med-" + UUID.randomUUID().toString().take(8)
            val med = MedicationEntity(
                id = medId,
                patientId = patientId,
                name = name,
                genericName = genericName,
                dosage = dosage,
                unit = unit,
                route = route,
                instructions = instructions,
                prescriber = prescriber,
                rxNumber = rxNumber,
                requiresFood = requiresFood,
                colorHex = "#006699",
                shape = "capsule",
                frequencyDescription = frequencyDescription,
                isActive = true
            )
            dao.insertMedication(med)

            // Generate dose events for today for the selected time slots
            val newDoses = timeSlots.map { slot ->
                DoseEventEntity(
                    id = "dose-" + UUID.randomUUID().toString().take(8),
                    medicationId = medId,
                    medicationName = name,
                    dosage = "$dosage $unit",
                    patientId = patientId,
                    scheduledTime = slot.typicalTime,
                    timeSlot = slot.name,
                    status = DoseStatus.PENDING,
                    requiresFood = requiresFood,
                    colorHex = "#006699"
                )
            }
            dao.insertDoses(newDoses)

            enqueueSync("medications", "INSERT", """{"medId":"$medId","name":"$name"}""")
            logAudit("MEDICATION_ADDED", _currentSession.value.fullName, "Medication", "Prescribed $name ($dosage $unit)")
        }
    }

    fun deleteMedication(medicationId: String) {
        scope.launch {
            dao.deleteMedication(medicationId)
            enqueueSync("medications", "DELETE", """{"medId":"$medicationId"}""")
            logAudit("MEDICATION_REMOVED", _currentSession.value.fullName, "Medication", "Medication $medicationId archived")
        }
    }

    fun addPatient(
        name: String,
        roomNumber: String,
        dob: String,
        emergencyName: String,
        emergencyPhone: String,
        allergies: String,
        conditions: String,
        notes: String
    ) {
        scope.launch {
            val patientId = "patient-" + UUID.randomUUID().toString().take(8)
            val patient = PatientEntity(
                id = patientId,
                fullName = name,
                roomNumber = roomNumber,
                dob = dob,
                emergencyContactName = emergencyName,
                emergencyContactPhone = emergencyPhone,
                allergies = allergies,
                medicalConditions = conditions,
                adherenceRate = 100.0,
                notes = notes,
                isPrimaryPatient = false
            )
            dao.insertPatient(patient)
            enqueueSync("patients", "INSERT", """{"patientId":"$patientId","name":"$name"}""")
            logAudit("PATIENT_ADMITTED", _currentSession.value.fullName, "Patient", "Admitted $name (Room: $roomNumber)")
        }
    }

    fun syncNow() {
        scope.launch {
            val pending = dao.getPendingSyncItems()
            for (item in pending) {
                // Simulate Supabase REST / GraphQL dispatch with 200 OK
                dao.removeSyncItem(item.id)
            }
            logAudit("SYNC_COMPLETED", "SyncEngine", "Supabase", "Processed ${pending.size} offline mutations")
        }
    }

    fun calculateAdherence(doses: List<DoseEventEntity>): AdherenceStats {
        val total = doses.size
        if (total == 0) {
            return AdherenceStats(100.0, 7, 0, 0, 0, emptyList())
        }
        val taken = doses.count { it.status == DoseStatus.TAKEN }
        val skipped = doses.count { it.status == DoseStatus.SKIPPED }
        val missed = doses.count { it.status == DoseStatus.MISSED }
        val pct = if (total > 0) ((taken.toDouble() / total.toDouble()) * 100.0) else 100.0
        val rounded = Math.round(pct * 10.0) / 10.0

        val days = listOf(
            DayAdherence("Mon", 100),
            DayAdherence("Tue", 85),
            DayAdherence("Wed", 100),
            DayAdherence("Thu", 92),
            DayAdherence("Fri", 100),
            DayAdherence("Sat", 88),
            DayAdherence("Sun", 95)
        )

        return AdherenceStats(
            overallAdherence = rounded,
            streakDays = 14,
            dosesTaken = taken,
            dosesSkipped = skipped,
            dosesMissed = missed,
            weeklyTrends = days
        )
    }

    fun getSubscriptionPlans(): List<SubscriptionTierInfo> {
        val current = _currentSubscriptionTier.value
        return listOf(
            SubscriptionTierInfo(
                tier = "family_care",
                name = "Family Care",
                priceDisplay = "$9.99",
                features = listOf(
                    "Up to 2 Patients",
                    "SMS & Push Reminders",
                    "AI Medication Assistant",
                    "Adherence Tracking & Charts",
                    "Caregiver Shared Calendar"
                ),
                isPopular = false,
                isCurrent = current == "family_care"
            ),
            SubscriptionTierInfo(
                tier = "agency_pro",
                name = "Home-Care Agency Pro",
                priceDisplay = "$49.99",
                features = listOf(
                    "Up to 50 Patients",
                    "20 Caregiver Accounts",
                    "Agency Web & Mobile Dashboard",
                    "Real-time Incident Alerts",
                    "CSV & Printable Clinical PDF",
                    "Audit Trail Compliance"
                ),
                isPopular = true,
                isCurrent = current == "agency_pro"
            ),
            SubscriptionTierInfo(
                tier = "enterprise",
                name = "Enterprise Healthcare",
                priceDisplay = "$199.99",
                features = listOf(
                    "Unlimited Patients & Roster",
                    "Unlimited Caregivers",
                    "EHR / HL7 / FHIR Integration",
                    "Custom Multi-tenant Supabase RLS",
                    "24/7 Priority Support & SLA"
                ),
                isPopular = false,
                isCurrent = current == "enterprise"
            )
        )
    }

    private suspend fun enqueueSync(entityType: String, action: String, payloadJson: String) {
        val item = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            action = action,
            payloadJson = payloadJson
        )
        dao.enqueueSyncItem(item)
    }

    private fun logAudit(action: String, actorName: String, resourceType: String, details: String) {
        scope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val log = AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                actorName = actorName,
                resourceType = resourceType,
                details = details,
                timestamp = nowStr
            )
            dao.insertAuditLog(log)
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val currentPatients = dao.getPatientById("patient-eleanor")
        if (currentPatients != null) return // Already seeded

        // Seed 4 realistic patients
        val patients = listOf(
            PatientEntity(
                id = "patient-eleanor",
                fullName = "Eleanor Vance",
                roomNumber = "Suite 204",
                dob = "1948-06-14",
                emergencyContactName = "David Vance (Son)",
                emergencyContactPhone = "+1 (555) 234-5678",
                allergies = "Penicillin, Sulfa",
                medicalConditions = "Hypertension, Osteoarthritis, Mild Arrhythmia",
                adherenceRate = 95.5,
                notes = "Prefers morning medications before breakfast. Remind to drink water.",
                isPrimaryPatient = true
            ),
            PatientEntity(
                id = "patient-robert",
                fullName = "Robert Chen",
                roomNumber = "Room 112",
                dob = "1953-11-20",
                emergencyContactName = "Mei Chen (Wife)",
                emergencyContactPhone = "+1 (555) 876-5432",
                allergies = "None known",
                medicalConditions = "Type 2 Diabetes, Hyperlipidemia",
                adherenceRate = 88.0,
                notes = "Monitor blood glucose post meals. Metformin strictly with food.",
                isPrimaryPatient = false
            ),
            PatientEntity(
                id = "patient-margaret",
                fullName = "Margaret Miller",
                roomNumber = "Room 305",
                dob = "1941-02-08",
                emergencyContactName = "Sarah Miller (Daughter)",
                emergencyContactPhone = "+1 (555) 345-6789",
                allergies = "Aspirin, Codeine",
                medicalConditions = "Congestive Heart Failure, CKD Stage 2",
                adherenceRate = 92.0,
                notes = "Weigh daily at 8 AM. Sodium restriction strictly enforced.",
                isPrimaryPatient = false
            ),
            PatientEntity(
                id = "patient-david",
                fullName = "David Ross",
                roomNumber = "Suite 108",
                dob = "1960-09-12",
                emergencyContactName = "Karen Ross (Sister)",
                emergencyContactPhone = "+1 (555) 456-7890",
                allergies = "Iodine contrast",
                medicalConditions = "Post-Surgical Knee Rehab, DVT Prophylaxis",
                adherenceRate = 97.0,
                notes = "Physical therapy scheduled daily at 2:00 PM.",
                isPrimaryPatient = false
            )
        )
        dao.insertPatients(patients)

        // Seed Eleanor's Medications
        val medications = listOf(
            MedicationEntity(
                id = "med-lisinopril",
                patientId = "patient-eleanor",
                name = "Lisinopril",
                genericName = "Lisinopril",
                dosage = "10",
                unit = "mg",
                route = "Oral",
                instructions = "Take every morning with water. Monitor blood pressure.",
                prescriber = "Dr. Sarah Jenkins, MD",
                rxNumber = "RX-8849201",
                requiresFood = false,
                colorHex = "#006699",
                shape = "round",
                frequencyDescription = "Daily at 8:00 AM"
            ),
            MedicationEntity(
                id = "med-metformin",
                patientId = "patient-eleanor",
                name = "Metformin HCl",
                genericName = "Metformin",
                dosage = "500",
                unit = "mg",
                route = "Oral",
                instructions = "Take with breakfast and dinner to avoid upset stomach.",
                prescriber = "Dr. Robert Zhao, Endocrinologist",
                rxNumber = "RX-8849202",
                requiresFood = true,
                colorHex = "#0D9488",
                shape = "capsule",
                frequencyDescription = "Twice daily (8:00 AM, 6:00 PM)"
            ),
            MedicationEntity(
                id = "med-atorvastatin",
                patientId = "patient-eleanor",
                name = "Atorvastatin Calcium",
                genericName = "Atorvastatin",
                dosage = "20",
                unit = "mg",
                route = "Oral",
                instructions = "Take once daily in the evening. Avoid grapefruit.",
                prescriber = "Dr. Sarah Jenkins, MD",
                rxNumber = "RX-8849203",
                requiresFood = false,
                colorHex = "#8B5CF6",
                shape = "oval",
                frequencyDescription = "Daily at 9:00 PM"
            ),
            MedicationEntity(
                id = "med-vitamin-d3",
                patientId = "patient-eleanor",
                name = "Vitamin D3 Cholecalciferol",
                genericName = "Cholecalciferol",
                dosage = "2000",
                unit = "IU",
                route = "Oral",
                instructions = "Take with lunch or largest meal of the day.",
                prescriber = "Dr. Angela Wu, Geriatrics",
                rxNumber = "OTC-392019",
                requiresFood = true,
                colorHex = "#F59E0B",
                shape = "softgel",
                frequencyDescription = "Daily at 1:00 PM"
            )
        )
        dao.insertMedications(medications)

        // Seed Today's Dose Events for Eleanor
        val doses = listOf(
            DoseEventEntity(
                id = "dose-morning-1",
                medicationId = "med-lisinopril",
                medicationName = "Lisinopril",
                dosage = "10 mg",
                patientId = "patient-eleanor",
                scheduledTime = "08:00 AM",
                timeSlot = TimeSlot.MORNING.name,
                status = DoseStatus.TAKEN,
                recordedAt = "8:05 AM",
                recordedBy = "Eleanor Vance (Self)",
                requiresFood = false,
                colorHex = "#006699"
            ),
            DoseEventEntity(
                id = "dose-morning-2",
                medicationId = "med-metformin",
                medicationName = "Metformin HCl",
                dosage = "500 mg",
                patientId = "patient-eleanor",
                scheduledTime = "08:00 AM",
                timeSlot = TimeSlot.MORNING.name,
                status = DoseStatus.TAKEN,
                recordedAt = "8:10 AM",
                recordedBy = "Eleanor Vance (Self)",
                requiresFood = true,
                colorHex = "#0D9488"
            ),
            DoseEventEntity(
                id = "dose-afternoon-1",
                medicationId = "med-vitamin-d3",
                medicationName = "Vitamin D3",
                dosage = "2000 IU",
                patientId = "patient-eleanor",
                scheduledTime = "01:00 PM",
                timeSlot = TimeSlot.AFTERNOON.name,
                status = DoseStatus.PENDING,
                requiresFood = true,
                colorHex = "#F59E0B"
            ),
            DoseEventEntity(
                id = "dose-evening-1",
                medicationId = "med-metformin",
                medicationName = "Metformin HCl",
                dosage = "500 mg",
                patientId = "patient-eleanor",
                scheduledTime = "06:00 PM",
                timeSlot = TimeSlot.EVENING.name,
                status = DoseStatus.PENDING,
                requiresFood = true,
                colorHex = "#0D9488"
            ),
            DoseEventEntity(
                id = "dose-night-1",
                medicationId = "med-atorvastatin",
                medicationName = "Atorvastatin Calcium",
                dosage = "20 mg",
                patientId = "patient-eleanor",
                scheduledTime = "09:00 PM",
                timeSlot = TimeSlot.NIGHT.name,
                status = DoseStatus.PENDING,
                requiresFood = false,
                colorHex = "#8B5CF6"
            )
        )
        dao.insertDoses(doses)

        // Seed Caregiver Alerts
        val alerts = listOf(
            AlertItemEntity(
                id = "alert-01",
                patientId = "patient-robert",
                patientName = "Robert Chen",
                severity = AlertSeverity.CRITICAL,
                type = "Missed Dose",
                message = "Morning Insulin Glargine (20 units) was scheduled at 7:30 AM but has not been confirmed. Overdue by 2 hours.",
                timestamp = "Today, 9:30 AM",
                isResolved = false
            ),
            AlertItemEntity(
                id = "alert-02",
                patientId = "patient-margaret",
                patientName = "Margaret Miller",
                severity = AlertSeverity.WARNING,
                type = "Low Inventory",
                message = "Furosemide 40mg has 3 doses remaining. Refill authorization requested from Dr. Jenkins.",
                timestamp = "Today, 8:15 AM",
                isResolved = false
            ),
            AlertItemEntity(
                id = "alert-03",
                patientId = "patient-eleanor",
                patientName = "Eleanor Vance",
                severity = AlertSeverity.INFO,
                type = "Adherence Milestone",
                message = "Eleanor has achieved a 14-day consecutive streak of 100% on-time medication adherence!",
                timestamp = "Yesterday, 9:00 PM",
                isResolved = true
            )
        )
        dao.insertAlerts(alerts)

        // Seed Initial Care Notes
        val notes = listOf(
            CareNoteEntity(
                id = "note-01",
                patientId = "patient-eleanor",
                authorName = "Elena Rostova, RN",
                noteType = "Vitals",
                content = "BP: 122/78 mmHg, Pulse: 72 bpm, O2 Sat: 98%. Eleanor reported good sleep and no morning dizziness.",
                timestamp = "Today, 8:20 AM"
            ),
            CareNoteEntity(
                id = "note-02",
                patientId = "patient-eleanor",
                authorName = "Elena Rostova, RN",
                noteType = "General",
                content = "Refilled pill organizer for the week. All child-resistant caps checked and loosened slightly for patient's arthritis comfort.",
                timestamp = "Yesterday, 3:30 PM"
            )
        )
        for (n in notes) dao.insertCareNote(n)

        // Seed Initial Audit Logs
        val auditLogs = listOf(
            AuditLogEntity(
                id = "audit-01",
                action = "SYSTEM_INITIALIZED",
                actorName = "MediSync Engine",
                resourceType = "Database",
                details = "Initialized Supabase schema migration and encrypted local cache",
                timestamp = "2026-10-29 08:00:00"
            ),
            AuditLogEntity(
                id = "audit-02",
                action = "ROSTER_SYNCHRONIZED",
                actorName = "Elena Rostova, RN",
                resourceType = "Agency",
                details = "Fetched 4 patients from St. Jude Home Health Care Agency backend",
                timestamp = "2026-10-29 08:01:15"
            )
        )
        for (a in auditLogs) dao.insertAuditLog(a)
    }
}
