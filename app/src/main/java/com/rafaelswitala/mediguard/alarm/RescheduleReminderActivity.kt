package com.rafaelswitala.mediguard.alarm

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.notification.MedicationNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class RescheduleReminderActivity : ComponentActivity() {
    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var intakeHistoryRepository: IntakeHistoryRepository

    @Inject
    lateinit var notificationManager: MedicationNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmRingingService.stop(this)

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

        if (historyIds.isEmpty()) {
            finish()
            return
        }

        val now = Calendar.getInstance()
        var handled = false
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                handled = true
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                lifecycleScope.launch {
                    historyIds.forEach { historyId ->
                        intakeHistoryRepository.updateIntakeStatus(
                            historyId,
                            IntakeStatus.SNOOZED.name,
                            null
                        )
                    }
                    alarmScheduler.scheduleOneTimeReminder(
                        triggerAtMillis = selectedTime.timeInMillis,
                        scheduleIds = scheduleIds,
                        historyIds = historyIds,
                        notificationId = notificationId
                    )
                    notificationManager.cancelNotification(notificationId)
                    finish()
                }
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).apply {
            setOnCancelListener { finish() }
            setOnDismissListener {
                if (!handled && !isFinishing) finish()
            }
        }.show()
    }
}
