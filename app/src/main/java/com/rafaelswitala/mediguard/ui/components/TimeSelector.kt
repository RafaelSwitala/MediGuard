package com.rafaelswitala.mediguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Time selector component for selecting hours and minutes
 * Returns selected time as pair of (hour, minute)
 */
@Composable
fun TimeSelector(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    onTimeSelected: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var hour by remember { mutableStateOf(initialHour.toString().padStart(2, '0')) }
    var minute by remember { mutableStateOf(initialMinute.toString().padStart(2, '0')) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = hour,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.toIntOrNull() ?: -1) in 0..23) {
                    hour = newValue
                }
            },
            label = { Text("Hour") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            maxLines = 1
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = minute,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.toIntOrNull() ?: -1) in 0..59) {
                    minute = newValue
                }
            },
            label = { Text("Minute") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            maxLines = 1
        )

        Button(
            onClick = {
                val h = hour.toIntOrNull() ?: 0
                val m = minute.toIntOrNull() ?: 0
                onTimeSelected(h, m)
            }
        ) {
            Text("OK")
        }
    }
}