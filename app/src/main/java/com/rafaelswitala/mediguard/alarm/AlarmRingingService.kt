package com.rafaelswitala.mediguard.alarm

/**
 * Dienst mit Benachrichtigung und Audioausgabe, wenn eine Medikamentenerinnerung fällig ist.
 * Bietet Buttons zum Bestätigen, Verschieben oder zeitlich neu Planen.
 */

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rafaelswitala.mediguard.MainActivity
import com.rafaelswitala.mediguard.R

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)
            ?: DEFAULT_NOTIFICATION_ID
        startForeground(notificationId, buildNotification(intent, notificationId))
        startRinging(intent?.getStringExtra(EXTRA_RINGTONE_URI))
        return START_STICKY
    }

    override fun onDestroy() {
        stopRinging()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun startRinging(ringtoneUri: String?) {
        if (mediaPlayer?.isPlaying == true) return

        val candidates = listOfNotNull(
            ringtoneUri?.let { runCatching { Uri.parse(it) }.getOrNull() },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ).distinct()

        mediaPlayer = candidates.firstNotNullOfOrNull { uri ->
            runCatching {
                MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
        }
    }

    private fun stopRinging() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        mediaPlayer = null
    }

    private fun buildNotification(intent: Intent?, notificationId: Int): Notification {
        ensureChannel()

        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank {
            getString(R.string.medication_reminder)
        }
        val message = intent?.getStringExtra(EXTRA_MESSAGE).orEmpty().ifBlank {
            ""
        }
        val scheduleIds = intent?.getStringExtra(ReminderActions.EXTRA_SCHEDULE_IDS).orEmpty()
        val historyIds = intent?.getStringExtra(ReminderActions.EXTRA_HISTORY_IDS).orEmpty()
        val scheduledTime = intent?.getLongExtra(
            ReminderActions.EXTRA_SCHEDULED_TIME,
            System.currentTimeMillis()
        ) ?: System.currentTimeMillis()

        val contentIntent = PendingIntent.getActivity(
            this,
            requestCode(notificationId, REQUEST_CONTENT),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_ALARM_RINGING)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(getColorCompat(R.color.primary))
            .setContentTitle(title)
            .setContentText(message.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)

        if (historyIds.isBlank()) {
            builder.addAction(
                R.drawable.ic_check,
                intent?.getStringExtra(EXTRA_STOP_LABEL).orEmpty().ifBlank { "Stop" },
                PendingIntent.getService(
                    this,
                    requestCode(notificationId, REQUEST_STOP),
                    Intent(this, AlarmRingingService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            builder
                .addAction(
                    R.drawable.ic_snooze,
                    intent?.getStringExtra(EXTRA_SNOOZE_LABEL).orEmpty().ifBlank { "Snooze" },
                    actionBroadcastIntent(
                        action = ReminderActions.ACTION_SNOOZE,
                        scheduleIds = scheduleIds,
                        historyIds = historyIds,
                        scheduledTime = scheduledTime,
                        notificationId = notificationId,
                        requestOffset = REQUEST_SNOOZE
                    )
                )
                .addAction(
                    R.drawable.ic_schedule,
                    intent?.getStringExtra(EXTRA_CHANGE_TIME_LABEL).orEmpty().ifBlank { "Change time" },
                    changeTimeIntent(
                        scheduleIds = scheduleIds,
                        historyIds = historyIds,
                        scheduledTime = scheduledTime,
                        notificationId = notificationId
                    )
                )
                .addAction(
                    R.drawable.ic_check,
                    intent?.getStringExtra(EXTRA_TAKEN_LABEL).orEmpty().ifBlank { "Taken" },
                    actionBroadcastIntent(
                        action = ReminderActions.ACTION_CONFIRM_INTAKE,
                        scheduleIds = scheduleIds,
                        historyIds = historyIds,
                        scheduledTime = scheduledTime,
                        notificationId = notificationId,
                        requestOffset = REQUEST_CONFIRM
                    )
                )
        }

        return builder.build()
    }

    private fun actionBroadcastIntent(
        action: String,
        scheduleIds: String,
        historyIds: String,
        scheduledTime: Long,
        notificationId: Int,
        requestOffset: Int
    ): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, scheduleIds)
            putExtra(ReminderActions.EXTRA_HISTORY_IDS, historyIds)
            putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode(notificationId, requestOffset),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun changeTimeIntent(
        scheduleIds: String,
        historyIds: String,
        scheduledTime: Long,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(this, RescheduleReminderActivity::class.java).apply {
            action = ReminderActions.ACTION_CHANGE_TIME
            putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, scheduleIds)
            putExtra(ReminderActions.EXTRA_HISTORY_IDS, historyIds)
            putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, scheduledTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            this,
            requestCode(notificationId, REQUEST_CHANGE_TIME),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID_ALARM_RINGING,
            "Medication alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Medication alarms that keep ringing until stopped"
            setSound(null, null)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun getColorCompat(colorRes: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(colorRes)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(colorRes)
        }

    private fun requestCode(notificationId: Int, offset: Int): Int =
        ((notificationId and 0x00FFFFFF) * 10) + offset

    companion object {
        private const val ACTION_START = "com.rafaelswitala.mediguard.action.START_ALARM_RINGING"
        private const val ACTION_STOP = "com.rafaelswitala.mediguard.action.STOP_ALARM_RINGING"
        private const val CHANNEL_ID_ALARM_RINGING = "medication_alarm_ringing"
        private const val DEFAULT_NOTIFICATION_ID = 42
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_SNOOZE = 2
        private const val REQUEST_CHANGE_TIME = 3
        private const val REQUEST_CONFIRM = 4
        private const val REQUEST_STOP = 5
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_RINGTONE_URI = "ringtone_uri"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_TAKEN_LABEL = "taken_label"
        private const val EXTRA_SNOOZE_LABEL = "snooze_label"
        private const val EXTRA_CHANGE_TIME_LABEL = "change_time_label"
        private const val EXTRA_STOP_LABEL = "stop_label"

        fun start(
            context: Context,
            notificationId: Int,
            ringtoneUri: String?,
            title: String,
            message: String,
            scheduleIds: List<Long>,
            historyIds: List<Long>,
            scheduledTime: Long,
            takenLabel: String,
            snoozeLabel: String,
            changeTimeLabel: String,
            stopLabel: String
        ) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(ReminderActions.EXTRA_SCHEDULE_IDS, ReminderActions.idsToCsv(scheduleIds))
                putExtra(ReminderActions.EXTRA_HISTORY_IDS, ReminderActions.idsToCsv(historyIds))
                putExtra(ReminderActions.EXTRA_SCHEDULED_TIME, scheduledTime)
                putExtra(EXTRA_TAKEN_LABEL, takenLabel)
                putExtra(EXTRA_SNOOZE_LABEL, snoozeLabel)
                putExtra(EXTRA_CHANGE_TIME_LABEL, changeTimeLabel)
                putExtra(EXTRA_STOP_LABEL, stopLabel)
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}
