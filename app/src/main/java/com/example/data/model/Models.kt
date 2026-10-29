package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    PATIENT,
    CAREGIVER,
    AGENCY_ADMIN,
    SYSTEM_ADMIN
}

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    SNOOZED
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

enum class TimeSlot(val title: String, val typicalTime: String) {
    MORNING("Morning", "08:00 AM"),
    AFTERNOON("Afternoon", "01:00 PM"),
    EVENING("Evening", "06:00 PM"),
    NIGHT("Night", "09:00 PM")
}

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val orgId: String = "org-main",
    val fullName: String,
    val roomNumber: String = "Suite 204",
    val dob: String = "1948-06-14",
    val emergencyContactName: String = "David Vance (Son)",
    val emergencyContactPhone: String = "+1 (555) 234-5678",
    val allergies: String = "Penicillin, Sulfa",
    val medicalConditions: String = "Hypertension, Type 2 Diabetes, Osteoarthritis",
    val adherenceRate: Double = 94.0,
    val notes: String = "Prefers liquid dosage where possible. Remind to drink water.",
    val isPrimaryPatient: Boolean = false
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val name: String,
    val genericName: String,
    val dosage: String,
    val unit: String = "mg",
    val route: String = "Oral", // Oral, Injection, Inhaler, Topical, Drops
    val instructions: String,
    val prescriber: String,
    val rxNumber: String,
    val requiresFood: Boolean = false,
    val colorHex: String = "#006699",
    val shape: String = "capsule", // capsule, round, oval, liquid
    val frequencyDescription: String = "Once daily in the morning",
    val isActive: Boolean = true
)

@Entity(tableName = "dose_events")
data class DoseEventEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val medicationName: String,
    val dosage: String,
    val patientId: String,
    val scheduledTime: String, // e.g. "08:00 AM"
    val timeSlot: String, // MORNING, AFTERNOON, EVENING, NIGHT
    val status: DoseStatus = DoseStatus.PENDING,
    val recordedAt: String? = null,
    val recordedBy: String? = null,
    val snoozeUntil: String? = null,
    val notes: String? = null,
    val requiresFood: Boolean = false,
    val colorHex: String = "#006699"
)

@Entity(tableName = "alerts")
data class AlertItemEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val severity: AlertSeverity,
    val type: String,
    val message: String,
    val timestamp: String,
    val isResolved: Boolean = false
)

@Entity(tableName = "care_notes")
data class CareNoteEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val authorName: String,
    val noteType: String = "General",
    val content: String,
    val timestamp: String
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val action: String,
    val actorName: String,
    val resourceType: String,
    val details: String,
    val timestamp: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val action: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

data class UserSession(
    val id: String = "usr-01",
    val email: String = "elena.care@medisync.io",
    val fullName: String = "Elena Rostova, RN",
    val role: UserRole = UserRole.PATIENT,
    val orgName: String = "St. Jude Home Health Care Agency",
    val isOnline: Boolean = true
)

data class SubscriptionTierInfo(
    val tier: String,
    val name: String,
    val priceDisplay: String,
    val billingCycle: String = "/ month",
    val features: List<String>,
    val isPopular: Boolean = false,
    val isCurrent: Boolean = false
)

data class AdherenceStats(
    val overallAdherence: Double,
    val streakDays: Int,
    val dosesTaken: Int,
    val dosesSkipped: Int,
    val dosesMissed: Int,
    val weeklyTrends: List<DayAdherence>
)

data class DayAdherence(
    val dayLabel: String,
    val adherencePercent: Int
)

data class AIChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: String,
    val isMedicalDisclaimer: Boolean = false
)
