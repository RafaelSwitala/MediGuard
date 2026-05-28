package com.rafaelswitala.mediguard.domain.model

/**
 * Verschiedene Erinnerungsarten: täglich, zeitlich begrenzt, nach Tageszeit oder Intervall.
 * Mit JSON-Codec für die Serialisierung der jeweiligen Zeitparameter.
 */

import com.rafaelswitala.mediguard.data.settings.AppLanguage
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import org.json.JSONObject

enum class FrequencyType {
    EXACT_TIME,
    TIME_RANGE,
    DAY_PERIOD,
    INTERVAL,
    WEEKLY;

    override fun toString(): String = when (this) {
        EXACT_TIME -> "Exact Time"
        TIME_RANGE -> "Time Range"
        DAY_PERIOD -> "Daily Period"
        INTERVAL -> "Interval"
        WEEKLY -> "Weekly"
    }
}

enum class DayPeriod {
    MORNING, NOON, AFTERNOON, EVENING, NIGHT;

    fun preferredHour(settings: DayPeriodSettings = DayPeriodSettings.Default): Int =
        settings.rangeFor(this).preferredHour()

    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.DE -> when (this) {
            MORNING -> "Morgens"
            NOON -> "Mittags"
            AFTERNOON -> "Nachmittags"
            EVENING -> "Abends"
            NIGHT -> "Nachts"
        }
        AppLanguage.EN -> when (this) {
            MORNING -> "Morning"
            NOON -> "Noon"
            AFTERNOON -> "Afternoon"
            EVENING -> "Evening"
            NIGHT -> "Night"
        }
    }

    override fun toString(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class DayOfWeekEnum(val dayNumber: Int) {
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4),
    FRIDAY(5), SATURDAY(6), SUNDAY(7);

    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.DE -> when (this) {
            MONDAY -> "Montag"
            TUESDAY -> "Dienstag"
            WEDNESDAY -> "Mittwoch"
            THURSDAY -> "Donnerstag"
            FRIDAY -> "Freitag"
            SATURDAY -> "Samstag"
            SUNDAY -> "Sonntag"
        }
        AppLanguage.EN -> when (this) {
            MONDAY -> "Monday"
            TUESDAY -> "Tuesday"
            WEDNESDAY -> "Wednesday"
            THURSDAY -> "Thursday"
            FRIDAY -> "Friday"
            SATURDAY -> "Saturday"
            SUNDAY -> "Sunday"
        }
    }

    override fun toString(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

object ScheduleDataCodec {
    fun exactTime(hour: Int, minute: Int): String = JSONObject()
        .put("hour", hour)
        .put("minute", minute)
        .toString()

    fun timeRange(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String =
        JSONObject()
            .put("startHour", startHour)
            .put("startMinute", startMinute)
            .put("endHour", endHour)
            .put("endMinute", endMinute)
            .toString()

    fun dayPeriod(period: DayPeriod): String = JSONObject()
        .put("period", period.name)
        .toString()

    fun interval(intervalHours: Int, startHour: Int, startMinute: Int): String = JSONObject()
        .put("intervalHours", intervalHours)
        .put("startHour", startHour)
        .put("startMinute", startMinute)
        .toString()

    fun weekly(daysOfWeek: List<DayOfWeekEnum>, hour: Int, minute: Int): String = JSONObject()
        .put("daysOfWeek", daysOfWeek.joinToString(",") { it.name })
        .put("hour", hour)
        .put("minute", minute)
        .toString()

    fun readInt(data: String, key: String, defaultValue: Int): Int =
        runCatching { JSONObject(data).optInt(key, defaultValue) }.getOrDefault(defaultValue)

    fun readPeriod(data: String): DayPeriod =
        runCatching {
            DayPeriod.valueOf(JSONObject(data).optString("period", DayPeriod.MORNING.name))
        }.getOrDefault(DayPeriod.MORNING)

    fun readDays(data: String): List<DayOfWeekEnum> =
        runCatching {
            JSONObject(data)
                .optString("daysOfWeek", DayOfWeekEnum.MONDAY.name)
                .split(",")
                .mapNotNull { value ->
                    runCatching { DayOfWeekEnum.valueOf(value.trim()) }.getOrNull()
                }
                .ifEmpty { listOf(DayOfWeekEnum.MONDAY) }
        }.getOrDefault(listOf(DayOfWeekEnum.MONDAY))
}
