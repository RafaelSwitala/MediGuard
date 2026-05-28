package com.rafaelswitala.mediguard.ui.screens

/**
 * Einnahmeverlauf mit Adhäranzquote in Prozenten und Bestätigung ausstehender Intakes.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.ui.components.StandardSectionCard
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing intake history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateMedications: () -> Unit = {},
    onNavigateSettings: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val intakeHistory by viewModel.allIntakeHistory.collectAsState()
    val adherenceRate by viewModel.adherenceRate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.history) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = strings.home) },
                    label = { Text(strings.home) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateMedications,
                    icon = { Icon(Icons.Default.Favorite, contentDescription = strings.medications) },
                    label = { Text(strings.medications) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
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
            if (intakeHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        strings.noHistory,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    item {
                        Text(
                            "${strings.adherence}: $adherenceRate%",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    items(intakeHistory) { entry ->
                        StandardSectionCard(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    entry.medicationName,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    "${entry.dosage} ${entry.dosageUnit}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${strings.status}: ${entry.status.label(strings)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${strings.scheduled}: ${formatDate(entry.scheduledTime)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                entry.actualIntakeTime?.let { actual ->
                                    Text(
                                        "${strings.taken}: ${formatDate(actual)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            if (entry.status == IntakeStatus.PENDING || entry.status == IntakeStatus.SNOOZED) {
                                Button(
                                    onClick = { viewModel.confirmIntake(entry.id) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(strings.takenNow)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
