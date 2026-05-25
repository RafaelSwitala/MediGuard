package com.rafaelswitala.mediguard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.UserManager
import androidx.core.app.NotificationCompat
import com.rafaelswitala.mediguard.MainActivity
import com.rafaelswitala.mediguard.R
import com.rafaelswitala.mediguard.alarm.AlarmRingingService
import com.rafaelswitala.mediguard.alarm.ReminderActions
import com.rafaelswitala.mediguard.alarm.RescheduleReminderActivity
import com.rafaelswitala.mediguard.alarm.NotificationActionReceiver
import com.rafaelswitala.mediguard.data.settings.AppAlertMode
import com.rafaelswitala.mediguard.data.settings.AppPreferencesRepository
import com.rafaelswitala.mediguard.data.settings.AppSettings
import com.rafaelswitala.mediguard.ui.localization.AppStrings
import com.rafaelswitala.mediguard.ui.localization.stringsFor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ReminderNotificationItem(
    val medicationId: Long,
    val historyId: Long?,
    val medicationName: String,
    val dosage: String,
    val dosageUnit: String
)

/**
 * Manages medication notifications.
 * Notifications are grouped by due time and expose all user actions directly.
 */
class MedicationNotificationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val appPreferencesRepository: AppPreferencesRepository
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createBaseNotificationChannels()
    }

    private fun createBaseNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val lockedChannel = NotificationChannel(
            CHANNEL_ID_LOCKED,
            "Medication Reminders (Locked)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Privacy-safe medication reminders"
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(lockedChannel)

        val unlockedChannel = NotificationChannel(
            CHANNEL_ID_UNLOCKED,
            "Medication Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Medication reminders with direct actions"
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(unlockedChannel)
    }

    suspend fun showMedicationNotification(
        medicationId: Long,
        scheduleId: Long,
        medicationName: String,
        dosage: String,
        dosageUnit: String,
        historyId: Long? = null
    ) {
        showMedicationGroupNotification(
            scheduleIds = listOf(scheduleId),
            scheduledTime = System.currentTimeMillis(),
            items = listOf(
                ReminderNotificationItem(
                    medicationId = medicationId,
                    historyId = historyId,
                    medicationName = medicationName,
                    dosage = dosage,
                    dosageUnit = dosageUnit
                )
            ),
            notificationId = scheduleId.toInt()
        )
    }

    suspend fun showMedicationGroupNotification(
        scheduleIds: List<Long>,
        scheduledTime: Long,
        items: List<ReminderNotificationItem>,
        notificationId: Int = scheduleIds.minOrNull()?.toInt() ?: DEFAULT_NOTIFICATION_ID
    ) {
        if (items.isEmpty()) return

        val settings = readSettings()
        val strings = stringsFor(settings.language)
        val channelId = channelIdFor(settings)
        val builder = createUnlockedNotification(
            channelId = channelId,
            scheduleIds = scheduleIds,
            scheduledTime = scheduledTime,
            items = items,
            notificationId = notificationId,
            strings = strings,
            alertMode = settings.alertMode
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            when (settings.alertMode) {
                AppAlertMode.SILENT_NOTIFICATION,
                AppAlertMode.ALARM -> builder.setSilent(true)
                AppAlertMode.SOUND_NOTIFICATION -> builder.setSound(soundUriFor(settings))
            }
        }

        if (settings.alertMode == AppAlertMode.ALARM) {
            AlarmRingingService.start(
                context = context,
                notificationId = notificationId,
                ringtoneUri = soundUriFor(settings).toString(),
                title = if (items.size == 1) {
                    strings.notificationMedicationReminder
                } else {
                    strings.notificationMedicationGroupReminder
                },
                message = notificationMessage(items, strings),
                scheduleIds = scheduleIds,
                historyIds = items.mapNotNull { it.historyId },
                scheduledTime = scheduledTime,
                takenLabel = strings.taken,
                snoozeLabel = strings.snooze,
                changeTimeLabel = strings.changeTimeOnce,
                stopLabel = strings.stopAlarm
            )
        } else {
            notificationManager.notify(notificationId, builder.build())
        }
    }

    suspend fun showPrivacyMedicationNotification(notificationId: Long) {
        val settings = readSettings()
        val strings = stringsFor(settings.language)
        val notification = createLockedNotification(notificationId, strings, settings.alertMode).build()
        if (settings.alertMode == AppAlertMode.ALARM) {
            AlarmRingingService.start(
                context = context,
                notificationId = notificationId.toInt(),
                ringtoneUri = soundUriFor(settings).toString(),
                title = strings.notificationMedicationReminder,
                message = "",
                scheduleIds = emptyList(),
                historyIds = emptyList(),
                scheduledTime = System.currentTimeMillis(),
                takenLabel = strings.taken,
                snoozeLabel = strings.snooze,
                changeTimeLabel = strings.changeTimeOnce,
                stopLabel = strings.stopAlarm
            )
        } else {
            notificationManager.notify(notificationId.toInt(), notification)
        }
    }

    private fun createLockedNotification(
        notificationId: Long,
        strings: AppStrings,
        alertMode: AppAlertMode
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("notification_id", notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_LOCKED)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(context.getColorCompat(R.color.primary))
            .setContentTitle(strings.notificationMedicationReminder)
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(alertMode != AppAlertMode.ALARM)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    private fun createUnlockedNotification(
        channelId: String,
        scheduleIds: List<Long>,
        scheduledTime: Long,
        items: List<ReminderNotificationItem>,
        notificationId: Int,
        strings: AppStrings,
        alertMode: AppAlertMode
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, ReminderActions.idsToCsv(scheduleIds))
            putExtra(
                ReminderActions.EXTRA_HISTORY_IDS,
                ReminderActions.idsToCsv(items.mapNotNull { it.historyId })
            )
            putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, scheduledTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode(notificationId, REQUEST_CONTENT),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (items.size == 1) {
            strings.notificationMedicationReminder
        } else {
            strings.notificationMedicationGroupReminder
        }
        val message = notificationMessage(items, strings)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(context.getColorCompat(R.color.primary))
            .setContentTitle(title)
            .setContentText(message.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (alertMode == AppAlertMode.ALARM) {
            builder
                .addAction(
                    R.drawable.ic_snooze,
                    strings.snooze,
                    actionBroadcastIntent(
                        action = ReminderActions.ACTION_SNOOZE,
                        scheduleIds = scheduleIds,
                        historyIds = items.mapNotNull { it.historyId },
                        scheduledTime = scheduledTime,
                        notificationId = notificationId,
                        requestOffset = REQUEST_SNOOZE
                    )
                )
                .addAction(
                    R.drawable.ic_schedule,
                    strings.changeTimeOnce,
                    changeTimeIntent(
                        scheduleIds = scheduleIds,
                        historyIds = items.mapNotNull { it.historyId },
                        scheduledTime = scheduledTime,
                        notificationId = notificationId
                    )
                )
        }

        return builder
            .addAction(
                R.drawable.ic_check,
                strings.taken,
                actionBroadcastIntent(
                    action = ReminderActions.ACTION_CONFIRM_INTAKE,
                    scheduleIds = scheduleIds,
                    historyIds = items.mapNotNull { it.historyId },
                    scheduledTime = scheduledTime,
                    notificationId = notificationId,
                    requestOffset = REQUEST_CONFIRM
                )
            )
    }

    private fun notificationMessage(
        items: List<ReminderNotificationItem>,
        strings: AppStrings
    ): String {
        if (items.size == 1) {
            val item = items.first()
            return "${strings.notificationPleaseTake} ${item.dosage} ${item.dosageUnit} ${item.medicationName}."
        }

        return buildString {
            append(strings.notificationMultipleIntro)
            append('\n')
            items.forEach { item ->
                append("- ")
                append(item.medicationName)
                append(": ")
                append(item.dosage)
                append(' ')
                append(item.dosageUnit)
                append('\n')
            }
        }.trim()
    }

    private fun actionBroadcastIntent(
        action: String,
        scheduleIds: List<Long>,
        historyIds: List<Long>,
        scheduledTime: Long,
        notificationId: Int,
        requestOffset: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, ReminderActions.idsToCsv(scheduleIds))
            putExtra(ReminderActions.EXTRA_HISTORY_IDS, ReminderActions.idsToCsv(historyIds))
            putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode(notificationId, requestOffset),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun changeTimeIntent(
        scheduleIds: List<Long>,
        historyIds: List<Long>,
        scheduledTime: Long,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, RescheduleReminderActivity::class.java).apply {
            action = ReminderActions.ACTION_CHANGE_TIME
            putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, ReminderActions.idsToCsv(scheduleIds))
            putExtra(ReminderActions.EXTRA_HISTORY_IDS, ReminderActions.idsToCsv(historyIds))
            putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, scheduledTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return PendingIntent.getActivity(
            context,
            requestCode(notificationId, REQUEST_CHANGE_TIME),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun readSettings(): AppSettings =
        runCatching { appPreferencesRepository.settings.first() }.getOrDefault(AppSettings())

    private fun channelIdFor(settings: AppSettings): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return CHANNEL_ID_UNLOCKED

        when (settings.alertMode) {
            AppAlertMode.SILENT_NOTIFICATION -> {
                val channel = NotificationChannel(
                    CHANNEL_ID_SILENT,
                    "Medication Reminders - Silent",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Medication reminders without sound"
                    setShowBadge(true)
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
                return CHANNEL_ID_SILENT
            }
            AppAlertMode.ALARM -> {
                val channel = NotificationChannel(
                    CHANNEL_ID_ALARM,
                    "Medication Reminders - Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Medication alarms controlled by the ringing service"
                    setShowBadge(true)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
                return CHANNEL_ID_ALARM
            }
            AppAlertMode.SOUND_NOTIFICATION -> Unit
        }

        val soundUri = soundUriFor(settings)
        val ringtoneUri = soundUri.toString()
        val channelId = "${CHANNEL_ID_SOUND}_${ringtoneUri.hashCode().toString().replace("-", "n")}"
        val channel = NotificationChannel(
            channelId,
            "Medication Reminders - ${settings.ringtoneTitle.orEmpty().ifBlank { "Notification sound" }}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Medication reminders with selected sound"
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
        return channelId
    }

    private fun soundUriFor(settings: AppSettings): Uri =
        settings.ringtoneUri
            ?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun isCredentialStorageUnlocked(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.isUserUnlocked
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelNotification(notificationId: Long) {
        notificationManager.cancel(notificationId.toInt())
    }

    private fun requestCode(notificationId: Int, offset: Int): Int =
        ((notificationId and 0x00FFFFFF) * 10) + offset

    private fun Context.getColorCompat(colorRes: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(colorRes)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(colorRes)
        }

    companion object {
        private const val CHANNEL_ID_LOCKED = "medication_locked"
        private const val CHANNEL_ID_UNLOCKED = "medication_unlocked"
        private const val CHANNEL_ID_SILENT = "medication_silent"
        private const val CHANNEL_ID_SOUND = "medication_sound"
        private const val CHANNEL_ID_ALARM = "medication_alarm"
        private const val DEFAULT_NOTIFICATION_ID = 42
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_SNOOZE = 2
        private const val REQUEST_CHANGE_TIME = 3
        private const val REQUEST_CONFIRM = 4
    }
}
