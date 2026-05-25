package com.rafaelswitala.mediguard.domain.model

import com.rafaelswitala.mediguard.ui.localization.AppStrings

/**
 * Domain model for IntakeHistory
 * Tracks medication intake confirmations
 */
data class IntakeHistory(
    val id: Long = 0,
    val medicationId: Long,
    val scheduledTime: Long,  // Timestamp of scheduled time
    val actualIntakeTime: Long? = null,  // Null if not taken
    val dosage: String,
    val dosageUnit: String,
    val medicationName: String,
    val status: IntakeStatus = IntakeStatus.PENDING
) {
    fun isTaken(): Boolean = actualIntakeTime != null && status == IntakeStatus.CONFIRMED
}

/**
 * Enum for intake status
 */
enum class IntakeStatus {
    PENDING,
    CONFIRMED,
    MISSED,
    SNOOZED;

    fun label(strings: AppStrings): String = when (this) {
        PENDING -> strings.statusPending
        CONFIRMED -> strings.statusConfirmed
        MISSED -> strings.statusMissed
        SNOOZED -> strings.statusSnoozed
    }

    override fun toString(): String = when (this) {
        PENDING -> "Pending"
        CONFIRMED -> "Confirmed"
        MISSED -> "Missed"
        SNOOZED -> "Snoozed"
    }
}
