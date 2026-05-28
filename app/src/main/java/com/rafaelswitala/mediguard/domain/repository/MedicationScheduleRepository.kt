package com.rafaelswitala.mediguard.domain.repository

/**
 * Repository für Erinnerungszeiten: Verwaltung von Zeitplänen für jedes Medikament.
 */

import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for MedicationSchedule operations
 */
interface MedicationScheduleRepository {
    suspend fun addSchedule(schedule: MedicationSchedule): Long
    suspend fun updateSchedule(schedule: MedicationSchedule)
    suspend fun deleteSchedule(scheduleId: Long)
    suspend fun getScheduleById(id: Long): MedicationSchedule?
    fun getSchedulesByMedicationId(medicationId: Long): Flow<List<MedicationSchedule>>
    fun getActiveSchedulesByMedicationId(medicationId: Long): Flow<List<MedicationSchedule>>
    fun getAllActiveSchedules(): Flow<List<MedicationSchedule>>
    fun getSchedulesByType(scheduleType: String): Flow<List<MedicationSchedule>>
    suspend fun updateScheduleStatus(scheduleId: Long, enabled: Boolean)
    suspend fun deleteSchedulesByMedicationId(medicationId: Long)
}
