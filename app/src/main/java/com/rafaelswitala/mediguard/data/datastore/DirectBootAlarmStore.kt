package com.rafaelswitala.mediguard.data.datastore

import android.content.Context
import android.content.SharedPreferences
import com.rafaelswitala.mediguard.data.settings.AppAlertMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Device Encrypted alarm store.
 *
 * This deliberately uses device-protected SharedPreferences instead of
 * DataStore, because the locked-boot path must not touch anything that can
 * resolve back to credential-encrypted application storage.
 */
class DirectBootAlarmStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = preferencesFor(context)

    suspend fun saveAlarmSchedule(
        scheduleId: Long,
        medicationId: Long,
        nextTriggerAtMillis: Long,
        scheduleType: String,
        scheduleData: String
    ) {
        preferences.edit()
            .putStringSet(ALARM_IDS, preferences.alarmIds() + scheduleId.toString())
            .putLong(scheduleIdKey(scheduleId), scheduleId)
            .putLong(medicationIdKey(scheduleId), medicationId)
            .putLong(nextTriggerKey(scheduleId), nextTriggerAtMillis)
            .putString(scheduleTypeKey(scheduleId), scheduleType)
            .putString(scheduleDataKey(scheduleId), scheduleData)
            .putLong(lastUpdatedKey(scheduleId), System.currentTimeMillis())
            .commit()
    }

    suspend fun removeAlarmSchedule(scheduleId: Long) {
        preferences.edit()
            .putStringSet(
                ALARM_IDS,
                preferences.alarmIds().filterNot { it == scheduleId.toString() }.toSet()
            )
            .remove(scheduleIdKey(scheduleId))
            .remove(medicationIdKey(scheduleId))
            .remove(nextTriggerKey(scheduleId))
            .remove(scheduleTypeKey(scheduleId))
            .remove(scheduleDataKey(scheduleId))
            .remove(lastUpdatedKey(scheduleId))
            .remove(snoozeKey(scheduleId))
            .commit()
    }

    suspend fun markLockedBootCompleted() {
        preferences.edit()
            .putLong(LAST_LOCKED_BOOT_COMPLETED_AT, System.currentTimeMillis())
            .commit()
    }

    suspend fun savePendingLockedAlarm(
        notificationId: Long,
        scheduledTime: Long,
        scheduleIds: List<Long>
    ) {
        if (scheduleIds.isEmpty()) return

        preferences.edit()
            .putStringSet(
                PENDING_LOCKED_NOTIFICATION_IDS,
                preferences.pendingLockedNotificationIds() + notificationId.toString()
            )
            .putLong(pendingLockedScheduledTimeKey(notificationId), scheduledTime)
            .putString(pendingLockedScheduleIdsKey(notificationId), scheduleIds.joinToString(","))
            .commit()
    }

    suspend fun removePendingLockedAlarm(notificationId: Long) {
        preferences.edit()
            .putStringSet(
                PENDING_LOCKED_NOTIFICATION_IDS,
                preferences.pendingLockedNotificationIds().filterNot { it == notificationId.toString() }.toSet()
            )
            .remove(pendingLockedScheduledTimeKey(notificationId))
            .remove(pendingLockedScheduleIdsKey(notificationId))
            .commit()
    }

    suspend fun saveAlertMode(alertMode: AppAlertMode) {
        preferences.edit()
            .putString(ALERT_MODE, alertMode.name)
            .commit()
    }

    suspend fun saveRingtone(uri: String?) {
        preferences.edit().apply {
            if (uri == null) {
                remove(RINGTONE_URI)
            } else {
                putString(RINGTONE_URI, uri)
            }
        }.commit()
    }

    fun readAlertSettings(): DirectBootAlertSettings =
        DirectBootAlertSettings(
            alertMode = AppAlertMode.fromStoredValue(preferences.getString(ALERT_MODE, null)),
            ringtoneUri = preferences.getString(RINGTONE_URI, null)
        )

    fun getAllAlarmSchedules(): Flow<List<AlarmScheduleData>> = flow {
        emit(readAllAlarmSchedules())
    }

    fun getPendingLockedAlarms(): Flow<List<PendingLockedAlarmData>> = flow {
        emit(readPendingLockedAlarms())
    }

    suspend fun getAlarmSchedule(scheduleId: Long): AlarmScheduleData? =
        preferences.toAlarmScheduleData(scheduleId)

    suspend fun saveSnoozeDuration(scheduleId: Long, snoozeMinutes: Int) {
        preferences.edit()
            .putInt(snoozeKey(scheduleId), snoozeMinutes)
            .commit()
    }

    fun getSnoozeDuration(scheduleId: Long): Flow<Int> = flow {
        emit(preferences.getInt(snoozeKey(scheduleId), 0))
    }

    private fun readAllAlarmSchedules(): List<AlarmScheduleData> =
        preferences.alarmIds()
            .mapNotNull { it.toLongOrNull() }
            .mapNotNull { scheduleId -> preferences.toAlarmScheduleData(scheduleId) }
            .sortedBy { it.nextTriggerAtMillis }

    private fun readPendingLockedAlarms(): List<PendingLockedAlarmData> =
        preferences.pendingLockedNotificationIds()
            .mapNotNull { it.toLongOrNull() }
            .mapNotNull { notificationId ->
                val scheduledTime = preferences.getLong(
                    pendingLockedScheduledTimeKey(notificationId),
                    0L
                ).takeIf { it > 0L } ?: return@mapNotNull null
                val scheduleIds = preferences.getString(
                    pendingLockedScheduleIdsKey(notificationId),
                    null
                ).orEmpty()
                    .split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                if (scheduleIds.isEmpty()) return@mapNotNull null
                PendingLockedAlarmData(
                    notificationId = notificationId,
                    scheduledTime = scheduledTime,
                    scheduleIds = scheduleIds
                )
            }
            .sortedBy { it.scheduledTime }

    private fun SharedPreferences.toAlarmScheduleData(scheduleId: Long): AlarmScheduleData? {
        if (!contains(scheduleIdKey(scheduleId))) return null
        val medicationId = getLong(medicationIdKey(scheduleId), -1L).takeIf { it > 0L } ?: return null
        val nextTrigger = getLong(nextTriggerKey(scheduleId), -1L).takeIf { it > 0L } ?: return null
        val scheduleType = getString(scheduleTypeKey(scheduleId), null) ?: return null
        val scheduleData = getString(scheduleDataKey(scheduleId), null) ?: return null

        return AlarmScheduleData(
            scheduleId = getLong(scheduleIdKey(scheduleId), scheduleId),
            medicationId = medicationId,
            nextTriggerAtMillis = nextTrigger,
            scheduleType = scheduleType,
            scheduleData = scheduleData,
            lastUpdated = getLong(lastUpdatedKey(scheduleId), 0L)
        )
    }

    data class AlarmScheduleData(
        val scheduleId: Long,
        val medicationId: Long,
        val nextTriggerAtMillis: Long,
        val scheduleType: String,
        val scheduleData: String,
        val lastUpdated: Long
    )

    data class PendingLockedAlarmData(
        val notificationId: Long,
        val scheduledTime: Long,
        val scheduleIds: List<Long>
    )

    data class DirectBootAlertSettings(
        val alertMode: AppAlertMode,
        val ringtoneUri: String?
    )

    companion object {
        private const val PREFERENCES_NAME = "direct_boot_alarms"
        private const val ALARM_IDS = "alarm_ids"
        private const val PENDING_LOCKED_NOTIFICATION_IDS = "pending_locked_notification_ids"
        private const val LAST_LOCKED_BOOT_COMPLETED_AT = "last_locked_boot_completed_at"
        private const val ALERT_MODE = "alert_mode"
        private const val RINGTONE_URI = "ringtone_uri"

        fun preferencesFor(context: Context): SharedPreferences =
            context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        private fun SharedPreferences.alarmIds(): Set<String> =
            getStringSet(ALARM_IDS, emptySet()).orEmpty()

        private fun SharedPreferences.pendingLockedNotificationIds(): Set<String> =
            getStringSet(PENDING_LOCKED_NOTIFICATION_IDS, emptySet()).orEmpty()

        private fun scheduleIdKey(scheduleId: Long) = "schedule_id_$scheduleId"
        private fun medicationIdKey(scheduleId: Long) = "med_id_$scheduleId"
        private fun nextTriggerKey(scheduleId: Long) = "next_trigger_$scheduleId"
        private fun scheduleTypeKey(scheduleId: Long) = "schedule_type_$scheduleId"
        private fun scheduleDataKey(scheduleId: Long) = "schedule_data_$scheduleId"
        private fun lastUpdatedKey(scheduleId: Long) = "updated_$scheduleId"
        private fun snoozeKey(scheduleId: Long) = "snooze_$scheduleId"
        private fun pendingLockedScheduledTimeKey(notificationId: Long) =
            "pending_locked_scheduled_$notificationId"
        private fun pendingLockedScheduleIdsKey(notificationId: Long) =
            "pending_locked_schedule_ids_$notificationId"
    }
}
