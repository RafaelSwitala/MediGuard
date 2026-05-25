package com.rafaelswitala.mediguard.domain.repository

import com.rafaelswitala.mediguard.domain.model.Medication
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    suspend fun addMedication(medication: Medication): Long
    suspend fun updateMedication(medication: Medication)
    suspend fun deleteMedication(medicationId: Long)
    suspend fun getMedicationById(id: Long): Medication?
    fun getAllActiveMedications(): Flow<List<Medication>>
    fun getAllMedications(): Flow<List<Medication>>
    fun getMedicationsOrderedByDate(): Flow<List<Medication>>
    suspend fun deactivateMedication(medicationId: Long)
    suspend fun updateRemainingDoses(medicationId: Long, remainingDoses: Int?)
    suspend fun addRemainingDoses(medicationId: Long, addedAmount: Int)
    suspend fun addRemainingVolume(medicationId: Long, addedMl: Double)
    suspend fun decrementAfterIntake(medicationId: Long)
    suspend fun updateIntakeGroup(medicationId: Long, groupId: Int?)
    @Deprecated("Use updateIntakeGroup")
    suspend fun updateMedicationGroup(medicationId: Long, groupName: String?)
}
