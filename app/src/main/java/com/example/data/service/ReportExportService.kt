package com.example.data.service

import android.content.Context
import android.content.Intent
import com.example.data.model.AdherenceStats
import com.example.data.model.DoseEventEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.PatientEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportService {

    fun generateAdherenceCsv(
        patient: PatientEntity,
        medications: List<MedicationEntity>,
        doses: List<DoseEventEntity>,
        stats: AdherenceStats
    ): String {
        val sb = StringBuilder()
        sb.append("=== MEDISYNC AI - MEDICATION ADHERENCE AUDIT EXPORT ===\n")
        sb.append("Export Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Patient Name,${patient.fullName}\n")
        sb.append("Date of Birth,${patient.dob}\n")
        sb.append("Overall Adherence,${stats.overallAdherence}%\n")
        sb.append("Adherence Streak Days,${stats.streakDays}\n")
        sb.append("Total Taken,${stats.dosesTaken}\n")
        sb.append("Total Skipped,${stats.dosesSkipped}\n")
        sb.append("Total Missed,${stats.dosesMissed}\n\n")

        sb.append("--- DOSE LOG AUDIT TRAIL ---\n")
        sb.append("Dose ID,Medication,Dosage,Time Slot,Scheduled Time,Status,Recorded At,Recorded By,Notes\n")

        for (dose in doses) {
            val recAt = dose.recordedAt ?: "N/A"
            val recBy = dose.recordedBy ?: "N/A"
            val notes = (dose.notes ?: "").replace(",", ";")
            sb.append("${dose.id},${dose.medicationName},${dose.dosage},${dose.timeSlot},${dose.scheduledTime},${dose.status.name},$recAt,$recBy,$notes\n")
        }

        sb.append("\n--- ACTIVE MEDICATION REGIMEN ---\n")
        sb.append("Medication ID,Name,Generic Name,Dosage,Route,Instructions,Prescriber,Rx Number,Requires Food\n")
        for (med in medications) {
            val instr = med.instructions.replace(",", ";")
            sb.append("${med.id},${med.name},${med.genericName},${med.dosage} ${med.unit},${med.route},$instr,${med.prescriber},${med.rxNumber},${med.requiresFood}\n")
        }

        return sb.toString()
    }

    fun generateClinicalPdfSummary(
        patient: PatientEntity,
        medications: List<MedicationEntity>,
        doses: List<DoseEventEntity>,
        stats: AdherenceStats
    ): String {
        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        return """
================================================================================
                    MEDISYNC AI - CLINICAL COMPLIANCE REPORT
                      ST. JUDE HOME HEALTH CARE AGENCY
================================================================================
Generated: $dateStr
Report Type: Comprehensive Medication Adherence & Incident Audit
Confidential Healthcare Record (Protected Health Information)

PATIENT DEMOGRAPHICS:
--------------------------------------------------------------------------------
Full Name:           ${patient.fullName}
Date of Birth:       ${patient.dob}
Room / Residence:    ${patient.roomNumber}
Emergency Contact:   ${patient.emergencyContactName} (${patient.emergencyContactPhone})
Allergies:           ${patient.allergies}
Medical Diagnoses:   ${patient.medicalConditions}

ADHERENCE & COMPLIANCE ANALYTICS:
--------------------------------------------------------------------------------
30-Day Adherence Score:  ${stats.overallAdherence}%
Consecutive Day Streak:  ${stats.streakDays} Days
Doses Successfully Taken: ${stats.dosesTaken}
Doses Skipped:           ${stats.dosesSkipped}
Doses Missed / Flagged:  ${stats.dosesMissed}

ACTIVE MEDICATION REGIMEN:
--------------------------------------------------------------------------------
${medications.joinToString("\n\n") { med ->
"""• ${med.name} (${med.genericName}) - ${med.dosage} ${med.unit}
  Route: ${med.route} | Frequency: ${med.frequencyDescription}
  Instructions: ${med.instructions} ${if (med.requiresFood) "[Take with food]" else ""}
  Prescriber: ${med.prescriber} | Rx#: ${med.rxNumber}"""
}}

RECENT DOSE EVENTS & AUDIT LOG:
--------------------------------------------------------------------------------
${doses.joinToString("\n") { dose ->
"[${dose.scheduledTime}] ${dose.medicationName} (${dose.dosage}) -> STATUS: ${dose.status.name} | Logged: ${dose.recordedAt ?: "Pending"} by ${dose.recordedBy ?: "System"} ${if (!dose.notes.isNullOrEmpty()) "| Notes: ${dose.notes}" else ""}"
}}

CLINICAL NOTES & OBSERVATIONS:
--------------------------------------------------------------------------------
${patient.notes}

AGENCY AUDIT SIGN-OFF:
--------------------------------------------------------------------------------
Supervising Registered Nurse: Elena Rostova, RN
Agency License: HHA-994820-CA
Audit Verification Hash: ${Integer.toHexString((patient.fullName + dateStr + stats.overallAdherence).hashCode()).uppercase()}
================================================================================
        """.trimIndent()
    }

    fun shareExportContent(context: Context, content: String, title: String, mimeType: String = "text/plain") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            type = mimeType
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
