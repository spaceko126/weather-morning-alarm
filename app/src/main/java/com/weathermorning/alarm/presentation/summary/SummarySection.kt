package com.weathermorning.alarm.presentation.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weathermorning.alarm.R
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.RainAmountThreshold
import com.weathermorning.alarm.domain.model.WeatherCheckWindow
import com.weathermorning.alarm.presentation.location.localizedLocationName
import com.weathermorning.alarm.presentation.settings.repeatDaysLabel

/**
 * One alarm on the home list.
 *
 * The switch is the only control: everything else is read-only, and tapping the
 * card opens the editor. Earlier this row mixed a toggle, an edit button and a
 * read-only label as three identical-looking pills, so two of them looked
 * tappable when only one was.
 */
@Composable
internal fun SummarySection(
    settings: AlarmSettings,
    onToggleAlarm: () -> Unit,
    onOpenEdit: () -> Unit,
) {
    val windowText = when (settings.weatherCheckWindow) {
        WeatherCheckWindow.ALARM_TIME -> stringResource(R.string.weather_window_alarm_time)
        WeatherCheckWindow.COMMUTE_WINDOW -> stringResource(R.string.weather_window_commute)
    }
    val dimmed = if (settings.enabled) 1f else 0.55f

    Surface(
        onClick = onOpenEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F766E).copy(alpha = dimmed),
                            Color(0xFF2563EB).copy(alpha = dimmed),
                            Color(0xFFFFB86B).copy(alpha = dimmed),
                        ),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = settings.label.ifBlank { stringResource(R.string.weather_alarm_brief) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = localizedLocationName(settings.locationName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { onToggleAlarm() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0F766E),
                        checkedTrackColor = Color.White,
                        checkedBorderColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.45f),
                    ),
                )
            }

            Text(
                text = "%02d:%02d".format(settings.alarmTime.hour, settings.alarmTime.minute),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
            )

            FactLine(
                iconRes = R.drawable.ic_calendar_days,
                text = repeatDaysLabel(settings),
            )
            FactLine(
                iconRes = R.drawable.ic_weather_cloud,
                text = windowText,
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.18f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rain),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = ringConditionText(settings),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // The card itself is the edit affordance; this says so quietly.
            Text(
                text = stringResource(R.string.tap_to_edit),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.End,
            )
        }
    }
}

/** Read-only fact: an icon and text, deliberately not shaped like a control. */
@Composable
private fun FactLine(iconRes: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One sentence saying exactly what makes this alarm ring. */
@Composable
private fun ringConditionText(settings: AlarmSettings): String {
    val trigger = when {
        settings.triggerRain && settings.triggerSnow -> stringResource(R.string.trigger_rain_snow)
        settings.triggerRain -> stringResource(R.string.trigger_rain_only)
        settings.triggerSnow -> stringResource(R.string.trigger_snow_only)
        else -> null
    }
    // DecideAlarmUseCase rings only when probability AND amount both match;
    // rain/snow selections only label the reason.
    val rule = if (settings.rainAmountThreshold == RainAmountThreshold.NONE) {
        stringResource(R.string.summary_rule_precipitation_only, settings.precipitationThreshold)
    } else {
        stringResource(
            R.string.summary_rule_with_amount,
            settings.precipitationThreshold,
            settings.rainAmountThreshold.minMmPerHour.toBigDecimal().stripTrailingZeros().toPlainString(),
        )
    }
    return if (trigger == null) rule else stringResource(R.string.summary_rule_with_weather, rule, trigger)
}
