package com.rafaelswitala.mediguard.ui.localization

/**
 * CompositionLocal für die aktuelle Sprache in Screens und Komponenten.
 */

import androidx.compose.runtime.staticCompositionLocalOf
import com.rafaelswitala.mediguard.data.settings.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EN }
