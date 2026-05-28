package com.rafaelswitala.mediguard.data.local.entity

/**
 * Datenbanktabelle für den Einnahmeverlauf mit Status und Zeitstempeln.
 */

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for IntakeHistory
 * Stores medication intake confirmations in Credential Encrypted (CE) storage
 */
@Entity(
    tableName = "intake_history",
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
data class IntakeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduledTime: Long,  // Timestamp
    val actualIntakeTime: Long? = null,  // Null if not taken
    val dosage: String,
    val dosageUnit: String,
    val medicationName: String,
    val status: String = "PENDING"  // PENDING, CONFIRMED, MISSED, SNOOZED
)
