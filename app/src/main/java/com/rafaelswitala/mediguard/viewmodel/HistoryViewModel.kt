package com.rafaelswitala.mediguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.usecases.ConfirmMedicationIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for medication intake history
 * Manages history data and user interactions
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val intakeHistoryRepository: IntakeHistoryRepository,
    private val confirmIntakeUseCase: ConfirmMedicationIntakeUseCase
) : ViewModel() {

    private val _uiState = androidx.compose.runtime.mutableStateOf(HistoryUiState())
    val uiState: androidx.compose.runtime.State<HistoryUiState> = _uiState

    val allIntakeHistory: StateFlow<List<IntakeHistory>> = intakeHistoryRepository
        .getAllIntakeHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val adherenceRate: StateFlow<Int> = intakeHistoryRepository
        .getAllIntakeHistory()
        .map { entries ->
            if (entries.isEmpty()) {
                0
            } else {
                (entries.count { it.isTaken() } * 100) / entries.size
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun getIntakeHistoryForMedication(medicationId: Long): StateFlow<List<IntakeHistory>> {
        return intakeHistoryRepository
            .getIntakeHistoryByMedicationId(medicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun confirmIntake(intakeHistoryId: Long) {
        viewModelScope.launch {
            try {
                confirmIntakeUseCase(intakeHistoryId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Intake confirmed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to confirm intake: ${e.message}"
                )
            }
        }
    }

    fun confirmIntakes(intakeHistoryIds: List<Long>) {
        viewModelScope.launch {
            try {
                intakeHistoryIds.forEach { intakeHistoryId ->
                    confirmIntakeUseCase(intakeHistoryId)
                }
                _uiState.value = _uiState.value.copy(
                    successMessage = "Intake confirmed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to confirm intake: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
}

data class HistoryUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
