package com.weathermorning.alarm.presentation.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import com.weathermorning.alarm.R
import com.weathermorning.alarm.presentation.components.AppSection
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText
import com.weathermorning.alarm.presentation.theme.appBackgroundBrush

/** Remembers whether the first-run walkthrough has been shown. */
class OnboardingState(context: Context) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    /**
     * Pre-WM-128 builds stored the single alarm under flat keys. Their presence
     * means this install was already in use, so the walkthrough would be
     * greeting someone who has been granting these permissions for months.
     */
    private val upgradedFromOlderVersion =
        context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE).contains("hour")

    fun isComplete(): Boolean = upgradedFromOlderVersion || prefs.getBoolean(KEY_COMPLETE, false)

    fun markComplete() {
        prefs.edit { putBoolean(KEY_COMPLETE, true) }
    }

    private companion object {
        const val KEY_COMPLETE = "complete"
    }
}

/**
 * First-run walkthrough.
 *
 * The three permissions this app needs all live in system settings screens the
 * user would otherwise have to find alone, and on Samsung devices the battery
 * setting silently defers alarms, so it is called out here rather than left to
 * be discovered after a missed morning.
 */
@Composable
internal fun OnboardingDialog(
    exactAlarmAllowed: Boolean,
    notificationAllowed: Boolean,
    fullScreenIntentAllowed: Boolean,
    batteryUnrestricted: Boolean,
    onOpenExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenFullScreenIntentPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onFinish: () -> Unit,
) {
    Dialog(
        onDismissRequest = onFinish,
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
                item("intro") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.onboarding_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.onboarding_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item("exact-alarm") {
                    OnboardingStep(
                        title = stringResource(R.string.onboarding_exact_alarm_title),
                        body = stringResource(R.string.onboarding_exact_alarm_body),
                        granted = exactAlarmAllowed,
                        onOpen = onOpenExactAlarmPermission,
                    )
                }
                item("notifications") {
                    OnboardingStep(
                        title = stringResource(R.string.onboarding_notification_title),
                        body = stringResource(R.string.onboarding_notification_body),
                        granted = notificationAllowed,
                        onOpen = onRequestNotificationPermission,
                    )
                }
                item("full-screen") {
                    OnboardingStep(
                        title = stringResource(R.string.onboarding_full_screen_title),
                        body = stringResource(R.string.onboarding_full_screen_body),
                        granted = fullScreenIntentAllowed,
                        onOpen = onOpenFullScreenIntentPermission,
                    )
                }
                item("battery") {
                    OnboardingStep(
                        title = stringResource(R.string.onboarding_battery_title),
                        body = stringResource(R.string.onboarding_battery_body),
                        granted = batteryUnrestricted,
                        onOpen = onOpenBatterySettings,
                    )
                }
                item("finish") {
                    CompactButton(
                        label = stringResource(R.string.onboarding_finish),
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item("skip") {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    title: String,
    body: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    AppSection(title) {
        InfoText(body)
        if (granted) {
            InfoText(stringResource(R.string.onboarding_step_done))
        } else {
            CompactButton(
                label = stringResource(R.string.onboarding_step_open),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
