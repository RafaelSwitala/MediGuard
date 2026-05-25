package com.rafaelswitala.mediguard.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.notification.MedicationNotificationManager
import com.rafaelswitala.mediguard.notification.ReminderNotificationItem
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Receives reminder alarm broadcasts.
 * Recurring alarms are grouped only when the due medications share an explicit
 * medication group.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAlarm(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderActions.ACTION_ONE_TIME_REMINDER -> handleOneTimeReminder(context, intent)
            else -> handleRecurringReminder(context, intent)
        }
    }

    private suspend fun handleRecurringReminder(context: Context, intent: Intent) {
        val directBootAlarmStore = DirectBootAlarmStore(context.applicationContext)
        val scheduleId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val medicationId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val scheduledTime = intent.getLongExtra(
            AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME,
            System.currentTimeMillis()
        )

        if (scheduleId <= 0L || medicationId <= 0L) return

        val storedAlarm = directBootAlarmStore.getAlarmSchedule(scheduleId)
        if (storedAlarm != null && storedAlarm.nextTriggerAtMillis > scheduledTime + GROUP_WINDOW_MS) {
            return
        }

        val dueAlarms = directBootAlarmStore.getAllAlarmSchedules()
            .first()
            .filter { abs(it.nextTriggerAtMillis - scheduledTime) <= GROUP_WINDOW_MS }
            .ifEmpty {
                listOf(
                    storedAlarm ?: DirectBootAlarmStore.AlarmScheduleData(
                        scheduleId = scheduleId,
                        medicationId = medicationId,
                        nextTriggerAtMillis = scheduledTime,
                        scheduleType = "",
                        scheduleData = "",
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }

        if (!isCredentialStorageUnlocked(context)) {
            val leaderScheduleId = dueAlarms.minOf { it.scheduleId }
            if (scheduleId != leaderScheduleId) return

            val notificationId = leaderScheduleId.toInt()
            directBootAlarmStore.savePendingLockedAlarm(
                notificationId = leaderScheduleId,
                scheduledTime = scheduledTime,
                scheduleIds = dueAlarms.map { it.scheduleId }
            )
            DirectBootReminderNotifier.showPrivacyReminder(
                context = context,
                directBootAlarmStore = directBootAlarmStore,
                notificationId = notificationId,
                scheduledTime = scheduledTime
            )
            dueAlarms.forEach { alarmData ->
                if (alarmData.scheduleType.isNotBlank()) {
                    DirectBootAlarmScheduler.scheduleStoredAlarm(
                        context = context,
                        directBootAlarmStore = directBootAlarmStore,
                        alarmData = alarmData
                    )
                }
            }
        } else {
            val entryPoint = entryPoint(context)
            val dueMedicationAlarms = dueAlarms.mapNotNull { alarmData ->
                val medication = entryPoint.medicationRepository().getMedicationById(alarmData.medicationId)
                    ?: return@mapNotNull null
                DueMedicationAlarm(alarmData, medication)
            }
            val currentAlarm = dueMedicationAlarms.firstOrNull { it.alarmData.scheduleId == scheduleId }
                ?: return
            val currentGroup = currentAlarm.medication.intakeGroupId
            val notificationAlarms = if (currentGroup == null) {
                listOf(currentAlarm)
            } else {
                dueMedicationAlarms.filter { it.medication.intakeGroupId == currentGroup }
            }

            val leaderScheduleId = notificationAlarms.minOf { it.alarmData.scheduleId }
            if (scheduleId != leaderScheduleId) return

            val notificationId = leaderScheduleId.toInt()
            showUnlockedNotification(
                entryPoint = entryPoint,
                dueAlarms = notificationAlarms,
                scheduledTime = scheduledTime,
                notificationId = notificationId
            )

            notificationAlarms.forEach { dueMedicationAlarm ->
                val alarmData = dueMedicationAlarm.alarmData
                if (alarmData.scheduleType.isNotBlank()) {
                    entryPoint.alarmScheduler().scheduleStoredAlarm(alarmData)
                }
            }
        }
    }

    private suspend fun handleOneTimeReminder(context: Context, intent: Intent) {
        val directBootAlarmStore = DirectBootAlarmStore(context.applicationContext)
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
        val scheduledTime = intent.getLongExtra(
            ReminderActions.EXTRA_SCHEDULED_TIME,
            System.currentTimeMillis()
        )

        if (historyIds.isEmpty()) return

        if (!isCredentialStorageUnlocked(context)) {
            DirectBootReminderNotifier.showPrivacyReminder(
                context = context,
                directBootAlarmStore = directBootAlarmStore,
                notificationId = notificationId,
                scheduledTime = scheduledTime
            )
            return
        }

        val entryPoint = entryPoint(context)
        val histories = historyIds
            .mapNotNull { historyId -> entryPoint.intakeHistoryRepository().getIntakeHistoryById(historyId) }
            .filterNot { it.isTaken() }

        if (histories.isEmpty()) {
            entryPoint.notificationManager().cancelNotification(notificationId)
            return
        }

        val items = histories.map { history ->
            ReminderNotificationItem(
                medicationId = history.medicationId,
                historyId = history.id,
                medicationName = history.medicationName,
                dosage = history.dosage,
                dosageUnit = history.dosageUnit
            )
        }

        entryPoint.notificationManager().showMedicationGroupNotification(
            scheduleIds = scheduleIds,
            scheduledTime = scheduledTime,
            items = items,
            notificationId = notificationId
        )
    }

    private suspend fun showUnlockedNotification(
        entryPoint: AlarmReceiverEntryPoint,
        dueAlarms: List<DueMedicationAlarm>,
        scheduledTime: Long,
        notificationId: Int
    ) {
        val items = dueAlarms.map { dueMedicationAlarm ->
            val medication = dueMedicationAlarm.medication
            val historyId = entryPoint.intakeHistoryRepository().recordIntake(
                IntakeHistory(
                    medicationId = medication.id,
                    scheduledTime = scheduledTime,
                    dosage = medication.dosage,
                    dosageUnit = medication.dosageUnit,
                    medicationName = medication.name,
                    status = IntakeStatus.PENDING
                )
            )

            ReminderNotificationItem(
                medicationId = medication.id,
                historyId = historyId,
                medicationName = medication.name,
                dosage = medication.dosage,
                dosageUnit = medication.dosageUnit
            )
        }

        if (items.isEmpty()) {
            entryPoint.notificationManager().showPrivacyMedicationNotification(notificationId.toLong())
            return
        }

        entryPoint.notificationManager().showMedicationGroupNotification(
            scheduleIds = dueAlarms.map { it.alarmData.scheduleId },
            scheduledTime = scheduledTime,
            items = items,
            notificationId = notificationId
        )
    }

    private fun entryPoint(context: Context): AlarmReceiverEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AlarmReceiverEntryPoint::class.java
        )

    private fun isCredentialStorageUnlocked(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) return true
        val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
        return userManager.isUserUnlocked
    }

    companion object {
        private const val GROUP_WINDOW_MS = 10 * 60_000L
    }
}

private data class DueMedicationAlarm(
    val alarmData: DirectBootAlarmStore.AlarmScheduleData,
    val medication: Medication
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlarmReceiverEntryPoint {
    fun alarmScheduler(): AlarmScheduler
    fun medicationRepository(): MedicationRepository
    fun intakeHistoryRepository(): IntakeHistoryRepository
    fun notificationManager(): MedicationNotificationManager
}
