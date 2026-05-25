package com.rafaelswitala.mediguard.data.repository

import com.rafaelswitala.mediguard.data.local.dao.MedicationDao
import com.rafaelswitala.mediguard.data.local.entity.MedicationEntity
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationFormType
import com.rafaelswitala.mediguard.domain.model.TreatmentType
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicationRepositoryImpl @Inject constructor(
    private val medicationDao: MedicationDao
) : MedicationRepository {

    override suspend fun addMedication(medication: Medication): Long =
        medicationDao.insert(medication.toEntity())

    override suspend fun updateMedication(medication: Medication) {
        medicationDao.update(medication.toEntity())
    }

    override suspend fun deleteMedication(medicationId: Long) {
        medicationDao.deleteById(medicationId)
    }

    override suspend fun getMedicationById(id: Long): Medication? =
        medicationDao.getMedicationById(id)?.toDomain()

    override fun getAllActiveMedications(): Flow<List<Medication>> =
        medicationDao.getAllActiveMedications().map { entities -> entities.map { it.toDomain() } }

    override fun getAllMedications(): Flow<List<Medication>> =
        medicationDao.getAllMedications().map { entities -> entities.map { it.toDomain() } }

    override fun getMedicationsOrderedByDate(): Flow<List<Medication>> =
        medicationDao.getMedicationsOrderedByDate().map { entities -> entities.map { it.toDomain() } }

    override suspend fun deactivateMedication(medicationId: Long) {
        medicationDao.deactivateMedication(medicationId)
    }

    override suspend fun updateRemainingDoses(medicationId: Long, remainingDoses: Int?) {
        medicationDao.updateRemainingDoses(medicationId, remainingDoses)
    }

    override suspend fun addRemainingDoses(medicationId: Long, addedAmount: Int) {
        val medication = getMedicationById(medicationId) ?: return
        val current = medication.remainingDoses ?: 0
        medicationDao.updateRemainingDoses(medicationId, current + addedAmount)
    }

    override suspend fun addRemainingVolume(medicationId: Long, addedMl: Double) {
        val medication = getMedicationById(medicationId) ?: return
        val current = medication.remainingVolumeMl ?: 0.0
        medicationDao.updateRemainingVolume(medicationId, current + addedMl)
    }

    override suspend fun decrementAfterIntake(medicationId: Long) {
        val medication = getMedicationById(medicationId) ?: return
        if (medication.medicationFormType.usesVolume()) {
            val perDose = medication.dosePerIntakeMl ?: return
            val total = perDose * medication.doseQuantity.coerceAtLeast(1)
            val next = medication.remainingVolumeMl?.minus(total)?.coerceAtLeast(0.0) ?: return
            medicationDao.updateRemainingVolume(medicationId, next)
        } else {
            val next = medication.remainingDoses?.minus(medication.doseQuantity.coerceAtLeast(1))
                ?.coerceAtLeast(0) ?: return
            medicationDao.updateRemainingDoses(medicationId, next)
        }
    }

    override suspend fun updateIntakeGroup(medicationId: Long, groupId: Int?) {
        medicationDao.updateIntakeGroup(medicationId, groupId)
    }

    override suspend fun updateMedicationGroup(medicationId: Long, groupName: String?) {
        medicationDao.updateMedicationGroup(medicationId, groupName?.takeIf { it.isNotBlank() })
    }

    private fun Medication.toEntity(): MedicationEntity = MedicationEntity(
        id = id,
        name = name,
        dosage = dosage,
        dosageUnit = dosageUnit,
        description = description,
        treatmentType = treatmentType.name,
        durationDays = durationDays,
        treatmentLimitDoses = treatmentLimitDoses,
        remainingDoses = remainingDoses,
        remainingVolumeMl = remainingVolumeMl,
        dosePerIntakeMl = dosePerIntakeMl,
        doseQuantity = doseQuantity,
        medicationFormType = medicationFormType.name,
        intakeGroupId = intakeGroupId,
        groupName = groupName,
        supplyAlertEnabled = supplyAlertEnabled,
        supplyAlertThreshold = supplyAlertThreshold,
        createdAt = createdAt,
        isActive = isActive
    )

    private fun MedicationEntity.toDomain(): Medication = Medication(
        id = id,
        name = name,
        dosage = dosage,
        dosageUnit = dosageUnit,
        description = description,
        treatmentType = runCatching { TreatmentType.valueOf(treatmentType) }
            .getOrDefault(TreatmentType.ONGOING),
        durationDays = durationDays,
        treatmentLimitDoses = treatmentLimitDoses,
        remainingDoses = remainingDoses,
        remainingVolumeMl = remainingVolumeMl,
        dosePerIntakeMl = dosePerIntakeMl,
        doseQuantity = doseQuantity,
        medicationFormType = runCatching { MedicationFormType.valueOf(medicationFormType) }
            .getOrDefault(MedicationFormType.TABLET),
        intakeGroupId = intakeGroupId,
        groupName = groupName,
        supplyAlertEnabled = supplyAlertEnabled,
        supplyAlertThreshold = supplyAlertThreshold,
        createdAt = createdAt,
        isActive = isActive
    )
}
