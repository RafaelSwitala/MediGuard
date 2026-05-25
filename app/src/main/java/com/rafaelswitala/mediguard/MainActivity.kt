package com.rafaelswitala.mediguard

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.rafaelswitala.mediguard.data.settings.AppPreferencesRepository
import com.rafaelswitala.mediguard.data.settings.AppSettings
import com.rafaelswitala.mediguard.data.settings.AppThemeMode
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.ui.localization.LocalAppLanguage
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.ui.localization.stringsFor
import com.rafaelswitala.mediguard.ui.navigation.AppNavigation
import com.rafaelswitala.mediguard.ui.theme.MediGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for MediGuard
 * Entry point for the application with Hilt dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MediGuard)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
        setContent {
            val settings by appPreferencesRepository.settings.collectAsState(initial = AppSettings())
            LaunchedEffect(settings.alertMode, settings.ringtoneUri) {
                val directBootAlarmStore = DirectBootAlarmStore(this@MainActivity)
                directBootAlarmStore.saveAlertMode(settings.alertMode)
                directBootAlarmStore.saveRingtone(settings.ringtoneUri)
            }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> systemDark
            }

            MediGuardTheme(
                darkTheme = darkTheme,
                dynamicColor = false
            ) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsFor(settings.language),
                    LocalAppLanguage provides settings.language
                ) {
                    val navController = rememberNavController()
                    Surface {
                        AppNavigation(navController = navController)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return

        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1001
    }
}
