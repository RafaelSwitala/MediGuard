package com.rafaelswitala.mediguard

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.alarm.DirectBootAlarmScheduler
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class MediGuardApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            restoreAlarmsAfterStart()
        }
    }

    private suspend fun restoreAlarmsAfterStart() {
        val directBootAlarmStore = DirectBootAlarmStore(this)
        directBootAlarmStore.getAllAlarmSchedules().first().forEach { stored ->
            DirectBootAlarmScheduler.scheduleStoredAlarm(
                context = this,
                directBootAlarmStore = directBootAlarmStore,
                alarmData = stored,
                allowImmediateIfOverdue = true
            )
        }
        if (!isCredentialStorageUnlocked()) return
        runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                this,
                MediGuardApplicationEntryPoint::class.java
            )
            entryPoint.medicationScheduleRepository().getAllActiveSchedules().first().forEach { schedule ->
                entryPoint.alarmScheduler().scheduleAlarm(schedule)
            }
        }
    }

    private fun isCredentialStorageUnlocked(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.isUserUnlocked
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediGuardApplicationEntryPoint {
    fun alarmScheduler(): AlarmScheduler
    fun medicationScheduleRepository(): MedicationScheduleRepository
}
