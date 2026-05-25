package com.rafaelswitala.mediguard.data.repository

import com.rafaelswitala.mediguard.data.local.dao.IntakeHistoryDao
import com.rafaelswitala.mediguard.data.local.entity.IntakeHistoryEntity
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of IntakeHistoryRepository
 * Handles conversation between domain models and database entities for intake history
 */
class IntakeHistoryRepositoryImpl @Inject constructor(
    private val intakeHistoryDao: IntakeHistoryDao
) : IntakeHistoryRepository {

    override suspend fun recordIntake(intakeHistory: IntakeHistory): Long {
        return intakeHistoryDao.insert(intakeHistory.toEntity())
    }

    override suspend fun updateIntakeStatus(intakeHistoryId: Long, status: String, intakeTime: Long?) {
        intakeHistoryDao.updateIntakeStatus(intakeHistoryId, status, intakeTime)
    }

    override suspend fun getIntakeHistoryById(id: Long): IntakeHistory? {
        return intakeHistoryDao.getIntakeHistoryById(id)?.toDomain()
    }

    override fun getIntakeHistoryByMedicationId(medicationId: Long): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getIntakeHistoryByMedicationId(medicationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllIntakeHistory(): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getAllIntakeHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getIntakeHistoryInRange(from: Long, to: Long): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getIntakeHistoryInRange(from, to).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getIntakeHistoryByStatus(status: String): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getIntakeHistoryByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getIntakeHistoryByMedicationAndStatus(
        medicationId: Long,
        status: String
    ): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getIntakeHistoryByMedicationAndStatus(medicationId, status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Mapping functions
    private fun IntakeHistory.toEntity(): IntakeHistoryEntity = IntakeHistoryEntity(
        id = id,
        medicationId = medicationId,
        scheduledTime = scheduledTime,
        actualIntakeTime = actualIntakeTime,
        dosage = dosage,
        dosageUnit = dosageUnit,
        medicationName = medicationName,
        status = status.name
    )

    private fun IntakeHistoryEntity.toDomain(): IntakeHistory = IntakeHistory(
        id = id,
        medicationId = medicationId,
        scheduledTime = scheduledTime,
        actualIntakeTime = actualIntakeTime,
        dosage = dosage,
        dosageUnit = dosageUnit,
        medicationName = medicationName,
        status = IntakeStatus.valueOf(status)
    )
}
