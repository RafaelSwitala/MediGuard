package com.rafaelswitala.mediguard.ui.theme

/**
 * MediGuardTheme Composable mit dynamischer Farbunterstützung (Android 12+).
 */

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Tertiary,
    onPrimary = Color(0xFF00211C),
    primaryContainer = Color(0xFF004D45),
    onPrimaryContainer = Color(0xFFB9FFF2),
    secondary = Color(0xFF9ADBE4),
    onSecondary = Color(0xFF052B32),
    secondaryContainer = Color(0xFF113C45),
    onSecondaryContainer = Color(0xFFD7FAFF),
    tertiary = PrimaryLight,
    onTertiary = Color(0xFF002019),
    error = Error,
    onError = Color(0xFFFFFFFF),
    background = Color(0xFF061218),
    onBackground = Color(0xFFE5F7F4),
    surface = Color(0xFF0B1B22),
    onSurface = Color(0xFFE5F7F4),
    surfaceVariant = Color(0xFF12313A),
    onSurfaceVariant = Color(0xFFC0DCD7)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8FFF0),
    onPrimaryContainer = Color(0xFF00352F),
    secondary = Secondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC9F6FF),
    onSecondaryContainer = Color(0xFF062D35),
    tertiary = Tertiary,
    onTertiary = Color(0xFF00241D),
    error = Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFF35514D)
)

@Composable
fun MediGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
