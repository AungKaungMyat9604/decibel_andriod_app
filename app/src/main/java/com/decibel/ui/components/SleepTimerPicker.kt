package com.decibel.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibel.player.SleepTimerOption
import com.decibel.player.SleepTimerState
import com.decibel.player.formatSleepRemaining

@Composable
fun SleepTimerTopBarAction(
    sleepTimer: SleepTimerState,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showDialog by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(end = 4.dp),
    ) {
        if (sleepTimer.isActive) {
            Text(
                text = formatSleepRemaining(sleepTimer.remainingMs),
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        IconButton(onClick = { showDialog = true }) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = if (sleepTimer.isActive) {
                    "Sleep timer ${formatSleepRemaining(sleepTimer.remainingMs)}"
                } else {
                    "Sleep timer"
                },
                tint = if (sleepTimer.isActive) colors.primary else colors.onSurfaceVariant,
            )
        }
    }
    if (showDialog) {
        SleepTimerPickerDialog(
            sleepTimer = sleepTimer,
            onDismiss = { showDialog = false },
            onSelect = { option ->
                onSetSleepTimer(option)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerPickerDialog(
    sleepTimer: SleepTimerState,
    onDismiss: () -> Unit,
    onSelect: (SleepTimerOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when {
        sleepTimer.isActive -> sleepTimer.option.label
        else -> SleepTimerOption.Off.label
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (sleepTimer.isActive) {
                        "Pauses playback and lets the screen sleep when time is up. Remaining ${formatSleepRemaining(sleepTimer.remainingMs)}."
                    } else {
                        "Pauses playback and lets the screen sleep when time is up."
                    },
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedLabel,
                        onValueChange = {},
                        label = { Text("Duration") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        SleepTimerOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
