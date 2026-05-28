package com.rafaelswitala.mediguard.ui.theme

/**
 * Material Design 3 Farbpalette: Primär, Sekundär, Tertiär, Fehler, Oberfläche, Hintergrund.
 * Mit Varianten für Hell- und Dunkelmodus.
 */

import androidx.compose.ui.graphics.Color

// Logo-inspired palette: deep navy, teal and mint.
val Primary = Color(0xFF00BFA5)
val PrimaryLight = Color(0xFF5FFFE0)
val PrimaryDark = Color(0xFF006C63)

val Secondary = Color(0xFF0B3D4A)
val SecondaryLight = Color(0xFF2B6C78)
val SecondaryDark = Color(0xFF061927)

val Tertiary = Color(0xFF4DF2C8)
val Success = Color(0xFF00C781)
val Warning = Color(0xFFFFB020)
val Error = Color(0xFFEA4D4D)

val Surface = Color(0xFFF3FFFB)
val SurfaceVariant = Color(0xFFD7F8F0)
val Background = Color(0xFFEAFBF7)
val OnSurface = Color(0xFF102224)
val OnBackground = Color(0xFF08191C)

// Legacy colors for compatibility
val Purple80 = PrimaryLight
val PurpleGrey80 = SecondaryLight
val Pink80 = Warning

val Purple40 = Primary
val PurpleGrey40 = Secondary
val Pink40 = Error
