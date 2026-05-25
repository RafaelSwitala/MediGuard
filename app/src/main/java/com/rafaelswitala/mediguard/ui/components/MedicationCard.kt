package com.rafaelswitala.mediguard.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.ui.theme.IntakeGroupColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MedicationCard(
    medication: Medication,
    onView: (Long) -> Unit = {},
    onLongPress: (Long) -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val groupColor = IntakeGroupColors.colorForGroupId(medication.intakeGroupId)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onSelectionChange(!selected)
                    } else {
                        onView(medication.id)
                    }
                },
                onLongClick = {
                    onLongPress(medication.id)
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { checked -> onSelectionChange(checked) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            groupColor?.let { color ->
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .padding(horizontal = 4.dp, vertical = 32.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${medication.dosage} ${medication.dosageUnit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (medication.doseQuantity > 1) {
                    Text(
                        text = "${strings.doseQuantity}: ${medication.doseQuantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                medication.durationDays?.let { days ->
                    Text(
                        text = "${strings.durationDays}: $days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (medication.remainingDoses != null || medication.remainingVolumeMl != null) {
                    Text(
                        text = medication.stockLabel(strings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (medication.description.isNotEmpty()) {
                    Text(
                        text = medication.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
