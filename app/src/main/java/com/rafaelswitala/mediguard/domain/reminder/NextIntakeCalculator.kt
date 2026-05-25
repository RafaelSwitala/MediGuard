package com.rafaelswitala.mediguard.domain.reminder

import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpcomingIntake(
    val medication: Medication,
    val schedule: MedicationSchedule,
    val triggerAtMillis: Long
) {
    fun formattedTime(locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", locale)
        return sdf.format(Date(triggerAtMillis))
    }

    fun formattedTimeOnly(locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("HH:mm", locale)
        return sdf.format(Date(triggerAtMillis))
    }
}

object NextIntakeCalculator {
    fun upcoming(
        medications: List<Medication>,
        schedulesByMedication: Map<Long, List<MedicationSchedule>>,
        now: Long = System.currentTimeMillis(),
        dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default,
        limit: Int = 8
    ): List<UpcomingIntake> {
        val pending = medications.flatMap { medication ->
            schedulesByMedication[medication.id].orEmpty().mapNotNull { schedule ->
                val trigger = ReminderPlanFactory.from(schedule, dayPeriodSettings).nextTriggerAfter(now)
                    ?: return@mapNotNull null
                if (trigger <= now) return@mapNotNull null
                UpcomingIntake(medication, schedule, trigger)
            }
        }
        return pending.sortedBy { it.triggerAtMillis }.take(limit)
    }
}
