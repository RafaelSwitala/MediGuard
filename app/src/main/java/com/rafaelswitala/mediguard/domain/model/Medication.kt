package com.rafaelswitala.mediguard.domain.model

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val dosageUnit: String = "mg",
    val description: String = "",
    val treatmentType: TreatmentType = TreatmentType.ONGOING,
    val durationDays: Int? = null,
    val treatmentLimitDoses: Int? = null,
    val remainingDoses: Int? = null,
    val remainingVolumeMl: Double? = null,
    val dosePerIntakeMl: Double? = null,
    val doseQuantity: Int = 1,
    val medicationFormType: MedicationFormType = MedicationFormType.TABLET,
    val intakeGroupId: Int? = null,
    @Deprecated("Use intakeGroupId")
    val groupName: String? = null,
    val supplyAlertEnabled: Boolean = true,
    val supplyAlertThreshold: Int = 7,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun needsSupplyRestock(): Boolean {
        if (!supplyAlertEnabled) return false
        return if (medicationFormType.usesVolume()) {
            remainingVolumeMl != null && remainingVolumeMl <= supplyAlertThreshold.toDouble()
        } else {
            remainingDoses != null && remainingDoses <= supplyAlertThreshold
        }
    }

    fun stockLabel(strings: com.rafaelswitala.mediguard.ui.localization.AppStrings): String =
        if (medicationFormType.usesVolume()) {
            "${strings.remainingVolume}: ${remainingVolumeMl ?: 0} ml"
        } else {
            "${strings.tabletsLeft}: ${remainingDoses ?: 0}"
        }
}

enum class TreatmentType {
    ONGOING,
    LIMITED;

    override fun toString(): String = when (this) {
        ONGOING -> "Ongoing"
        LIMITED -> "Limited"
    }
}
