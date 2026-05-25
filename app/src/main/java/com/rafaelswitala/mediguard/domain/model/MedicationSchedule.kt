package com.rafaelswitala.mediguard.domain.model

/**
 * Domain model for MedicationSchedule
 * Handles various scheduling strategies using polymorphism
 */
data class MedicationSchedule(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleType: FrequencyType,
    val scheduleData: String,  // JSON serialized based on schedule type
    val reminderMinutes: Int = 15,  // Minutes before to remind
    val snoozeOptions: List<Int> = listOf(5, 10, 15, 30, 60),  // Snooze options in minutes
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Detailed schedule models for different schedule types
 */
data class ExactTimeSchedule(
    val hour: Int,
    val minute: Int
)

data class TimeRangeSchedule(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

data class DayPeriodSchedule(
    val period: DayPeriod
)

data class WeeklySchedule(
    val daysOfWeek: List<DayOfWeekEnum>
)

data class IntervalSchedule(
    val intervalHours: Int,
    val startHour: Int,
    val startMinute: Int
)
