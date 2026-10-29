package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AlertItemEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CareNoteEntity
import com.example.data.model.DoseEventEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.PatientEntity
import com.example.data.model.SyncQueueEntity

@Database(
    entities = [
        PatientEntity::class,
        MedicationEntity::class,
        DoseEventEntity::class,
        AlertItemEntity::class,
        CareNoteEntity::class,
        AuditLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediSyncDatabase : RoomDatabase() {
    abstract fun dao(): MediSyncDao

    companion object {
        @Volatile
        private var INSTANCE: MediSyncDatabase? = null

        fun getDatabase(context: Context): MediSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediSyncDatabase::class.java,
                    "medisync_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
