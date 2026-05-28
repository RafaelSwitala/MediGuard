package com.rafaelswitala.mediguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Datenbanktabelle für Medikamente mit allen Stammdaten, Bestand und Behandlungstyp.
 * Speichert im Credential-Encrypted-Bereich (nach Unlock zugänglich).
 */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val dosageUnit: String = "mg",
    val description: String = "",
    val treatmentType: String = "ONGOING",
    val durationDays: Int? = null,
    val treatmentLimitDoses: Int? = null,
    val remainingDoses: Int? = null,
    val remainingDosesDecimal: Double? = null,
    val remainingVolumeMl: Double? = null,
    val dosePerIntakeMl: Double? = null,
    val doseQuantity: Int = 1,
    val doseQuantityDecimal: Double = 1.0,
    val medicationFormType: String = "TABLET",
    val intakeGroupId: Int? = null,
    val groupName: String? = null,
    val supplyAlertEnabled: Boolean = true,
    val supplyAlertThreshold: Int = 7,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
