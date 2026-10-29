package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlertItemEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CareNoteEntity
import com.example.data.model.DoseEventEntity
import com.example.data.model.DoseStatus
import com.example.data.model.MedicationEntity
import com.example.data.model.PatientEntity
import com.example.data.model.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediSyncDao {

    // Patients
    @Query("SELECT * FROM patients ORDER BY fullName ASC")
    fun getAllPatientsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun getPatientById(patientId: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("DELETE FROM patients WHERE id = :patientId")
    suspend fun deletePatient(patientId: String)

    // Medications
    @Query("SELECT * FROM medications WHERE patientId = :patientId AND isActive = 1")
    fun getMedicationsForPatientFlow(patientId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isActive = 1")
    fun getAllActiveMedicationsFlow(): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationEntity>)

    @Query("DELETE FROM medications WHERE id = :medicationId")
    suspend fun deleteMedication(medicationId: String)

    // Dose Events
    @Query("SELECT * FROM dose_events WHERE patientId = :patientId")
    fun getDosesForPatientFlow(patientId: String): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events")
    fun getAllDosesFlow(): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE id = :doseId LIMIT 1")
    suspend fun getDoseById(doseId: String): DoseEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDose(dose: DoseEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoses(doses: List<DoseEventEntity>)

    @Update
    suspend fun updateDose(dose: DoseEventEntity)

    @Query("UPDATE dose_events SET status = :status, recordedAt = :recordedAt, recordedBy = :recordedBy, notes = :notes WHERE id = :doseId")
    suspend fun updateDoseStatus(doseId: String, status: DoseStatus, recordedAt: String, recordedBy: String, notes: String?)

    @Query("UPDATE dose_events SET status = :status, snoozeUntil = :snoozeUntil, notes = :notes WHERE id = :doseId")
    suspend fun snoozeDose(doseId: String, status: DoseStatus, snoozeUntil: String, notes: String?)

    // Alerts
    @Query("SELECT * FROM alerts ORDER BY isResolved ASC, timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<AlertItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertItemEntity>)

    @Query("UPDATE alerts SET isResolved = 1 WHERE id = :alertId")
    suspend fun resolveAlert(alertId: String)

    // Care Notes
    @Query("SELECT * FROM care_notes WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getCareNotesFlow(patientId: String): Flow<List<CareNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareNote(note: CareNoteEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Sync Queue
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingSyncItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun removeSyncItem(id: String)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getPendingSyncCountFlow(): Flow<Int>
}
