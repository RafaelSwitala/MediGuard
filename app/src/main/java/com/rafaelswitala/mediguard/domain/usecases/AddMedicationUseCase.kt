package com.rafaelswitala.mediguard.domain.usecases

/**
 * Datei für den fachlichen Anwendungsfall AddMedicationUseCase.
 * Wichtig: Nur die zentrale Aufgabe dieser Komponente ist hier kommentiert.
 */

import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Fügt ein neues Medikament hinzu und speichert es im Repository.
 */
class AddMedicationUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(medication: Medication): Long = 
        medicationRepository.addMedication(medication)
}