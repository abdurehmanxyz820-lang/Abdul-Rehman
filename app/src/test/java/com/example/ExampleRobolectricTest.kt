package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.service.AIEducationService
import com.example.data.service.ReportExportService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context verifies MediSync AI brand`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MediSync AI", appName)
  }

  @Test
  fun `test AI assistant medical disclaimer is present in query response`() {
    val reply = AIEducationService.processQuery("What should I do if I miss a dose of Lisinopril?")
    assertTrue(reply.isMedicalDisclaimer)
    assertTrue(reply.text.contains("MEDICAL DISCLAIMER"))
    assertTrue(reply.text.contains("Never Take a Double Dose"))
  }

  @Test
  fun `test ReportExportService formats CSV and Clinical Summary properly`() {
    val patient = PatientEntity(
      id = "test-patient",
      fullName = "Eleanor Vance",
      roomNumber = "Suite 204",
      dob = "1948-06-14"
    )
    val med = MedicationEntity(
      id = "test-med",
      patientId = "test-patient",
      name = "Lisinopril",
      genericName = "Lisinopril",
      dosage = "10",
      unit = "mg",
      route = "Oral",
      instructions = "Take every morning",
      prescriber = "Dr. Jenkins",
      rxNumber = "RX-12345"
    )
    val dose = DoseEventEntity(
      id = "test-dose",
      medicationId = "test-med",
      medicationName = "Lisinopril",
      dosage = "10 mg",
      patientId = "test-patient",
      scheduledTime = "08:00 AM",
      timeSlot = "MORNING",
      status = DoseStatus.TAKEN,
      recordedAt = "8:05 AM",
      recordedBy = "Eleanor Vance"
    )
    val stats = AdherenceStats(
      overallAdherence = 100.0,
      streakDays = 14,
      dosesTaken = 1,
      dosesSkipped = 0,
      dosesMissed = 0,
      weeklyTrends = emptyList()
    )

    val csv = ReportExportService.generateAdherenceCsv(patient, listOf(med), listOf(dose), stats)
    assertTrue(csv.contains("Eleanor Vance"))
    assertTrue(csv.contains("Lisinopril"))
    assertTrue(csv.contains("TAKEN"))

    val summary = ReportExportService.generateClinicalPdfSummary(patient, listOf(med), listOf(dose), stats)
    assertTrue(summary.contains("CLINICAL COMPLIANCE REPORT"))
    assertTrue(summary.contains("100.0%"))
    assertTrue(summary.contains("Dr. Jenkins"))
  }
}
