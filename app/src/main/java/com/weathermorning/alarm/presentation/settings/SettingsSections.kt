package com.weathermorning.alarm.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.weathermorning.alarm.R
import com.weathermorning.alarm.domain.model.AlarmSoundName
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.FailPolicy
import com.weathermorning.alarm.domain.model.RainAmountThreshold
import com.weathermorning.alarm.domain.model.SNOOZE_MINUTE_OPTIONS
import com.weathermorning.alarm.domain.model.WeatherCheckWindow
import com.weathermorning.alarm.presentation.components.AppSection
import com.weathermorning.alarm.presentation.components.ChipFlow
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText
import com.weathermorning.alarm.presentation.components.SelectableChip
import com.weathermorning.alarm.presentation.components.SettingSwitch
import com.weathermorning.alarm.presentation.components.StepperRow
import java.time.DayOfWeek

@Composable
internal fun SettingsWarningsSection(
    settings: AlarmSettings,
) {
    val warningNoRepeatDays = stringResource(R.string.warning_no_repeat_days)
    val warningPrecipitationZero = stringResource(R.string.warning_precipitation_zero)
    val warningDirectTriggersOff = stringResource(R.string.warning_direct_triggers_off)
    val warnings = buildList {
        if (!settings.enabled) return@buildList
        if (settings.repeatDays.isEmpty()) {
            add(warningNoRepeatDays)
        }
        if (settings.precipitationThreshold == 0) {
            add(warningPrecipitationZero)
        }
        if (!settings.triggerRain && !settings.triggerSnow) {
            add(warningDirectTriggersOff)
        }
    }

    if (warnings.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        warnings.forEach { warning ->
            InfoText(
                text = warning,
                isError = true,
            )
        }
    }
}

@Composable
internal fun AlarmTimeSection(
    settings: AlarmSettings,
    onSetTimeClick: () -> Unit,
) {
    AppSection(stringResource(R.string.alarm_edit)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBubble(painterResource(R.drawable.ic_alarm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.alarm_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%02d:%02d".format(settings.alarmTime.hour, settings.alarmTime.minute),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.time_wheel_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CompactButton(
                label = stringResource(R.string.set_time),
                onClick = onSetTimeClick,
            )
        }
    }
}

