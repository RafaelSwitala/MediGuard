package com.rafaelswitala.mediguard.domain.model

/**
 * Definiert die Darreichungsform eines Medikaments (Tablette, Sirup, Spray).
 * Bestimmt die Bestandserfassung (Menge vs. Anzahl) und Standardwerte.
 */

import com.rafaelswitala.mediguard.data.settings.AppLanguage

enum class MedicationFormType {
    TABLET,
    SYRUP,
    SPRAY;

    fun usesVolume(): Boolean = this == SYRUP

    fun defaultUnit(): String = when (this) {
        SYRUP -> "ml"
        SPRAY -> "Hub"
        TABLET -> "mg"
    }

    fun defaultSupplyThreshold(): Int = when (this) {
        SYRUP -> 50
        else -> 7
    }

    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.DE -> when (this) {
            TABLET -> "Tablette"
            SYRUP -> "Sirup"
            SPRAY -> "Spray"
        }
        AppLanguage.EN -> when (this) {
            TABLET -> "Tablet"
            SYRUP -> "Syrup"
            SPRAY -> "Spray"
        }
    }
}
