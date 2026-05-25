package com.rafaelswitala.mediguard.domain.usecases

import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import javax.inject.Inject

/**
 * Use case for scheduling a medication
 * Creates recurring or one-time reminders for medications
 */
class ScheduleMedicationUseCase @Inject constructor(
    private val scheduleRepository: MedicationScheduleRepository
) {
    suspend operator fun invoke(schedule: MedicationSchedule): Long =
        scheduleRepository.addSchedule(schedule)
}