package com.weathermorning.alarm.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.weathermorning.alarm.R
import com.weathermorning.alarm.alarm.AlarmReceiver
import com.weathermorning.alarm.alarm.AlarmService
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.presentation.theme.WeatherMorningTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import org.json.JSONArray

class AlarmActivity : ComponentActivity() {
    private lateinit var settings: AlarmSettings

    /** The alarm can also be stopped or snoozed from its notification. */
    private val alarmHandledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LocalAlarmSettingsStore(this)
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
        settings = alarmId.takeIf { it > 0L }?.let(store::load)
            ?: store.loadAll().firstOrNull()
            ?: AlarmSettings.Default
        ContextCompat.registerReceiver(
            this,
            alarmHandledReceiver,
            IntentFilter(AlarmService.ACTION_ALARM_HANDLED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        val reason = intent.getStringExtra(EXTRA_REASON) ?: getString(R.string.alarm_default_reason)
        val weatherSummary = intent.toAlarmWeatherSummary(this)
        val snoozeCount = intent.getIntExtra(AlarmService.EXTRA_SNOOZE_COUNT, 0)
        setContent {
            AlarmScreen(
                reason = reason,
                weatherSummary = weatherSummary,
                snoozeRemaining = (settings.snoozeMaxCount - snoozeCount).coerceAtLeast(0),
                snoozeMinutes = settings.snoozeMinutes,
                canSnooze = settings.snoozeEnabled && snoozeCount < settings.snoozeMaxCount,
                onSnooze = { sendToAlarmService(AlarmService.ACTION_SNOOZE, keepPayload = true) },
                onDismiss = { sendToAlarmService(AlarmService.ACTION_STOP, keepPayload = false) },
            )
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(alarmHandledReceiver) }
        super.onDestroy()
    }

    /**
     * Sound and vibration belong to AlarmService, so stopping and snoozing are
     * commands to it rather than local state changes.
     */
    private fun sendToAlarmService(action: String, keepPayload: Boolean) {
        val serviceIntent = Intent(this, AlarmService::class.java)
            .setAction(action)
            .apply { if (keepPayload) intent.extras?.let(::putExtras) }
        runCatching { ContextCompat.startForegroundService(this, serviceIntent) }
        finish()
    }

    companion object {
        const val EXTRA_REASON = "reason"
        const val EXTRA_WEATHER_CONDITION = "weather_condition"
        const val EXTRA_PRECIPITATION_PROBABILITY = "precipitation_probability"
        const val EXTRA_PRECIPITATION_AMOUNT_MM = "precipitation_amount_mm"
        const val EXTRA_WEATHER_SOURCE = "weather_source"
        const val EXTRA_TARGET_TIME = "target_time"
        const val EXTRA_FORECAST_SNAPSHOTS = "forecast_snapshots"
    }
}

@Composable
private fun AlarmScreen(
    reason: String,
    weatherSummary: AlarmWeatherSummary?,
    snoozeRemaining: Int,
    snoozeMinutes: Int,
    canSnooze: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Always dark: this screen opens in a dark bedroom.
    WeatherMorningTheme(darkTheme = true) {
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000L)
                now = LocalTime.now()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Everything above the buttons scrolls, so Stop stays reachable on
            // a short screen or with a large system font.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(
                        R.string.alarm_today_condition,
                        weatherSummary?.conditionLabel ?: stringResource(R.string.alarm_test_today_condition),
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                AlarmInfoCard(
                    weatherSummary = weatherSummary,
                    reason = reason,
                )
            }

