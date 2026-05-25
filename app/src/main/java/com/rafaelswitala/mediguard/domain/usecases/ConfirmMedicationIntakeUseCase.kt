package com.rafaelswitala.mediguard.domain.usecases

import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import javax.inject.Inject

class ConfirmMedicationIntakeUseCase @Inject constructor(
    private val intakeHistoryRepository: IntakeHistoryRepository,
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(
        intakeHistoryId: Long,
        intakeTime: Long = System.currentTimeMillis()
    ) {
        val history = intakeHistoryRepository.getIntakeHistoryById(intakeHistoryId) ?: return
        if (history.status == IntakeStatus.CONFIRMED && history.actualIntakeTime != null) return

        intakeHistoryRepository.updateIntakeStatus(
            intakeHistoryId,
            IntakeStatus.CONFIRMED.name,
            intakeTime
        )
        medicationRepository.decrementAfterIntake(history.medicationId)
    }
}
