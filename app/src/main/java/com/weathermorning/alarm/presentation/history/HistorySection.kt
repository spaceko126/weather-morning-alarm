package com.weathermorning.alarm.presentation.history

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weathermorning.alarm.R
import com.weathermorning.alarm.domain.model.AlarmHistoryEntry
import com.weathermorning.alarm.domain.model.AlarmHistoryType
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.presentation.components.AppSection
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HistorySection(
    entries: List<AlarmHistoryEntry>,
    onOpen: () -> Unit,
) {
    AppSection(stringResource(R.string.history)) {
        CompactButton(
            label = stringResource(R.string.view_history),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
        )
        if (entries.isEmpty()) {
            InfoText(stringResource(R.string.no_history_yet))
        }
    }
}

@Composable
internal fun HistoryDialog(
    entries: List<AlarmHistoryEntry>,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.clear))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (entries.isEmpty()) {
                    InfoText(stringResource(R.string.no_history_yet))
                } else {
                    entries.forEach { entry ->
                        HistoryRow(entry)
                    }
                }
            }
        },
    )
}

@Composable
private fun HistoryRow(entry: AlarmHistoryEntry) {
    val time = remember(entry.occurredAt) {
        DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(entry.occurredAt)
    }
    val reason = historyReasonText(entry)
    val type = historyTypeText(entry.type)
    val weatherCondition = entry.weatherCondition?.let { weatherConditionText(it) }
    val reasonHasWeatherDetails = HistoryReasonDisplayParser.hasWeatherDetails(entry.reason)
    val weather = buildString {
        if (!reasonHasWeatherDetails) {
            weatherCondition?.let { append(" $it") }
            entry.precipitationProbability?.let { append(" ${it}%") }
            entry.precipitationAmountMm?.takeIf { it > 0.0 }?.let {
                append(" ${"%.1f".format(Locale.US, it)}mm")
            }
        }
    }
    InfoText("$time  $type\n${reason + weather}")
}

@Composable
private fun historyTypeText(type: AlarmHistoryType): String {
    return when (type) {
        AlarmHistoryType.PRECHECK -> stringResource(R.string.history_type_precheck)
        AlarmHistoryType.RING -> stringResource(R.string.history_type_ring)
        AlarmHistoryType.SKIP -> stringResource(R.string.history_type_skip)
        AlarmHistoryType.TEST -> stringResource(R.string.history_type_test)
        AlarmHistoryType.DIAGNOSTIC -> stringResource(R.string.history_type_diagnostic)
    }
}

@Composable
private fun weatherConditionText(condition: WeatherCondition): String {
    return when (condition) {
        WeatherCondition.CLEAR -> stringResource(R.string.condition_clear)
        WeatherCondition.RAIN -> stringResource(R.string.condition_rain)
        WeatherCondition.SNOW -> stringResource(R.string.condition_snow)
        WeatherCondition.SLEET -> stringResource(R.string.condition_sleet)
        WeatherCondition.UNKNOWN -> stringResource(R.string.condition_unknown)
    }
}

@Composable
private fun historyReasonText(entry: AlarmHistoryEntry): String {
    val display = if (entry.reasonCode != null) {
        HistoryReasonDisplayParser.parseCode(
            code = entry.reasonCode,
            condition = entry.reasonCondition,
            value1 = entry.reasonValue1,
            value2 = entry.reasonValue2,
        )
    } else {
        HistoryReasonDisplayParser.parse(entry.reason)
    }
    return when (display) {
        HistoryReasonDisplay.AlarmDisabled -> stringResource(R.string.reason_alarm_disabled)
        HistoryReasonDisplay.WeatherCheckFailedRing -> stringResource(R.string.reason_weather_check_failed_ring)
        HistoryReasonDisplay.WeatherCheckFailedSkip -> stringResource(R.string.reason_weather_check_failed_skip)
        HistoryReasonDisplay.RainForecast -> stringResource(R.string.reason_rain_forecast)
        HistoryReasonDisplay.SnowForecast -> stringResource(R.string.reason_snow_forecast)
        HistoryReasonDisplay.SleetForecast -> stringResource(R.string.reason_sleet_forecast)
        HistoryReasonDisplay.ReceiverTestAlarm -> stringResource(R.string.alarm_receiver_test_reason)
        HistoryReasonDisplay.WeatherConditionMet -> stringResource(R.string.alarm_default_reason)
        HistoryReasonDisplay.NoSavedDecisionFailRing -> stringResource(R.string.alarm_no_saved_decision_reason)
        HistoryReasonDisplay.WeatherConditionNotMet -> stringResource(R.string.alarm_weather_not_met_reason)
        is HistoryReasonDisplay.PrecipitationThreshold -> {
            stringResource(R.string.reason_precipitation_threshold, display.actual, display.threshold)
        }
        is HistoryReasonDisplay.NoTrigger -> {
            stringResource(
                R.string.reason_no_trigger,
                weatherConditionText(display.condition),
                display.probability,
            )
        }
        is HistoryReasonDisplay.Raw -> display.text
    }
}
