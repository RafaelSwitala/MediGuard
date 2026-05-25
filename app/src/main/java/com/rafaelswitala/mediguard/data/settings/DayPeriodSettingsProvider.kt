package com.rafaelswitala.mediguard.data.settings

import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayPeriodSettingsProvider @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _settings = MutableStateFlow(DayPeriodSettings.Default)
    val settings: StateFlow<DayPeriodSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            appPreferencesRepository.settings.collect { appSettings ->
                _settings.value = appSettings.dayPeriodSettings
            }
        }
    }

    fun current(): DayPeriodSettings = _settings.value
}
