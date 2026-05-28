package com.rafaelswitala.mediguard.alarm

/**
 * Konstanten für Alarm-Intent-Extras (Zeitplan-IDs, Benachrichtigungs-IDs, History-IDs).
 */

object ReminderActions {
    const val ACTION_RECURRING_ALARM = "com.rafaelswitala.mediguard.action.RECURRING_ALARM"
    const val ACTION_ONE_TIME_REMINDER = "com.rafaelswitala.mediguard.action.ONE_TIME_REMINDER"
    const val ACTION_CONFIRM_INTAKE = "com.rafaelswitala.mediguard.action.CONFIRM_INTAKE"
    const val ACTION_SNOOZE = "com.rafaelswitala.mediguard.action.SNOOZE"
    const val ACTION_CHANGE_TIME = "com.rafaelswitala.mediguard.action.CHANGE_TIME"

    const val EXTRA_SCHEDULE_IDS = "schedule_ids"
    const val EXTRA_HISTORY_IDS = "history_ids"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_SCHEDULED_TIME = "scheduled_time"

    fun idsToCsv(ids: List<Long>): String = ids.joinToString(",")

    fun idsFromCsv(value: String?): List<Long> =
        value.orEmpty()
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
}
