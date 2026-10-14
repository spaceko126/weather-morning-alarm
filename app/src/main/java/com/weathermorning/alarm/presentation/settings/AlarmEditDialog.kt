package com.weathermorning.alarm.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weathermorning.alarm.R
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.presentation.components.AppSection
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText
import com.weathermorning.alarm.presentation.location.localizedLocationName
import com.weathermorning.alarm.presentation.theme.appBackgroundBrush
import java.time.LocalTime

/**
 * Full-screen editor for one alarm.
 *
 * Edits land on a draft the caller holds, so leaving without saving really does
 * leave the alarm as it was.
 */
@Composable
internal fun AlarmEditDialog(
    draft: AlarmSettings,
    saved: AlarmSettings?,
    onDraftChange: (AlarmSettings) -> Unit,
    onSetTimeClick: () -> Unit,
    onChooseAlarmSound: () -> Unit,
    onOpenLocation: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
    showTimePicker: Boolean,
    onTimePickerDismiss: () -> Unit,
    onTimeConfirm: (LocalTime) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val hasChanges = saved != null && saved != draft

    fun leave() {
        if (hasChanges) confirmDiscard = true else onDiscard()
    }

    Dialog(
        onDismissRequest = ::leave,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackgroundBrush())
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item("bar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = ::leave) {
                            Text(stringResource(R.string.discard_and_close))
                        }
                        Text(
                            text = stringResource(R.string.alarm_edit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = { confirmDelete = true }) {
                            Text(
                                text = stringResource(R.string.delete_alarm),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                item("label") {
                    AppSection(stringResource(R.string.alarm_label)) {
                        TextField(
                            value = draft.label,
                            onValueChange = { onDraftChange(draft.copy(label = it.take(30))) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.alarm_label_hint)) },
                            singleLine = true,
                        )
                    }
                }
                item("warnings") {
                    SettingsWarningsSection(settings = draft)
                }
                item("time") {
                    AlarmTimeSection(
                        settings = draft,
                        onSetTimeClick = onSetTimeClick,
                    )
                }
                item("location") {
                    AppSection(stringResource(R.string.set_location)) {
                        InfoText(localizedLocationName(draft.locationName))
                        CompactButton(
                            label = stringResource(R.string.set_location),
                            onClick = onOpenLocation,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item("repeat") {
                    RepeatDaysSection(settings = draft, onSettingsChange = onDraftChange)
                }
                item("behavior") {
                    AlarmBehaviorSection(
                        settings = draft,
                        onSettingsChange = onDraftChange,
                        onChooseAlarmSound = onChooseAlarmSound,
                    )
                }
                item("triggers") {
                    WeatherTriggersSection(settings = draft, onSettingsChange = onDraftChange)
                }
                item("fail-policy") {
                    FailPolicySection(settings = draft, onSettingsChange = onDraftChange)
                }
                item("save") {
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        TimeWheelDialog(
            initialTime = draft.alarmTime,
            onConfirm = onTimeConfirm,
            onDismiss = onTimePickerDismiss,
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onDiscard()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.discard_changes_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_alarm)) },
            text = { Text(stringResource(R.string.delete_alarm_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete_alarm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