@Composable
internal fun RepeatDaysSection(
    settings: AlarmSettings,
    onSettingsChange: (AlarmSettings) -> Unit,
) {
    AppSection(stringResource(R.string.repeat_days)) {
        OptionHeading(
            icon = painterResource(R.drawable.ic_calendar_days),
            title = stringResource(R.string.repeat_days),
            body = repeatSummary(settings),
        )
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        ChipFlow {
            SelectableChip(
                label = stringResource(R.string.every_day),
                selected = settings.repeatDays.size == 7 && !settings.excludePublicHolidays,
                onClick = {
                    onSettingsChange(
                        settings.copy(
                            repeatDays = DayOfWeek.entries.toSet(),
                            excludePublicHolidays = false,
                        ),
                    )
                },
            )
            SelectableChip(
                label = stringResource(R.string.weekdays_only),
                selected = settings.repeatDays == weekdays && !settings.excludePublicHolidays,
                onClick = {
                    onSettingsChange(
                        settings.copy(
                            repeatDays = weekdays,
                            excludePublicHolidays = false,
                        ),
                    )
                },
            )
            SelectableChip(
                label = stringResource(R.string.weekdays_except_holidays),
                selected = settings.repeatDays == weekdays && settings.excludePublicHolidays,
                onClick = {
                    onSettingsChange(
                        settings.copy(
                            repeatDays = weekdays,
                            excludePublicHolidays = true,
                        ),
                    )
                },
            )
        }
        if (settings.excludePublicHolidays) {
            InfoText(stringResource(R.string.holiday_exclusion_note))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            samsungDayOrder().forEach { day ->
                DayButton(
                    label = day.shortName(),
                    selected = settings.repeatDays.contains(day),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val nextDays = if (settings.repeatDays.contains(day)) {
                            settings.repeatDays - day
                        } else {
                            settings.repeatDays + day
                        }
                        onSettingsChange(
                            settings.copy(
                                repeatDays = nextDays,
                                excludePublicHolidays = false,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun AlarmBehaviorSection(
    settings: AlarmSettings,
    onSettingsChange: (AlarmSettings) -> Unit,
    onChooseAlarmSound: () -> Unit,
) {
    AppSection(stringResource(R.string.alarm_options)) {
        OptionToggleRow(
            icon = painterResource(R.drawable.ic_volume),
            title = stringResource(R.string.alarm_sound),
            body = if (settings.alarmSoundEnabled) {
                settings.alarmSoundDisplayName()
            } else {
                stringResource(R.string.alarm_off)
            },
            checked = settings.alarmSoundEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(alarmSoundEnabled = it)) },
        )
        if (settings.alarmSoundEnabled) {
            // The chosen tone is already named in the row above; one chip that
            // opens the picker is enough.
            ChipFlow {
                SelectableChip(
                    label = stringResource(R.string.choose_alarm_sound),
                    selected = false,
                    onClick = onChooseAlarmSound,
                )
            }
        }

        OptionToggleRow(
            icon = painterResource(R.drawable.ic_vibration),
            title = stringResource(R.string.vibration),
            body = stringResource(if (settings.vibrationEnabled) R.string.alarm_on else R.string.alarm_off),
            checked = settings.vibrationEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(vibrationEnabled = it)) },
        )

        OptionToggleRow(
            icon = painterResource(R.drawable.ic_snooze),
            title = stringResource(R.string.snooze),
            body = if (settings.snoozeEnabled) {
                stringResource(
                    R.string.snooze_summary,
                    settings.snoozeMinutes,
                    settings.snoozeMaxCount,
                )
            } else {
                stringResource(R.string.alarm_off)
            },
            checked = settings.snoozeEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(snoozeEnabled = it)) },
        )
        if (settings.snoozeEnabled) {
            ChipFlow {
                SNOOZE_MINUTE_OPTIONS.forEach { minutes ->
                    SelectableChip(
                        label = stringResource(R.string.snooze_minutes, minutes),
                        selected = settings.snoozeMinutes == minutes,
                        onClick = { onSettingsChange(settings.copy(snoozeMinutes = minutes)) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun WeatherTriggersSection(
    settings: AlarmSettings,
    onSettingsChange: (AlarmSettings) -> Unit,
) {
    AppSection(stringResource(R.string.weather_triggers)) {
        SettingSwitch(
            label = stringResource(R.string.rain),
            checked = settings.triggerRain,
            onChange = { onSettingsChange(settings.copy(triggerRain = it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.snow),
            checked = settings.triggerSnow,
            onChange = { onSettingsChange(settings.copy(triggerSnow = it)) },
        )
        StepperRow(
            label = stringResource(R.string.precipitation),
            value = "${settings.precipitationThreshold}%",
            onMinus = {
                val next = (settings.precipitationThreshold - 10).coerceAtLeast(0)
                onSettingsChange(settings.copy(precipitationThreshold = next))
            },
            onPlus = {
                val next = (settings.precipitationThreshold + 10).coerceAtMost(100)
                onSettingsChange(settings.copy(precipitationThreshold = next))
            },
        )
        OptionHeading(
            icon = painterResource(R.drawable.ic_rain),
            title = stringResource(R.string.rain_amount_threshold),
            body = rainAmountSummary(settings.rainAmountThreshold),
        )
        ChipFlow {
            RainAmountThreshold.entries.forEach { threshold ->
                SelectableChip(
                    label = rainAmountLabel(threshold),
                    selected = settings.rainAmountThreshold == threshold,
                    onClick = {
                        onSettingsChange(settings.copy(rainAmountThreshold = threshold))
                    },
                )
            }
        }
        OptionHeading(
            icon = painterResource(R.drawable.ic_weather_cloud),
            title = stringResource(R.string.weather_check_window),
            body = weatherWindowSummary(settings.weatherCheckWindow),
        )
        ChipFlow {
            SelectableChip(
                label = stringResource(R.string.weather_window_alarm_time),
                selected = settings.weatherCheckWindow == WeatherCheckWindow.ALARM_TIME,
                onClick = {
                    onSettingsChange(settings.copy(weatherCheckWindow = WeatherCheckWindow.ALARM_TIME))
                },
            )
            SelectableChip(
                label = stringResource(R.string.weather_window_commute),
                selected = settings.weatherCheckWindow == WeatherCheckWindow.COMMUTE_WINDOW,
                onClick = {
                    onSettingsChange(settings.copy(weatherCheckWindow = WeatherCheckWindow.COMMUTE_WINDOW))
                },
            )
        }
    }
}

@Composable
private fun rainAmountLabel(threshold: RainAmountThreshold): String {
    return when (threshold) {
        RainAmountThreshold.NONE -> stringResource(R.string.rain_amount_none)
        RainAmountThreshold.DRIZZLE -> stringResource(R.string.rain_amount_drizzle)
        RainAmountThreshold.LIGHT -> stringResource(R.string.rain_amount_light)
        RainAmountThreshold.HEAVY -> stringResource(R.string.rain_amount_heavy)
    }
}

@Composable
private fun rainAmountSummary(threshold: RainAmountThreshold): String {
    return when (threshold) {
        RainAmountThreshold.NONE -> stringResource(R.string.rain_amount_none_summary)
        RainAmountThreshold.DRIZZLE -> stringResource(R.string.rain_amount_drizzle_summary)
        RainAmountThreshold.LIGHT -> stringResource(R.string.rain_amount_light_summary)
        RainAmountThreshold.HEAVY -> stringResource(R.string.rain_amount_heavy_summary)
    }
}

@Composable
private fun OptionHeading(
    icon: Painter,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // These lines carry the only explanation of what the option
                // below actually does, so they must not be clipped.
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OptionToggleRow(
    icon: Painter,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DayButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun IconBubble(icon: Painter) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun AlarmSoundName.label(): String {
    return when (this) {
        AlarmSoundName.DEFAULT -> stringResource(R.string.alarm_sound_default)
        AlarmSoundName.SOFT_RAIN -> stringResource(R.string.alarm_sound_soft_rain)
        AlarmSoundName.BRIGHT_BELL -> stringResource(R.string.alarm_sound_bright_bell)
    }
}

@Composable
private fun AlarmSettings.alarmSoundDisplayName(): String {
    return alarmSoundTitle.ifBlank { alarmSoundName.label() }
}

@Composable
internal fun repeatDaysLabel(settings: AlarmSettings): String = repeatSummary(settings)

@Composable
private fun repeatSummary(settings: AlarmSettings): String {
    val days = when {
        settings.repeatDays.size == 7 -> stringResource(R.string.every_day)
        settings.repeatDays == weekdays() -> stringResource(R.string.weekdays_only)
        settings.repeatDays.isEmpty() -> return stringResource(R.string.warning_no_repeat_days)
        // map is inline so shortName() stays in a composable context;
        // joinToString would not be.
        else -> samsungDayOrder()
            .filter { settings.repeatDays.contains(it) }
            .map { it.shortName() }
            .joinToString(" ")
    }
    // Holiday exclusion applies to any day set, so it is appended rather than
    // handled as one special case that other sets silently drop.
    if (!settings.excludePublicHolidays) return days
    return stringResource(R.string.repeat_summary_except_holidays, days)
}

private fun weekdays(): Set<DayOfWeek> {
    return setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )
}

private fun samsungDayOrder(): List<DayOfWeek> {
    return listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )
}

@Composable
private fun DayOfWeek.shortName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> stringResource(R.string.weekday_monday_short)
        DayOfWeek.TUESDAY -> stringResource(R.string.weekday_tuesday_short)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.weekday_wednesday_short)
        DayOfWeek.THURSDAY -> stringResource(R.string.weekday_thursday_short)
        DayOfWeek.FRIDAY -> stringResource(R.string.weekday_friday_short)
        DayOfWeek.SATURDAY -> stringResource(R.string.weekday_saturday_short)
        DayOfWeek.SUNDAY -> stringResource(R.string.weekday_sunday_short)
    }
}

@Composable
private fun weatherWindowSummary(window: WeatherCheckWindow): String {
    return when (window) {
        WeatherCheckWindow.ALARM_TIME -> stringResource(R.string.weather_window_alarm_time_summary)
        WeatherCheckWindow.COMMUTE_WINDOW -> stringResource(R.string.weather_window_commute_summary)
    }
}

@Composable
internal fun FailPolicySection(
    settings: AlarmSettings,
    onSettingsChange: (AlarmSettings) -> Unit,
) {
    AppSection(stringResource(R.string.fail_policy)) {
        InfoText(stringResource(R.string.fail_policy_summary))
        val ringLabel = stringResource(R.string.ring)
        val skipLabel = stringResource(R.string.skip)
        ChipFlow {
            SelectableChip(
                label = ringLabel,
                selected = settings.failPolicy == FailPolicy.RING,
                onClick = { onSettingsChange(settings.copy(failPolicy = FailPolicy.RING)) },
            )
            SelectableChip(
                label = skipLabel,
                selected = settings.failPolicy == FailPolicy.SKIP,
                onClick = { onSettingsChange(settings.copy(failPolicy = FailPolicy.SKIP)) },
            )
        }
    }
}
