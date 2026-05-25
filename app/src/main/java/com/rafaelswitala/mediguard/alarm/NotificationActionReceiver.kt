package com.rafaelswitala.mediguard.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.usecases.ConfirmMedicationIntakeUseCase
import com.rafaelswitala.mediguard.notification.MedicationNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var confirmMedicationIntakeUseCase: ConfirmMedicationIntakeUseCase

    @Inject
    lateinit var intakeHistoryRepository: IntakeHistoryRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationManager: MedicationNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderActions.ACTION_CONFIRM_INTAKE -> confirmIntake(context, intent)
                    ReminderActions.ACTION_SNOOZE -> snooze(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun confirmIntake(context: Context, intent: Intent) {
        AlarmRingingService.stop(context)
        val historyIds = ReminderActions.idsFromCsv(
            intent.getStringExtra(ReminderActions.EXTRA_HISTORY_IDS)
        )
        val notificationId = intent.getIntExtra(ReminderActions.EXTRA_NOTIFICATION_ID, 0)

        historyIds.forEach { historyId ->
            confirmMedicationIntakeUseCase(historyId)
        }
        alarmScheduler.cancelOneTimeReminder(notificationId)
        notificationManager.cancelNotification(notificationId)
    }

    private suspend fun snooze(context: Context, intent: Intent) {
        AlarmRingingService.stop(context)
        val scheduleIds = ReminderActions.idsFromCsv(
            intent.getStringExtra(ReminderActions.EXTRA_SCHEDULE_IDS)
        )
        val historyIds = ReminderActions.idsFromCsv(
            intent.getStringExtra(ReminderActions.EXTRA_HISTORY_IDS)
        )
        val notificationId = intent.getIntExtra(
            ReminderActions.EXTRA_NOTIFICATION_ID,
            scheduleIds.minOrNull()?.toInt() ?: 0
        )

        historyIds.forEach { historyId ->
            intakeHistoryRepository.updateIntakeStatus(historyId, IntakeStatus.SNOOZED.name, null)
        }

        alarmScheduler.scheduleOneTimeReminder(
            triggerAtMillis = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
            scheduleIds = scheduleIds,
            historyIds = historyIds,
            notificationId = notificationId
        )
        notificationManager.cancelNotification(notificationId)
    }

    companion object {
        private const val SNOOZE_MINUTES = 15
    }
}
