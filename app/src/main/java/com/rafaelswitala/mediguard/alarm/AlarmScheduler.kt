package com.rafaelswitala.mediguard.alarm

/**
 * Schnittstelle für die Planung und Verwaltung von Weckrufen.
 */

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.data.settings.DayPeriodSettingsProvider
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.reminder.ReminderPlanFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AlarmScheduler {
    suspend fun scheduleAlarm(medicationSchedule: MedicationSchedule)
    suspend fun scheduleStoredAlarm(
        alarmData: DirectBootAlarmStore.AlarmScheduleData,
        allowImmediateIfOverdue: Boolean = false
    )
    fun scheduleOneTimeReminder(
        triggerAtMillis: Long,
        scheduleIds: List<Long>,
        historyIds: List<Long>,
        notificationId: Int
    )
    fun cancelOneTimeReminder(notificationId: Int)
    suspend fun cancelAlarm(scheduleId: Long)
    fun calculateNextAlarmTime(schedule: MedicationSchedule): Long?
}

/**
 * Battery-efficient scheduler based on exact one-shot AlarmManager alarms.
 */
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directBootAlarmStore: DirectBootAlarmStore,
    private val dayPeriodSettingsProvider: DayPeriodSettingsProvider
) : AlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleAlarm(medicationSchedule: MedicationSchedule) {
        if (!medicationSchedule.isEnabled) return

        val nextAlarmTime = calculateNextAlarmTime(medicationSchedule) ?: return
        schedulePendingIntent(
            scheduleId = medicationSchedule.id,
            medicationId = medicationSchedule.medicationId,
            triggerAtMillis = nextAlarmTime
        )
        directBootAlarmStore.saveAlarmSchedule(
            scheduleId = medicationSchedule.id,
            medicationId = medicationSchedule.medicationId,
            nextTriggerAtMillis = nextAlarmTime,
            scheduleType = medicationSchedule.scheduleType.name,
            scheduleData = medicationSchedule.scheduleData
        )
    }

    override suspend fun scheduleStoredAlarm(
        alarmData: DirectBootAlarmStore.AlarmScheduleData,
        allowImmediateIfOverdue: Boolean
    ) {
        val scheduleType = runCatching {
            FrequencyType.valueOf(alarmData.scheduleType)
        }.getOrDefault(FrequencyType.EXACT_TIME)

        val schedule = MedicationSchedule(
            id = alarmData.scheduleId,
            medicationId = alarmData.medicationId,
            scheduleType = scheduleType,
            scheduleData = alarmData.scheduleData
        )

        val now = System.currentTimeMillis()
        val nextAlarmTime = when {
            alarmData.nextTriggerAtMillis > now -> alarmData.nextTriggerAtMillis
            allowImmediateIfOverdue && now - alarmData.nextTriggerAtMillis <= OVERDUE_GRACE_MS -> now + 1_000L
            else -> calculateNextAlarmTime(schedule) ?: return
        }

        schedulePendingIntent(
            scheduleId = alarmData.scheduleId,
            medicationId = alarmData.medicationId,
            triggerAtMillis = nextAlarmTime
        )

        directBootAlarmStore.saveAlarmSchedule(
            scheduleId = alarmData.scheduleId,
            medicationId = alarmData.medicationId,
            nextTriggerAtMillis = nextAlarmTime,
            scheduleType = alarmData.scheduleType,
            scheduleData = alarmData.scheduleData
        )
    }

    override fun scheduleOneTimeReminder(
        triggerAtMillis: Long,
        scheduleIds: List<Long>,
        historyIds: List<Long>,
        notificationId: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderActions.ACTION_ONE_TIME_REMINDER
            putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, ReminderActions.idsToCsv(scheduleIds))
            putExtra(ReminderActions.EXTRA_HISTORY_IDS, ReminderActions.idsToCsv(historyIds))
            putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, triggerAtMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            oneTimeRequestCode(notificationId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(triggerAtMillis, pendingIntent, notificationId)
    }

    override fun cancelOneTimeReminder(notificationId: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            oneTimeRequestCode(notificationId),
            Intent(context, AlarmReceiver::class.java).apply {
                action = ReminderActions.ACTION_ONE_TIME_REMINDER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override suspend fun cancelAlarm(scheduleId: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        directBootAlarmStore.removeAlarmSchedule(scheduleId)
    }

    override fun calculateNextAlarmTime(schedule: MedicationSchedule): Long? =
        ReminderPlanFactory.from(schedule, dayPeriodSettingsProvider.current()).nextTriggerAfter()

    private fun schedulePendingIntent(
        scheduleId: Long,
        medicationId: Long,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderActions.ACTION_RECURRING_ALARM
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_SCHEDULED_TIME, triggerAtMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(triggerAtMillis, pendingIntent, scheduleId.toInt())
    }

    private fun scheduleExact(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        notificationId: Int
    ) {
        val showIntent = PendingIntent.getActivity(
            context,
            oneTimeRequestCode(notificationId) + SHOW_INTENT_OFFSET,
            Intent(context, com.rafaelswitala.mediguard.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (securityException: SecurityException) {
            runCatching {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    pendingIntent
                )
            }.getOrElse {
                AlarmManagerCompat.setAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun oneTimeRequestCode(notificationId: Int): Int =
        ONE_TIME_REQUEST_CODE_OFFSET + (notificationId and 0x00FFFFFF)

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        private const val ONE_TIME_REQUEST_CODE_OFFSET = 100_000
        private const val SHOW_INTENT_OFFSET = 30_000
        private const val OVERDUE_GRACE_MS = 12L * 60L * 60L * 1000L
    }
}
