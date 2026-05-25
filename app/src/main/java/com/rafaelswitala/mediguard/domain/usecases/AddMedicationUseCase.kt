package com.rafaelswitala.mediguard.domain.usecases

import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Use case for adding a new medication
 * Encapsulates business logic for medication addition
 */
class AddMedicationUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(medication: Medication): Long = 
        medicationRepository.addMedication(medication)
}