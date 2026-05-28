package com.rafaelswitala.mediguard.data.local.entity

/**
 * Datenbanktabelle für Erinnerungszeiten mit Häufigkeit und Zeitparan und Typ.
 */

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for MedicationSchedule
 * Stores schedule information in Credential Encrypted (CE) storage
 */
@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduleType: String,  // ExactTime, TimeRange, etc.
    val scheduleData: String,  // JSON
    val reminderMinutes: Int = 15,
    val snoozeOptions: String = "5,10,15,30,60",  // CSV format
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
