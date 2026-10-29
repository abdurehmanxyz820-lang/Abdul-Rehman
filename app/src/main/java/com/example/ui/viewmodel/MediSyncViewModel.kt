package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AIChatMessage
import com.example.data.model.AdherenceStats
import com.example.data.model.AlertItemEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CareNoteEntity
import com.example.data.model.DoseEventEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.PatientEntity
import com.example.data.model.SubscriptionTierInfo
import com.example.data.model.TimeSlot
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.repository.MediSyncRepository
import com.example.data.service.AIEducationService
import com.example.data.service.ReportExportService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MediSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediSyncRepository(application.applicationContext)

    val currentSession: StateFlow<UserSession> = repository.currentSession
    val selectedPatientId: StateFlow<String> = repository.selectedPatientId
    val isDevPaymentMode: StateFlow<Boolean> = repository.isDevPaymentMode
    val currentSubscriptionTier: StateFlow<String> = repository.currentSubscriptionTier
    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val patients: StateFlow<List<PatientEntity>> = repository.getPatientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPatient: StateFlow<PatientEntity?> = selectedPatientId.flatMapLatest { id ->
        patients.map { list -> list.find { it.id == id } ?: list.firstOrNull() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val medications: StateFlow<List<MedicationEntity>> = selectedPatientId.flatMapLatest { id ->
        repository.getMedicationsForPatient(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doses: StateFlow<List<DoseEventEntity>> = selectedPatientId.flatMapLatest { id ->
        repository.getDosesForPatient(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adherenceStats: StateFlow<AdherenceStats> = doses.map { doseList ->
        repository.calculateAdherence(doseList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdherenceStats(100.0, 14, 0, 0, 0, emptyList()))

    val alerts: StateFlow<List<AlertItemEntity>> = repository.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careNotes: StateFlow<List<CareNoteEntity>> = selectedPatientId.flatMapLatest { id ->
        repository.getCareNotes(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiMessages = MutableStateFlow<List<AIChatMessage>>(
        listOf(AIEducationService.getInitialGreeting())
    )
    val aiMessages: StateFlow<List<AIChatMessage>> = _aiMessages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Actions
    fun setRole(role: UserRole) {
        repository.setRole(role)
    }

    fun selectPatient(patientId: String) {
        repository.setSelectedPatient(patientId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleOnlineStatus() {
        repository.toggleOnlineStatus()
    }

    fun syncNow() {
        repository.syncNow()
    }

    fun takeDose(doseId: String, notes: String? = null) {
        repository.takeDose(doseId, notes)
    }

    fun skipDose(doseId: String, reason: String) {
        repository.skipDose(doseId, reason)
    }

    fun snoozeDose(doseId: String, minutes: Int) {
        repository.snoozeDose(doseId, minutes)
    }

    fun resetDose(doseId: String) {
        repository.resetDose(doseId)
    }

    fun resolveAlert(alertId: String) {
        repository.resolveAlert(alertId)
    }

    fun addCareNote(patientId: String, content: String, noteType: String = "General") {
        repository.addCareNote(patientId, content, noteType)
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
        repository.addMedication(
            patientId, name, genericName, dosage, unit, route,
            instructions, prescriber, rxNumber, requiresFood,
            frequencyDescription, timeSlots
        )
    }

    fun deleteMedication(medicationId: String) {
        repository.deleteMedication(medicationId)
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
        repository.addPatient(name, roomNumber, dob, emergencyName, emergencyPhone, allergies, conditions, notes)
    }

    fun sendAIMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = AIChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            isUser = true,
            text = userText,
            timestamp = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        )
        _aiMessages.value = _aiMessages.value + userMsg

        viewModelScope.launch {
            // Process AI response with medical educational safety guardrails
            val botMsg = AIEducationService.processQuery(userText)
            _aiMessages.value = _aiMessages.value + botMsg
        }
    }

    fun exportCsv(context: Context) {
        val patient = selectedPatient.value ?: return
        val meds = medications.value
        val doseList = doses.value
        val stats = adherenceStats.value
        val csv = ReportExportService.generateAdherenceCsv(patient, meds, doseList, stats)
        ReportExportService.shareExportContent(
            context,
            csv,
            "MediSync_Adherence_${patient.fullName.replace(" ", "_")}.csv",
            "text/comma-separated-values"
        )
    }

    fun exportClinicalPdf(context: Context) {
        val patient = selectedPatient.value ?: return
        val meds = medications.value
        val doseList = doses.value
        val stats = adherenceStats.value
        val clinicalReport = ReportExportService.generateClinicalPdfSummary(patient, meds, doseList, stats)
        ReportExportService.shareExportContent(
            context,
            clinicalReport,
            "Clinical_Report_${patient.fullName.replace(" ", "_")}.txt",
            "text/plain"
        )
    }

    fun getSubscriptionPlans(): List<SubscriptionTierInfo> = repository.getSubscriptionPlans()

    fun selectSubscriptionTier(tier: String) {
        repository.setSubscriptionTier(tier)
    }

    fun toggleDevPaymentMode() {
        repository.toggleDevPaymentMode()
    }
}
