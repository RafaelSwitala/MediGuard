package com.rafaelswitala.mediguard.domain.repository

/**
 * Repository für Einnahmeverlauf: Speichert und verwaltet alle Einnahmebestätigungen.
 */

import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for IntakeHistory operations
 */
interface IntakeHistoryRepository {
    suspend fun recordIntake(intakeHistory: IntakeHistory): Long
    suspend fun updateIntakeStatus(intakeHistoryId: Long, status: String, intakeTime: Long?)
    suspend fun getIntakeHistoryById(id: Long): IntakeHistory?
    fun getIntakeHistoryByMedicationId(medicationId: Long): Flow<List<IntakeHistory>>
    fun getAllIntakeHistory(): Flow<List<IntakeHistory>>
    fun getIntakeHistoryInRange(from: Long, to: Long): Flow<List<IntakeHistory>>
    fun getIntakeHistoryByStatus(status: String): Flow<List<IntakeHistory>>
    fun getIntakeHistoryByMedicationAndStatus(medicationId: Long, status: String): Flow<List<IntakeHistory>>
}
