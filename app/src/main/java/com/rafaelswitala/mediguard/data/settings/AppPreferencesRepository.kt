package com.rafaelswitala.mediguard.data.settings

/**
 * DataStore-basierte Einstellungen für Theme, Sprache, Benachrichtigungsmodus, Klingelton und Tageszeiten.
 */

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AppLanguage {
    EN,
    DE
}

enum class AppAlertMode {
    SILENT_NOTIFICATION,
    SOUND_NOTIFICATION,
    ALARM;

    companion object {
        fun fromStoredValue(value: String?): AppAlertMode =
            when (value) {
                "NOTIFICATION" -> SILENT_NOTIFICATION
                null -> ALARM
                else -> runCatching { valueOf(value) }.getOrDefault(ALARM)
            }
    }
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.EN,
    val alertMode: AppAlertMode = AppAlertMode.ALARM,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    val dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
)

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<AppSettings> = context.appSettingsDataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[themeModeKey]?.toEnumOrDefault(AppThemeMode.LIGHT)
                ?: AppThemeMode.LIGHT,
            language = preferences[languageKey]?.toEnumOrDefault(AppLanguage.EN)
                ?: AppLanguage.EN,
            alertMode = AppAlertMode.fromStoredValue(preferences[alertModeKey]),
            ringtoneUri = preferences[ringtoneUriKey],
            ringtoneTitle = preferences[ringtoneTitleKey],
            dayPeriodSettings = DayPeriodSettings.fromJson(preferences[dayPeriodSettingsKey])
        )
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[languageKey] = language.name
        }
    }

    suspend fun setAlertMode(alertMode: AppAlertMode) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[alertModeKey] = alertMode.name
        }
        DirectBootAlarmStore(context).saveAlertMode(alertMode)
    }

    suspend fun setRingtone(uri: String?, title: String?) {
        context.appSettingsDataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(ringtoneUriKey)
                preferences.remove(ringtoneTitleKey)
            } else {
                preferences[ringtoneUriKey] = uri
                preferences[ringtoneTitleKey] = title.orEmpty()
            }
        }
        DirectBootAlarmStore(context).saveRingtone(uri)
    }

    suspend fun setDayPeriodSettings(dayPeriodSettings: DayPeriodSettings) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[dayPeriodSettingsKey] = dayPeriodSettings.toJson()
        }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(defaultValue: T): T =
        runCatching { enumValueOf<T>(this) }.getOrDefault(defaultValue)

    companion object {
        private val themeModeKey = stringPreferencesKey("theme_mode")
        private val languageKey = stringPreferencesKey("language")
        private val alertModeKey = stringPreferencesKey("alert_mode")
        private val ringtoneUriKey = stringPreferencesKey("ringtone_uri")
        private val ringtoneTitleKey = stringPreferencesKey("ringtone_title")
        private val dayPeriodSettingsKey = stringPreferencesKey("day_period_settings")
    }
}
