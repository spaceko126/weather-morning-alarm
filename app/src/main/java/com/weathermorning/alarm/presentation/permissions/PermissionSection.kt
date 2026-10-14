package com.weathermorning.alarm.presentation.permissions

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weathermorning.alarm.R
import com.weathermorning.alarm.presentation.components.AppSection
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText

@Composable
internal fun SupportSection(
    onRunAlarmTest: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    AppSection(stringResource(R.string.support_settings)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactButton(
                label = stringResource(R.string.test_alarm),
                onClick = onRunAlarmTest,
                modifier = Modifier.fillMaxWidth(),
            )
            InfoText(stringResource(R.string.test_alarm_summary))
            CompactButton(
                label = stringResource(R.string.manage_permissions),
                onClick = onOpenPermissions,
                modifier = Modifier.fillMaxWidth(),
            )
            CompactButton(
                label = stringResource(R.string.privacy_and_policy),
                onClick = onOpenPrivacy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PermissionDialog(
    exactAlarmAllowed: Boolean,
    notificationAllowed: Boolean,
    fullScreenIntentAllowed: Boolean,
    onOpenExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenFullScreenIntentPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val allowed = stringResource(R.string.allowed)
    val needsPermission = stringResource(R.string.needs_permission)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permissions)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoText(stringResource(R.string.exact_alarm_status, if (exactAlarmAllowed) allowed else needsPermission))
                if (!exactAlarmAllowed) {
                    CompactButton(
                        label = stringResource(R.string.open_alarm_permission),
                        onClick = onOpenExactAlarmPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                InfoText(stringResource(R.string.notifications_status, if (notificationAllowed) allowed else needsPermission))
                if (!notificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    CompactButton(
                        label = stringResource(R.string.request_notification_permission),
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    InfoText(stringResource(R.string.full_screen_alarm_status, if (fullScreenIntentAllowed) allowed else needsPermission))
                    if (!fullScreenIntentAllowed) {
                        CompactButton(
                            label = stringResource(R.string.open_full_screen_alarm_permission),
                            onClick = onOpenFullScreenIntentPermission,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    )
}

@Composable
internal fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_and_policy)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoText(stringResource(R.string.privacy_notice_body))
                InfoText(stringResource(R.string.privacy_policy_body))
            }
        },
    )
}
