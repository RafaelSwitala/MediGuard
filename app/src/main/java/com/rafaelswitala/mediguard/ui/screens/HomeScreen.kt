package com.rafaelswitala.mediguard.ui.screens

/**
 * Dashboard mit fälligen Medikamenten, Bestandswarnungen und nächsten Intakes.
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.R
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.reminder.UpcomingIntake
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.HistoryViewModel
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Datei für den Startbildschirm.
 * Zeigt fällige Einnahmen, niedrigen Bestand und die nächsten Erinnerungen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MedicationViewModel,
    historyViewModel: HistoryViewModel,
    onNavigateToMedications: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val medications by viewModel.activeMedications.collectAsState()
    val lowSupplyMedications by viewModel.lowSupplyMedications.collectAsState()
    val upcomingIntakes by viewModel.upcomingIntakes.collectAsState()
    val intakeHistory by historyViewModel.allIntakeHistory.collectAsState()
    val pendingGroups = intakeHistory
        .filter { it.status == IntakeStatus.PENDING || it.status == IntakeStatus.SNOOZED }
        .groupBy { it.scheduledTime }
        .toSortedMap()
        .values
        .toList()
    var medicationToRestock by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = strings.home) },
                    label = { Text(strings.home) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMedications,
                    icon = { Icon(Icons.Default.Favorite, contentDescription = strings.medications) },
                    label = { Text(strings.medications) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = strings.history) },
                    label = { Text(strings.history) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                item {
                    HeaderCard()
                }

                item {
                    Text(
                        strings.dueNow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                if (pendingGroups.isEmpty()) {
                    item {
                        NoDueMedicationCard()
                    }
                } else {
                    items(pendingGroups, key = { group -> group.first().scheduledTime }) { group ->
                        PendingIntakeCard(
                            entries = group,
                            onTaken = { historyViewModel.confirmIntakes(group.map { it.id }) },
                            onStopAlarm = historyViewModel::stopAlarm
                        )
                    }
                }

                if (lowSupplyMedications.isNotEmpty()) {
                    item {
                        Text(
                            strings.lowSupplyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(lowSupplyMedications, key = { "supply-${it.id}" }) { medication ->
                        LowSupplyCard(
                            medication = medication,
                            onRestock = { medicationToRestock = medication }
                        )
                    }
                }

                if (upcomingIntakes.isNotEmpty()) {
                    item {
                        Text(
                            strings.nextIntakes,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(upcomingIntakes, key = { "next-${it.medication.id}-${it.triggerAtMillis}" }) { intake ->
                        NextIntakeCard(intake = intake)
                    }
                }

                if (medications.isEmpty()) {
                    item {
                        EmptyState()
                    }
                }
            }
        }
    }

    medicationToRestock?.let { medication ->
        RestockDialog(
            medication = medication,
            onDismiss = { medicationToRestock = null },
            onConfirm = { addedAmount ->
                viewModel.addStock(medication.id, addedAmount)
                medicationToRestock = null
            }
        )
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                strings.noReminders,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                strings.noRemindersBody,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun NoDueMedicationCard() {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            strings.noDueMedication,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PendingIntakeCard(
    entries: List<IntakeHistory>,
    onTaken: () -> Unit,
    onStopAlarm: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (entries.size == 1) entries.first().medicationName else "${entries.size} ${strings.medications}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            entries.forEach { entry ->
                Text(
                    "${entry.medicationName}: ${entry.dosage} ${entry.dosageUnit}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "${strings.scheduled}: ${formatHomeDate(entries.first().scheduledTime)}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTaken) {
                    Text(strings.markGroupTaken)
                }
                OutlinedButton(onClick = onStopAlarm) {
                    Text(strings.stopAlarm)
                }
            }
        }
    }
}

@Composable
private fun LowSupplyCard(
    medication: Medication,
    onRestock: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${medication.name} ${strings.lowSupplyBody}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    medication.stockLabel(strings),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(onClick = onRestock) {
                Text(strings.restock)
            }
        }
    }
}

@Composable
private fun NextIntakeCard(intake: UpcomingIntake) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                intake.medication.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${intake.medication.dosage} ${intake.medication.dosageUnit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${strings.scheduled}: ${intake.formattedTime()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatHomeDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
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
        text = {
            Text("${strings.confirmDeleteBody}\n\n${medication.name}")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
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
private fun RestockDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var stockText by remember(medication.id) { mutableStateOf("") }
    val stock = parseDecimalAmount(stockText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.restock}: ${medication.name}") },
        text = {
            OutlinedTextField(
                value = stockText,
                onValueChange = { stockText = filterDecimalInput(it) },
                label = { Text(strings.newStock) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { stock?.let(onConfirm) },
                enabled = stock != null
            ) {
                Text(strings.saveStock)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

private fun filterDecimalInput(value: String): String {
    var hasSeparator = false
    return value.filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !hasSeparator -> {
                hasSeparator = true
                true
            }
            else -> false
        }
    }
}

private fun parseDecimalAmount(value: String): Double? =
    value.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 }
