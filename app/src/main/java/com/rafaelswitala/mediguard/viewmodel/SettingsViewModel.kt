package com.rafaelswitala.mediguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.data.settings.AppLanguage
import com.rafaelswitala.mediguard.data.settings.AppAlertMode
import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.data.settings.AppPreferencesRepository
import com.rafaelswitala.mediguard.data.settings.AppSettings
import com.rafaelswitala.mediguard.data.settings.AppThemeMode
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    val settings: StateFlow<AppSettings> = appPreferencesRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appPreferencesRepository.setLanguage(language)
        }
    }

    fun setAlertMode(alertMode: AppAlertMode) {
        viewModelScope.launch {
            appPreferencesRepository.setAlertMode(alertMode)
        }
    }

    fun setRingtone(uri: String?, title: String?) {
        viewModelScope.launch {
            appPreferencesRepository.setRingtone(uri, title)
        }
    }

    fun setDayPeriodSettings(dayPeriodSettings: DayPeriodSettings, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val validation = dayPeriodSettings.validate()
            if (!validation.isValid) {
                onResult(false)
                return@launch
            }
            appPreferencesRepository.setDayPeriodSettings(dayPeriodSettings)
            scheduleRepository.getAllActiveSchedules().first()
                .filter { it.scheduleType == FrequencyType.DAY_PERIOD }
                .forEach { alarmScheduler.scheduleAlarm(it) }
            onResult(true)
        }
    }
}
