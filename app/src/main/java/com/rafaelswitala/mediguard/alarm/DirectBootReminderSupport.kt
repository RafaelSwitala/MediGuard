package com.rafaelswitala.mediguard.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import com.rafaelswitala.mediguard.MainActivity
import com.rafaelswitala.mediguard.R
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.data.settings.AppAlertMode
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.reminder.ReminderPlanFactory
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings

object DirectBootAlarmScheduler {
    suspend fun scheduleStoredAlarm(
        context: Context,
        directBootAlarmStore: DirectBootAlarmStore,
        alarmData: DirectBootAlarmStore.AlarmScheduleData,
        allowImmediateIfOverdue: Boolean = false
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
            else -> ReminderPlanFactory.from(
                schedule,
                DayPeriodSettings.Default
            ).nextTriggerAfter(now) ?: return
        }

        scheduleExact(
            context = context,
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

    private fun scheduleExact(
        context: Context,
        scheduleId: Long,
        medicationId: Long,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderActions.ACTION_RECURRING_ALARM
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME, triggerAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            context,
            oneTimeRequestCode(scheduleId.toInt()) + SHOW_INTENT_OFFSET,
            Intent(context, MainActivity::class.java),
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

    private const val ONE_TIME_REQUEST_CODE_OFFSET = 100_000
    private const val SHOW_INTENT_OFFSET = 30_000
    private const val OVERDUE_GRACE_MS = 12L * 60L * 60L * 1000L
}

object DirectBootReminderNotifier {
    fun showPrivacyReminder(
        context: Context,
        directBootAlarmStore: DirectBootAlarmStore,
        notificationId: Int,
        scheduledTime: Long
    ) {
        val settings = directBootAlarmStore.readAlertSettings()
        val title = context.getString(R.string.medication_reminder)

        postPrivacyNotification(
            context = context,
            notificationId = notificationId,
            alertMode = settings.alertMode,
            ringtoneUri = settings.ringtoneUri,
            title = title
        )

        if (settings.alertMode == AppAlertMode.ALARM) {
            AlarmRingingService.start(
                context = context,
                notificationId = notificationId,
                ringtoneUri = soundUriFor(context, settings.ringtoneUri).toString(),
                title = title,
                message = "",
                scheduleIds = emptyList(),
                historyIds = emptyList(),
                scheduledTime = scheduledTime,
                takenLabel = "Taken",
                snoozeLabel = "Snooze",
                changeTimeLabel = "Change time",
                stopLabel = "Stop alarm"
            )
        }
    }

    private fun postPrivacyNotification(
        context: Context,
        notificationId: Int,
        alertMode: AppAlertMode,
        ringtoneUri: String?,
        title: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = channelIdFor(context, notificationManager, alertMode, ringtoneUri)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(getColorCompat(context, R.color.primary))
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            when (alertMode) {
                AppAlertMode.SOUND_NOTIFICATION -> builder.setSound(soundUriFor(context, ringtoneUri))
                AppAlertMode.SILENT_NOTIFICATION,
                AppAlertMode.ALARM -> builder.setSilent(true)
            }
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun channelIdFor(
        context: Context,
        notificationManager: NotificationManager,
        alertMode: AppAlertMode,
        ringtoneUri: String?
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return CHANNEL_ID_LOCKED_SILENT

        return when (alertMode) {
            AppAlertMode.SILENT_NOTIFICATION,
            AppAlertMode.ALARM -> {
                val channel = NotificationChannel(
                    CHANNEL_ID_LOCKED_SILENT,
                    "Medication reminders before unlock",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                CHANNEL_ID_LOCKED_SILENT
            }
            AppAlertMode.SOUND_NOTIFICATION -> {
                val soundUri = soundUriFor(context, ringtoneUri)
                val channelId = "${CHANNEL_ID_LOCKED_SOUND}_${soundUri.hashCode().toString().replace("-", "n")}"
                val channel = NotificationChannel(
                    channelId,
                    "Medication reminders before unlock with sound",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setShowBadge(true)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                notificationManager.createNotificationChannel(channel)
                channelId
            }
        }
    }

    private fun soundUriFor(context: Context, ringtoneUri: String?): Uri =
        ringtoneUri
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Uri.EMPTY

    private fun getColorCompat(context: Context, colorRes: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(colorRes)
        } else {
            @Suppress("DEPRECATION")
            context.resources.getColor(colorRes)
        }

    private const val CHANNEL_ID_LOCKED_SILENT = "medication_locked_direct_silent"
    private const val CHANNEL_ID_LOCKED_SOUND = "medication_locked_direct_sound"
}
