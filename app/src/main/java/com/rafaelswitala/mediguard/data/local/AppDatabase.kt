package com.rafaelswitala.mediguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rafaelswitala.mediguard.data.local.dao.IntakeHistoryDao
import com.rafaelswitala.mediguard.data.local.dao.MedicationDao
import com.rafaelswitala.mediguard.data.local.dao.MedicationScheduleDao
import com.rafaelswitala.mediguard.data.local.entity.IntakeHistoryEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicationEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicationScheduleEntity

/**
 * Lokale Room-Datenbank für Medikamente, Zeitpläne und Einnahmeverlauf.
 * Speichert sensible App-Daten im Credential-Encrypted-Bereich.
 */
@Database(
    entities = [
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        IntakeHistoryEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao

    companion object {
        const val DATABASE_NAME = "mediguard_db"
    }
}
