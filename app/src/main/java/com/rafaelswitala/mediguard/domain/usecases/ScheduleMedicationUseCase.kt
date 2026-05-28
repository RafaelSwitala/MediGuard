package com.rafaelswitala.mediguard.domain.usecases

/**
 * Datei für den fachlichen Anwendungsfall ScheduleMedicationUseCase.
 * Wichtig: Nur die zentrale Aufgabe dieser Komponente ist hier kommentiert.
 */

import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import javax.inject.Inject

/**
 * Erstellt wiederkehrende oder einmalige Erinnerungen für Medikamente.
 */
class ScheduleMedicationUseCase @Inject constructor(
    private val scheduleRepository: MedicationScheduleRepository
) {
    suspend operator fun invoke(schedule: MedicationSchedule): Long =
        scheduleRepository.addSchedule(schedule)
}