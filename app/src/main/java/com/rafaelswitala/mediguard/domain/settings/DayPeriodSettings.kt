package com.rafaelswitala.mediguard.domain.settings

/**
 * Datei für Tageszeit-Einstellungen und deren Validierung.
 * Wichtig: Nur die zentrale Aufgabe dieser Komponente ist hier kommentiert.
 */

import com.rafaelswitala.mediguard.domain.model.DayPeriod
import org.json.JSONObject

data class TimeRangeMinutes(
    val startMinutes: Int,
    val endMinutes: Int
) {
    fun contains(minutesOfDay: Int): Boolean {
        val normalized = ((minutesOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
        return if (startMinutes <= endMinutes) {
            normalized in startMinutes..endMinutes
        } else {
            normalized >= startMinutes || normalized <= endMinutes
        }
    }

    fun preferredHour(): Int = startMinutes / 60
}

data class DayPeriodSettings(
    val morning: TimeRangeMinutes = TimeRangeMinutes(5 * 60, 10 * 60 + 59),
    val noon: TimeRangeMinutes = TimeRangeMinutes(11 * 60, 13 * 60 + 59),
    val afternoon: TimeRangeMinutes = TimeRangeMinutes(14 * 60, 17 * 60 + 59),
    val evening: TimeRangeMinutes = TimeRangeMinutes(18 * 60, 21 * 60 + 59),
    val night: TimeRangeMinutes = TimeRangeMinutes(22 * 60, 4 * 60 + 59)
) {
    fun rangeFor(period: DayPeriod): TimeRangeMinutes = when (period) {
        DayPeriod.MORNING -> morning
        DayPeriod.NOON -> noon
        DayPeriod.AFTERNOON -> afternoon
        DayPeriod.EVENING -> evening
        DayPeriod.NIGHT -> night
    }

    fun periodForMinutes(minutesOfDay: Int): DayPeriod {
        val order = listOf(
            DayPeriod.MORNING,
            DayPeriod.NOON,
            DayPeriod.AFTERNOON,
            DayPeriod.EVENING,
            DayPeriod.NIGHT
        )
        return order.firstOrNull { rangeFor(it).contains(minutesOfDay) } ?: DayPeriod.MORNING
    }

    fun validate(): DayPeriodValidationResult {
        val ranges = listOf(
            DayPeriod.MORNING to morning,
            DayPeriod.NOON to noon,
            DayPeriod.AFTERNOON to afternoon,
            DayPeriod.EVENING to evening,
            DayPeriod.NIGHT to night
        )
        val covered = BooleanArray(24 * 60)
        ranges.forEach { (_, range) ->
            markCovered(covered, range)
        }
        val gaps = mutableListOf<String>()
        var startGap: Int? = null
        for (minute in 0 until 24 * 60) {
            if (!covered[minute]) {
                if (startGap == null) startGap = minute
            } else if (startGap != null) {
                gaps.add("${formatMinute(startGap)}-${formatMinute(minute - 1)}")
                startGap = null
            }
        }
        if (startGap != null) {
            gaps.add("${formatMinute(startGap)}-${formatMinute(23 * 60 + 59)}")
        }
        return DayPeriodValidationResult(isValid = gaps.isEmpty(), gaps = gaps)
    }

    fun toJson(): String = JSONObject()
        .put("morningStart", morning.startMinutes)
        .put("morningEnd", morning.endMinutes)
        .put("noonStart", noon.startMinutes)
        .put("noonEnd", noon.endMinutes)
        .put("afternoonStart", afternoon.startMinutes)
        .put("afternoonEnd", afternoon.endMinutes)
        .put("eveningStart", evening.startMinutes)
        .put("eveningEnd", evening.endMinutes)
        .put("nightStart", night.startMinutes)
        .put("nightEnd", night.endMinutes)
        .toString()

    companion object {
        val Default = DayPeriodSettings()

        fun fromJson(raw: String?): DayPeriodSettings {
            if (raw.isNullOrBlank()) return Default
            return runCatching {
                val json = JSONObject(raw)
                DayPeriodSettings(
                    morning = TimeRangeMinutes(
                        json.optInt("morningStart", Default.morning.startMinutes),
                        json.optInt("morningEnd", Default.morning.endMinutes)
                    ),
                    noon = TimeRangeMinutes(
                        json.optInt("noonStart", Default.noon.startMinutes),
                        json.optInt("noonEnd", Default.noon.endMinutes)
                    ),
                    afternoon = TimeRangeMinutes(
                        json.optInt("afternoonStart", Default.afternoon.startMinutes),
                        json.optInt("afternoonEnd", Default.afternoon.endMinutes)
                    ),
                    evening = TimeRangeMinutes(
                        json.optInt("eveningStart", Default.evening.startMinutes),
                        json.optInt("eveningEnd", Default.evening.endMinutes)
                    ),
                    night = TimeRangeMinutes(
                        json.optInt("nightStart", Default.night.startMinutes),
                        json.optInt("nightEnd", Default.night.endMinutes)
                    )
                )
            }.getOrDefault(Default)
        }

        private fun markCovered(covered: BooleanArray, range: TimeRangeMinutes) {
            if (range.startMinutes <= range.endMinutes) {
                for (minute in range.startMinutes..range.endMinutes) {
                    covered[minute % (24 * 60)] = true
                }
            } else {
                for (minute in range.startMinutes until 24 * 60) {
                    covered[minute] = true
                }
                for (minute in 0..range.endMinutes) {
                    covered[minute] = true
                }
            }
        }

        private fun formatMinute(minute: Int): String {
            val hour = minute / 60
            val min = minute % 60
            return "${hour.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}"
        }
    }
}

data class DayPeriodValidationResult(
    val isValid: Boolean,
    val gaps: List<String>
)