            if (canSnooze) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.snooze_action, snoozeMinutes, snoozeRemaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(0.62f)
                    .height(64.dp),
            ) {
                Text(
                    text = stringResource(R.string.stop),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AlarmInfoCard(
    weatherSummary: AlarmWeatherSummary?,
    reason: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.alarm_current_weather),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (weatherSummary == null) {
                Text(
                    text = stringResource(R.string.alarm_test_weather_summary),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.alarm_hourly_forecast),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                EmptyHourlyForecastRow()
            } else {
                WeatherMetricRow(
                    label = stringResource(R.string.alarm_metric_condition),
                    value = weatherSummary.conditionLabel,
                )
                WeatherMetricRow(
                    label = stringResource(R.string.alarm_metric_precipitation),
                    value = stringResource(
                        R.string.alarm_precipitation_value,
                        weatherSummary.precipitationProbability,
                    ),
                )
                WeatherMetricRow(
                    label = stringResource(R.string.alarm_metric_amount),
                    value = stringResource(
                        R.string.alarm_precipitation_amount_value,
                        weatherSummary.precipitationAmountText,
                    ),
                )
                Text(
                    text = stringResource(R.string.alarm_hourly_forecast),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                weatherSummary.forecastRows
                    .ifEmpty { listOf(weatherSummary.toForecastRow()) }
                    .take(MAX_VISIBLE_FORECAST_ROWS)
                    .forEach { forecastRow ->
                        HourlyForecastRow(forecastRow = forecastRow)
                    }
            }
            Text(
                text = stringResource(R.string.alarm_reason_summary, reason),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyHourlyForecastRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = stringResource(R.string.alarm_forecast_unavailable),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeatherMetricRow(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun HourlyForecastRow(forecastRow: AlarmForecastRow) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = forecastRow.targetTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = forecastRow.conditionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        R.string.alarm_precipitation_value,
                        forecastRow.precipitationProbability,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.alarm_precipitation_amount_value,
                        forecastRow.precipitationAmountText,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private data class AlarmWeatherSummary(
    val conditionLabel: String,
    val precipitationProbability: Int,
    val precipitationAmountText: String,
    val source: String,
    val targetTime: String,
    val forecastRows: List<AlarmForecastRow>,
)

private data class AlarmForecastRow(
    val targetTime: String,
    val conditionLabel: String,
    val precipitationProbability: Int,
    val precipitationAmountText: String,
)

private fun android.content.Intent.toAlarmWeatherSummary(context: android.content.Context): AlarmWeatherSummary? {
    val condition = getStringExtra(AlarmActivity.EXTRA_WEATHER_CONDITION) ?: return null
    val probability = getIntExtra(AlarmActivity.EXTRA_PRECIPITATION_PROBABILITY, -1)
    if (probability < 0) return null
    val amount = getDoubleExtra(AlarmActivity.EXTRA_PRECIPITATION_AMOUNT_MM, 0.0)
    val source = getStringExtra(AlarmActivity.EXTRA_WEATHER_SOURCE).orEmpty().ifBlank { "-" }
    val targetTime = getStringExtra(AlarmActivity.EXTRA_TARGET_TIME).orEmpty().ifBlank { "--:--" }
    val fallbackRow = AlarmForecastRow(
        targetTime = targetTime,
        conditionLabel = condition.toWeatherConditionLabel(context),
        precipitationProbability = probability,
        precipitationAmountText = String.format(Locale.US, "%.1f", amount),
    )
    return AlarmWeatherSummary(
        conditionLabel = condition.toWeatherConditionLabel(context),
        precipitationProbability = probability,
        precipitationAmountText = String.format(Locale.US, "%.1f", amount),
        source = source,
        targetTime = targetTime,
        forecastRows = getStringExtra(AlarmActivity.EXTRA_FORECAST_SNAPSHOTS)
            ?.toForecastRows(context)
            .orEmpty()
            .ifEmpty { listOf(fallbackRow) },
    )
}

private fun AlarmWeatherSummary.toForecastRow(): AlarmForecastRow {
    return AlarmForecastRow(
        targetTime = targetTime,
        conditionLabel = conditionLabel,
        precipitationProbability = precipitationProbability,
        precipitationAmountText = precipitationAmountText,
    )
}

private fun String.toForecastRows(context: android.content.Context): List<AlarmForecastRow> {
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    AlarmForecastRow(
                        targetTime = item.optString("targetTime", "--:--"),
                        conditionLabel = item.optString("condition")
                            .toWeatherConditionLabel(context),
                        precipitationProbability = item.optInt("precipitationProbability", 0),
                        precipitationAmountText = String.format(
                            Locale.US,
                            "%.1f",
                            item.optDouble("precipitationAmountMm", 0.0),
                        ),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun String.toWeatherConditionLabel(context: android.content.Context): String {
    return when (runCatching { WeatherCondition.valueOf(this) }.getOrNull()) {
        WeatherCondition.CLEAR -> context.getString(R.string.condition_clear)
        WeatherCondition.RAIN -> context.getString(R.string.condition_rain)
        WeatherCondition.SNOW -> context.getString(R.string.condition_snow)
        WeatherCondition.SLEET -> context.getString(R.string.condition_sleet)
        WeatherCondition.UNKNOWN, null -> context.getString(R.string.condition_unknown)
    }
}

private const val MAX_VISIBLE_FORECAST_ROWS = 3
