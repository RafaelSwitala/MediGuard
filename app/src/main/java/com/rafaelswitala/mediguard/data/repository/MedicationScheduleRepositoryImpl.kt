package com.rafaelswitala.mediguard.data.repository

/**
 * Implementierung des MedicationScheduleRepository: Verwaltung von Erinnerungszeiten in der Datenbank.
 */

import com.rafaelswitala.mediguard.data.local.dao.MedicationScheduleDao
import com.rafaelswitala.mediguard.data.local.entity.MedicationScheduleEntity
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of MedicationScheduleRepository
 * Handles conversation between domain models and database entities for schedules
 */
class MedicationScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: MedicationScheduleDao
) : MedicationScheduleRepository {

    override suspend fun addSchedule(schedule: MedicationSchedule): Long {
        return scheduleDao.insert(schedule.toEntity())
    }

    override suspend fun updateSchedule(schedule: MedicationSchedule) {
        scheduleDao.update(schedule.toEntity())
    }

    override suspend fun deleteSchedule(scheduleId: Long) {
        val schedule = scheduleDao.getScheduleById(scheduleId)
        schedule?.let { scheduleDao.delete(it) }
    }

    override suspend fun getScheduleById(id: Long): MedicationSchedule? {
        return scheduleDao.getScheduleById(id)?.toDomain()
    }

    override fun getSchedulesByMedicationId(medicationId: Long): Flow<List<MedicationSchedule>> {
        return scheduleDao.getSchedulesByMedicationId(medicationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveSchedulesByMedicationId(medicationId: Long): Flow<List<MedicationSchedule>> {
        return scheduleDao.getActiveSchedulesByMedicationId(medicationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllActiveSchedules(): Flow<List<MedicationSchedule>> {
        return scheduleDao.getAllActiveSchedules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSchedulesByType(scheduleType: String): Flow<List<MedicationSchedule>> {
        return scheduleDao.getSchedulesByType(scheduleType).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateScheduleStatus(scheduleId: Long, enabled: Boolean) {
        scheduleDao.updateScheduleStatus(scheduleId, enabled)
    }

    override suspend fun deleteSchedulesByMedicationId(medicationId: Long) {
        scheduleDao.deleteSchedulesByMedicationId(medicationId)
    }

    // Mapping functions
    private fun MedicationSchedule.toEntity(): MedicationScheduleEntity = MedicationScheduleEntity(
        id = id,
        medicationId = medicationId,
        scheduleType = scheduleType.name,
        scheduleData = scheduleData,
        reminderMinutes = reminderMinutes,
        snoozeOptions = snoozeOptions.joinToString(","),
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    private fun MedicationScheduleEntity.toDomain(): MedicationSchedule {
        val scheduleType = runCatching {
            FrequencyType.valueOf(scheduleType)
        }.getOrDefault(FrequencyType.EXACT_TIME)

        return MedicationSchedule(
            id = id,
            medicationId = medicationId,
            scheduleType = scheduleType,
            scheduleData = scheduleData,
            reminderMinutes = reminderMinutes,
            snoozeOptions = snoozeOptions.split(",").mapNotNull { it.toIntOrNull() },
            isEnabled = isEnabled,
            createdAt = createdAt
        )
    }
}
