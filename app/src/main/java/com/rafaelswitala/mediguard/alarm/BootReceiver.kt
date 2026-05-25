package com.rafaelswitala.mediguard.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
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

/**
 * Restores alarms after locked boot and normal boot. Locked boot only touches
 * Device Encrypted metadata, while user-unlocked restore may read CE schedules.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val directBootAlarmStore = DirectBootAlarmStore(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                        directBootAlarmStore.markLockedBootCompleted()
                        restoreFromDeviceEncryptedStorage(context, directBootAlarmStore)
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    "android.intent.action.QUICKBOOT_POWERON" -> {
                        if (isCredentialStorageUnlocked(context)) {
                            restoreFromCredentialEncryptedStorage(context, directBootAlarmStore)
                        } else {
                            restoreFromDeviceEncryptedStorage(context, directBootAlarmStore)
                        }
                    }

                    Intent.ACTION_USER_UNLOCKED -> restoreFromCredentialEncryptedStorage(
                        context,
                        directBootAlarmStore
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun restoreFromDeviceEncryptedStorage(
        context: Context,
        directBootAlarmStore: DirectBootAlarmStore
    ) {
        directBootAlarmStore.getAllAlarmSchedules()
            .first()
            .forEach { alarmData ->
                DirectBootAlarmScheduler.scheduleStoredAlarm(
                    context = context,
                    directBootAlarmStore = directBootAlarmStore,
                    alarmData = alarmData,
                    allowImmediateIfOverdue = true
                )
            }
    }

    private suspend fun restoreFromCredentialEncryptedStorage(
        context: Context,
        directBootAlarmStore: DirectBootAlarmStore
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        restorePendingLockedNotifications(directBootAlarmStore, entryPoint)
        entryPoint.medicationScheduleRepository()
            .getAllActiveSchedules()
            .first()
            .forEach { entryPoint.alarmScheduler().scheduleAlarm(it) }
    }

    private suspend fun restorePendingLockedNotifications(
        directBootAlarmStore: DirectBootAlarmStore,
        entryPoint: BootReceiverEntryPoint
    ) {
        directBootAlarmStore.getPendingLockedAlarms()
            .first()
            .forEach { pendingAlarm ->
                val items = pendingAlarm.scheduleIds.mapNotNull { scheduleId ->
                    val alarmData = directBootAlarmStore.getAlarmSchedule(scheduleId)
                        ?: return@mapNotNull null
                    val medication = entryPoint.medicationRepository().getMedicationById(alarmData.medicationId)
                        ?: return@mapNotNull null
                    val historyId = entryPoint.intakeHistoryRepository().recordIntake(
                        IntakeHistory(
                            medicationId = medication.id,
                            scheduledTime = pendingAlarm.scheduledTime,
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

                if (items.isNotEmpty()) {
                    entryPoint.notificationManager().showMedicationGroupNotification(
                        scheduleIds = pendingAlarm.scheduleIds,
                        scheduledTime = pendingAlarm.scheduledTime,
                        items = items,
                        notificationId = pendingAlarm.notificationId.toInt()
                    )
                }
                directBootAlarmStore.removePendingLockedAlarm(pendingAlarm.notificationId)
            }
    }

    private fun isCredentialStorageUnlocked(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) return true
        val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
        return userManager.isUserUnlocked
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun alarmScheduler(): AlarmScheduler
    fun medicationScheduleRepository(): MedicationScheduleRepository
    fun medicationRepository(): MedicationRepository
    fun intakeHistoryRepository(): IntakeHistoryRepository
    fun notificationManager(): MedicationNotificationManager
}
