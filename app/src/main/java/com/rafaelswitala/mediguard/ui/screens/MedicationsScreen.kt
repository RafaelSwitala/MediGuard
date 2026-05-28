package com.rafaelswitala.mediguard.ui.screens

/**
 * Medikamentenliste mit Auswahlmodus, Massenauswahl, Löschen und Gruppierungsvorschlägen.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.grouping.GroupingCandidate
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.model.ScheduleDataCodec
import com.rafaelswitala.mediguard.domain.model.formatMedicationAmount
import com.rafaelswitala.mediguard.ui.components.MedicationCard
import com.rafaelswitala.mediguard.ui.localization.AppStrings
import com.rafaelswitala.mediguard.ui.localization.LocalAppLanguage
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel

/**
 * Datei für die Medikamentenübersicht.
 * Enthält Liste, Detaildialog, Mehrfachauswahl und Gruppierungsabfrage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    viewModel: MedicationViewModel,
    onNavigateToAdd: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val medications by viewModel.activeMedications.collectAsState()
    val viewingMedication by viewModel.editingMedication
    val viewingSchedules by viewModel.editingSchedules
    val pendingGrouping by viewModel.pendingGroupingCandidate
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }
    var showDeleteSelectionDialog by remember { mutableStateOf(false) }
    var medicationToViewId by remember { mutableStateOf<Long?>(null) }
    val selectedMedicationIds = remember { mutableStateListOf<Long>() }
    val selectionMode = selectedMedicationIds.isNotEmpty()
    val selectedMedications = medications.filter { it.id in selectedMedicationIds }

    LaunchedEffect(medicationToViewId) {
        viewModel.loadMedicationForEdit(medicationToViewId)
    }

    LaunchedEffect(medications) {
        val currentIds = medications.map { it.id }.toSet()
        selectedMedicationIds.removeAll { it !in currentIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) {
                            "${selectedMedicationIds.size} ${strings.selectedCount}"
                        } else {
                            strings.medications
                        }
                    )
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { selectedMedicationIds.clear() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.cancel
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { showDeleteSelectionDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = strings.deleteSelected
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateToAdd) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addMedication
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = strings.home) },
                    label = { Text(strings.home) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Favorite, contentDescription = strings.medications) },
                    label = { Text(strings.medications) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateHistory,
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = strings.history) },
                    label = { Text(strings.history) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = strings.settings) },
                    label = { Text(strings.settings) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (medications.isEmpty()) {
                EmptyMedicationsState(onAdd = onNavigateToAdd)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    items(medications, key = { it.id }) { medication ->
                        MedicationCard(
                            medication = medication,
                            onView = { medicationToViewId = medication.id },
                            onLongPress = { id ->
                                if (id !in selectedMedicationIds) {
                                    selectedMedicationIds.add(id)
                                }
                            },
                            selectionMode = selectionMode,
                            selected = medication.id in selectedMedicationIds,
                            onSelectionChange = { checked ->
                                if (checked && medication.id !in selectedMedicationIds) {
                                    selectedMedicationIds.add(medication.id)
                                } else if (!checked) {
                                    selectedMedicationIds.remove(medication.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    medicationToDelete?.let { medication ->
        DeleteMedicationDialog(
            medication = medication,
            onDismiss = { medicationToDelete = null },
            onConfirm = {
                viewModel.deleteMedication(medication.id)
                medicationToDelete = null
            }
        )
    }

    if (showDeleteSelectionDialog) {
        DeleteSelectedMedicationsDialog(
            medications = selectedMedications,
            onDismiss = { showDeleteSelectionDialog = false },
            onConfirm = {
                viewModel.deleteMedications(selectedMedicationIds.toList())
                selectedMedicationIds.clear()
                showDeleteSelectionDialog = false
            }
        )
    }

    if (medicationToViewId != null && viewingMedication?.id == medicationToViewId) {
        MedicationDetailsDialog(
            medication = viewingMedication,
            schedules = viewingSchedules,
            onDismiss = { medicationToViewId = null },
            onEdit = { medication ->
                medicationToViewId = null
                onNavigateToEdit(medication.id)
            }
        )
    }

    pendingGrouping?.let { candidate ->
        GroupingPromptDialog(
            candidate = candidate,
            onDismiss = { viewModel.dismissGroupingPrompt() },
            onConfirm = {
                viewModel.confirmGrouping(candidate)
            }
        )
    }
}

@Composable
private fun EmptyMedicationsState(onAdd: () -> Unit) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(strings.createMedicationNow, style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onAdd, modifier = Modifier.padding(top = 16.dp)) {
            Text(strings.addMedication)
        }
    }
}

@Composable
private fun GroupingPromptDialog(
    candidate: GroupingCandidate,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.groupTogetherTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.groupTogetherBody)
                Text("- ${candidate.medicationA.name}")
                Text("- ${candidate.medicationB.name}")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(strings.groupTogetherConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.groupTogetherDecline)
            }
        }
    )
}

@Composable
private fun DeleteMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.confirmDeleteTitle) },
        text = { Text("${strings.confirmDeleteBody}\n\n${medication.name}") },
        confirmButton = {
            Button(onClick = onConfirm) { Text(strings.delete) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun DeleteSelectedMedicationsDialog(
    medications: List<Medication>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.confirmDeleteSelectedTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.confirmDeleteSelectedBody)
                medications.forEach { medication ->
                    Text("- ${medication.name}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = medications.isNotEmpty()
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun MedicationDetailsDialog(
    medication: Medication?,
    schedules: List<MedicationSchedule>,
    onDismiss: () -> Unit,
    onEdit: (Medication) -> Unit
) {
    if (medication == null) return
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "${strings.details}: ${medication.name}",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onEdit(medication) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = strings.edit
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailLine(strings.dosage, "${medication.dosage} ${medication.dosageUnit}")
                if (medication.doseQuantity > 1.0) {
                    DetailLine(strings.doseQuantity, formatMedicationAmount(medication.doseQuantity))
                }
                if (medication.description.isNotBlank()) {
                    DetailLine(strings.notes, medication.description)
                }
                medication.durationDays?.let { days ->
                    DetailLine(strings.durationDays, days.toString())
                }
                medication.treatmentLimitDoses?.let { limit ->
                    DetailLine(strings.treatmentLimitDoses, limit.toString())
                }
                if (medication.remainingDoses != null || medication.remainingVolumeMl != null) {
                    DetailLine(strings.stock, medication.stockLabel(strings))
                }
                Text(
                    strings.schedules,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (schedules.isEmpty()) {
                    Text(strings.noSchedules, style = MaterialTheme.typography.bodyMedium)
                } else {
                    schedules.forEach { schedule ->
                        Text(
                            formatSchedule(schedule, strings, language),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatSchedule(
    schedule: MedicationSchedule,
    strings: AppStrings,
    language: com.rafaelswitala.mediguard.data.settings.AppLanguage
): String =
    when (schedule.scheduleType) {
        FrequencyType.EXACT_TIME -> {
            val hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8)
            val minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
            "${strings.exactTime}: ${formatScheduleTime(hour, minute)}"
        }
        FrequencyType.TIME_RANGE -> {
            val startHour = ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8)
            val startMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0)
            val endHour = ScheduleDataCodec.readInt(schedule.scheduleData, "endHour", 10)
            val endMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "endMinute", 0)
            "${strings.timeRange}: ${formatScheduleTime(startHour, startMinute)} - ${formatScheduleTime(endHour, endMinute)}"
        }
        FrequencyType.DAY_PERIOD -> "${strings.dayPeriod}: ${
            ScheduleDataCodec.readPeriod(schedule.scheduleData).label(language)
        }"
        FrequencyType.INTERVAL -> {
            val hours = ScheduleDataCodec.readInt(schedule.scheduleData, "intervalHours", 6)
            val startHour = ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8)
            val startMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0)
            "${strings.interval}: $hours h, ${strings.startTime} ${formatScheduleTime(startHour, startMinute)}"
        }
        FrequencyType.WEEKLY -> {
            val hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8)
            val minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
            "${strings.weekly}: ${
                ScheduleDataCodec.readDays(schedule.scheduleData).joinToString(", ") { it.label(language) }
            } ${formatScheduleTime(hour, minute)}"
        }
    }

private fun formatScheduleTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
