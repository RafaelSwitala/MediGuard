package com.rafaelswitala.mediguard.viewmodel

/**
 * Verwaltet die Medikamentenliste, Bestand, Gruppierungsvorschläge und Erinnerungszeiten.
 * Synchronisiert mit Datenbank und Alarm-Scheduler bei Änderungen.
 */

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.data.settings.DayPeriodSettingsProvider
import com.rafaelswitala.mediguard.domain.grouping.GroupingCandidate
import com.rafaelswitala.mediguard.domain.grouping.MedicationGroupingService
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.reminder.NextIntakeCalculator
import com.rafaelswitala.mediguard.domain.reminder.UpcomingIntake
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import com.rafaelswitala.mediguard.domain.usecases.AddMedicationUseCase
import com.rafaelswitala.mediguard.domain.usecases.ScheduleMedicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Datei für Medikamentenlisten, Bestand und Gruppierung.
 * Das ViewModel verbindet UI-Aktionen mit Repositories und Alarmplanung.
 */
@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val addMedicationUseCase: AddMedicationUseCase,
    private val scheduleMedicationUseCase: ScheduleMedicationUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val dayPeriodSettingsProvider: DayPeriodSettingsProvider
) : ViewModel() {

    private val _uiState = mutableStateOf(MedicationUiState())
    val uiState: androidx.compose.runtime.State<MedicationUiState> = _uiState

    private val _editingMedication = mutableStateOf<Medication?>(null)
    val editingMedication: androidx.compose.runtime.State<Medication?> = _editingMedication

    private val _editingSchedules = mutableStateOf<List<MedicationSchedule>>(emptyList())
    val editingSchedules: androidx.compose.runtime.State<List<MedicationSchedule>> = _editingSchedules

    private val _pendingGroupingCandidate = mutableStateOf<GroupingCandidate?>(null)
    val pendingGroupingCandidate: androidx.compose.runtime.State<GroupingCandidate?> = _pendingGroupingCandidate

    val activeMedications: StateFlow<List<Medication>> = medicationRepository
        .getAllActiveMedications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMedications: StateFlow<List<Medication>> = medicationRepository
        .getAllMedications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lowSupplyMedications: StateFlow<List<Medication>> = medicationRepository
        .getAllActiveMedications()
        .map { medications -> medications.filter { it.needsSupplyRestock() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingIntakes: StateFlow<List<UpcomingIntake>> = combine(
        medicationRepository.getAllActiveMedications(),
        scheduleRepository.getAllActiveSchedules(),
        dayPeriodSettingsProvider.settings
    ) { medications, schedules, daySettings ->
        val byMedication = schedules.groupBy { it.medicationId }
        NextIntakeCalculator.upcoming(
            medications = medications,
            schedulesByMedication = byMedication,
            dayPeriodSettings = daySettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            combine(
                medicationRepository.getAllActiveMedications(),
                scheduleRepository.getAllActiveSchedules()
            ) { meds, schedules -> meds to schedules }
                .collect { (meds, schedules) ->
                runGroupCleanup(meds)
                expandExistingGroups(meds, schedules.groupBy { it.medicationId })
            }
        }
    }

    fun addMedicationWithSchedules(
        medication: Medication,
        scheduleDrafts: List<MedicationSchedule>
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val medicationId = addMedicationUseCase(medication)
                persistSchedules(medicationId, scheduleDrafts)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Medication and reminders saved",
                    lastAddedMedicationId = medicationId
                )
                detectGroupingCandidate(medicationId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save medication: ${e.message}"
                )
            }
        }
    }

    fun updateMedicationWithSchedules(
        medication: Medication,
        scheduleDrafts: List<MedicationSchedule>
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                medicationRepository.updateMedication(medication)
                scheduleRepository.getSchedulesByMedicationId(medication.id)
                    .first()
                    .forEach { alarmScheduler.cancelAlarm(it.id) }
                scheduleRepository.deleteSchedulesByMedicationId(medication.id)
                persistSchedules(medication.id, scheduleDrafts)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Medication updated"
                )
                detectGroupingCandidate(medication.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update medication: ${e.message}"
                )
            }
        }
    }

    fun loadMedicationForEdit(medicationId: Long?) {
        if (medicationId == null) {
            _editingMedication.value = null
            _editingSchedules.value = emptyList()
            return
        }

        viewModelScope.launch {
            _editingMedication.value = medicationRepository.getMedicationById(medicationId)
            _editingSchedules.value = scheduleRepository.getSchedulesByMedicationId(medicationId).first()
        }
    }

    fun deleteMedication(medicationId: Long) {
        deleteMedications(listOf(medicationId))
    }

    fun deleteMedications(medicationIds: Collection<Long>) {
        viewModelScope.launch {
            try {
                medicationIds.distinct().forEach { medicationId ->
                    scheduleRepository.getSchedulesByMedicationId(medicationId)
                        .first()
                        .forEach { alarmScheduler.cancelAlarm(it.id) }
                    medicationRepository.deleteMedication(medicationId)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Medication deleted")
                runGroupCleanup(activeMedications.value)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete medication: ${e.message}"
                )
            }
        }
    }

    fun addStock(medicationId: Long, addedAmount: Double) {
        viewModelScope.launch {
            try {
                val medication = medicationRepository.getMedicationById(medicationId) ?: return@launch
                if (medication.medicationFormType.usesVolume()) {
                    medicationRepository.addRemainingVolume(medicationId, addedAmount.toDouble())
                } else {
                    medicationRepository.addRemainingDoses(medicationId, addedAmount)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Stock updated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update stock: ${e.message}"
                )
            }
        }
    }

    fun confirmGrouping(
        candidate: GroupingCandidate
    ) {
        viewModelScope.launch {
            val medications = activeMedications.value
            val schedules = scheduleRepository.getAllActiveSchedules().first()
            val connectedMedicationIds = connectedMedicationIds(
                root = candidate,
                medications = medications,
                schedulesByMedication = schedules.groupBy { it.medicationId }
            )
            val groupId = medications
                .filter { it.id in connectedMedicationIds }
                .mapNotNull { it.intakeGroupId }
                .firstOrNull()
                ?: MedicationGroupingService.nextAvailableGroupId(medications)

            connectedMedicationIds.forEach { medId ->
                medicationRepository.updateIntakeGroup(medId, groupId)
            }
            _pendingGroupingCandidate.value = null
        }
    }

    fun dismissGroupingPrompt() {
        _pendingGroupingCandidate.value = null
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    private suspend fun persistSchedules(medicationId: Long, scheduleDrafts: List<MedicationSchedule>) {
        scheduleDrafts.forEach { draft ->
            val schedule = draft.copy(medicationId = medicationId)
            val scheduleId = scheduleMedicationUseCase(schedule)
            alarmScheduler.scheduleAlarm(schedule.copy(id = scheduleId))
        }
    }

    private suspend fun detectGroupingCandidate(savedMedicationId: Long) {
        val medications = activeMedications.value
        val schedules = scheduleRepository.getAllActiveSchedules().first()
        val byMedication = schedules.groupBy { it.medicationId }
        val candidates = MedicationGroupingService.findCandidates(medications, byMedication)
            .filter { it.medicationA.id == savedMedicationId || it.medicationB.id == savedMedicationId }
        _pendingGroupingCandidate.value = candidates.firstOrNull()
    }

    private suspend fun runGroupCleanup(medications: List<Medication>) {
        MedicationGroupingService.cleanupGroups(medications).forEach { (id, groupId) ->
            medicationRepository.updateIntakeGroup(id, groupId)
        }
    }

    private suspend fun expandExistingGroups(
        medications: List<Medication>,
        schedulesByMedication: Map<Long, List<MedicationSchedule>>
    ) {
        val mutableGroups = medications.associate { it.id to it.intakeGroupId }.toMutableMap()
        MedicationGroupingService.findCandidates(medications, schedulesByMedication).forEach { candidate ->
            val groupA = mutableGroups[candidate.medicationA.id]
            val groupB = mutableGroups[candidate.medicationB.id]
            when {
                groupA != null && groupB == null -> mutableGroups[candidate.medicationB.id] = groupA
                groupB != null && groupA == null -> mutableGroups[candidate.medicationA.id] = groupB
                groupA != null && groupB != null && groupA != groupB -> {
                    mutableGroups.replaceAll { _, groupId -> if (groupId == groupB) groupA else groupId }
                }
            }
        }
        mutableGroups.forEach { (medicationId, groupId) ->
            val current = medications.firstOrNull { it.id == medicationId }?.intakeGroupId
            if (current != groupId) {
                medicationRepository.updateIntakeGroup(medicationId, groupId)
            }
        }
    }

    private fun connectedMedicationIds(
        root: GroupingCandidate,
        medications: List<Medication>,
        schedulesByMedication: Map<Long, List<MedicationSchedule>>
    ): Set<Long> {
        val candidates = MedicationGroupingService.findCandidates(medications, schedulesByMedication)
        val connected = mutableSetOf(root.medicationA.id, root.medicationB.id)
        var changed: Boolean
        do {
            changed = false
            candidates.forEach { candidate ->
                val ids = setOf(candidate.medicationA.id, candidate.medicationB.id)
                if (ids.any { it in connected } && connected.addAll(ids)) {
                    changed = true
                }
            }
        } while (changed)
        return connected
    }
}

data class MedicationUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastAddedMedicationId: Long? = null
)
