package com.rafaelswitala.mediguard.domain.reminder

import com.rafaelswitala.mediguard.domain.model.DayOfWeekEnum
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.model.ScheduleDataCodec
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import java.util.Calendar

abstract class BaseReminder(
    protected val schedule: MedicationSchedule,
    protected val dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
) {
    abstract fun nextTriggerAfter(now: Long = System.currentTimeMillis()): Long?

    protected fun todayAt(hour: Int, minute: Int, now: Long): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    protected fun nextDailyAt(hour: Int, minute: Int, now: Long): Long {
        val candidate = todayAt(hour, minute, now)
        if (candidate.timeInMillis <= now) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }
}

class TimedReminder(
    schedule: MedicationSchedule,
    private val hour: Int,
    private val minute: Int,
    dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
) : BaseReminder(schedule, dayPeriodSettings) {
    override fun nextTriggerAfter(now: Long): Long = nextDailyAt(hour, minute, now)
}

class TimeRangeReminder(
    schedule: MedicationSchedule,
    private val startHour: Int,
    private val startMinute: Int,
    dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
) : BaseReminder(schedule, dayPeriodSettings) {
    override fun nextTriggerAfter(now: Long): Long = nextDailyAt(startHour, startMinute, now)
}

class IntervalReminder(
    schedule: MedicationSchedule,
    private val intervalHours: Int,
    private val startHour: Int,
    private val startMinute: Int,
    dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
) : BaseReminder(schedule, dayPeriodSettings) {
    override fun nextTriggerAfter(now: Long): Long? {
        val intervalMillis = intervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        var candidate = todayAt(startHour, startMinute, now).timeInMillis

        while (candidate <= now) {
            candidate += intervalMillis
        }

        return candidate
    }
}

class WeeklyReminder(
    schedule: MedicationSchedule,
    private val daysOfWeek: List<DayOfWeekEnum>,
    private val hour: Int,
    private val minute: Int,
    dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
) : BaseReminder(schedule, dayPeriodSettings) {
    override fun nextTriggerAfter(now: Long): Long? {
        val selectedDays = daysOfWeek.map { it.toCalendarDay() }.toSet()

        for (dayOffset in 0..7) {
            val candidate = todayAt(hour, minute, now).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in selectedDays && candidate.timeInMillis > now) {
                return candidate.timeInMillis
            }
        }

        return null
    }

    private fun DayOfWeekEnum.toCalendarDay(): Int = when (this) {
        DayOfWeekEnum.MONDAY -> Calendar.MONDAY
        DayOfWeekEnum.TUESDAY -> Calendar.TUESDAY
        DayOfWeekEnum.WEDNESDAY -> Calendar.WEDNESDAY
        DayOfWeekEnum.THURSDAY -> Calendar.THURSDAY
        DayOfWeekEnum.FRIDAY -> Calendar.FRIDAY
        DayOfWeekEnum.SATURDAY -> Calendar.SATURDAY
        DayOfWeekEnum.SUNDAY -> Calendar.SUNDAY
    }
}

object ReminderPlanFactory {
    fun from(
        schedule: MedicationSchedule,
        dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
    ): BaseReminder = when (schedule.scheduleType) {
        FrequencyType.EXACT_TIME -> TimedReminder(
            schedule = schedule,
            hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8),
            minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0),
            dayPeriodSettings = dayPeriodSettings
        )

        FrequencyType.TIME_RANGE -> TimeRangeReminder(
            schedule = schedule,
            startHour = ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8),
            startMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0),
            dayPeriodSettings = dayPeriodSettings
        )

        FrequencyType.DAY_PERIOD -> TimedReminder(
            schedule = schedule,
            hour = ScheduleDataCodec.readPeriod(schedule.scheduleData).preferredHour(dayPeriodSettings),
            minute = 0,
            dayPeriodSettings = dayPeriodSettings
        )

        FrequencyType.INTERVAL -> IntervalReminder(
            schedule = schedule,
            intervalHours = ScheduleDataCodec.readInt(schedule.scheduleData, "intervalHours", 6),
            startHour = ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8),
            startMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0),
            dayPeriodSettings = dayPeriodSettings
        )

        FrequencyType.WEEKLY -> WeeklyReminder(
            schedule = schedule,
            daysOfWeek = ScheduleDataCodec.readDays(schedule.scheduleData),
            hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8),
            minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0),
            dayPeriodSettings = dayPeriodSettings
        )
    }
}
