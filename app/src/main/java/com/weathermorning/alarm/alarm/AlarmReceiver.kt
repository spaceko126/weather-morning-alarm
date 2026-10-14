package com.weathermorning.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.weathermorning.alarm.R
import com.weathermorning.alarm.data.local.LocalAlarmDecisionStore
import com.weathermorning.alarm.data.local.LocalAlarmHistoryStore
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.domain.model.AlarmDecision
import com.weathermorning.alarm.domain.model.AlarmHistoryType
import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.FailPolicy
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.presentation.AlarmActivity
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val isTest = intent?.getBooleanExtra(EXTRA_TEST_ALARM, false) == true
        val isSnooze = intent?.getBooleanExtra(EXTRA_SNOOZE_ALARM, false) == true
        val store = LocalAlarmSettingsStore(context)
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, 0L)?.takeIf { it > 0L }
        val settings = alarmId?.let(store::load)
            ?: store.loadAll().firstOrNull()
            ?: return
        val decision = if (isTest || isSnooze) {
            null
        } else {
            LocalAlarmDecisionStore(context, settings.id).load()?.takeIf(::isFresh)
        }

        val shouldRing = isTest || isSnooze ||
            (decision?.shouldRing ?: (settings.failPolicy == FailPolicy.RING))

        val reason = when {
            isTest -> context.getString(R.string.alarm_receiver_test_reason)
            isSnooze -> intent?.getStringExtra(AlarmActivity.EXTRA_REASON)
                ?: context.getString(R.string.alarm_default_reason)
            shouldRing -> decision?.reason
                ?: context.getString(R.string.alarm_no_saved_decision_reason)
            else -> decision?.reason
                ?: context.getString(R.string.alarm_weather_not_met_reason)
        }

        if (shouldRing) {
            startAlarmService(
                context = context,
                reason = reason,
                alarmId = settings.id,
                snoozeCount = intent?.getIntExtra(AlarmService.EXTRA_SNOOZE_COUNT, 0) ?: 0,
                weatherSnapshot = if (isTest) null else decision?.weatherSnapshot,
                weatherSnapshots = if (isTest) emptyList() else decision?.weatherSnapshots.orEmpty(),
                carriedForecastJson = intent
                    ?.getStringExtra(AlarmActivity.EXTRA_FORECAST_SNAPSHOTS)
                    ?.takeIf { isSnooze },
                carriedExtras = if (isSnooze) intent else null,
            )
        }

        // A snooze re-ring is the same alarm run: it must not roll the schedule
        // forward or overwrite the saved decision.
        if (!isTest && !isSnooze) {
            AndroidAlarmScheduler(context).scheduleNext(settings)
        }

        appendHistoryAsync(
            context = context,
            type = when {
                isTest -> AlarmHistoryType.TEST
                shouldRing -> AlarmHistoryType.RING
                else -> AlarmHistoryType.SKIP
            },
            reason = reason,
            decision = decision,
            reasonCode = when {
                isTest -> AlarmReasonCode.RECEIVER_TEST_ALARM
                shouldRing -> AlarmReasonCode.NO_SAVED_DECISION_FAIL_RING
                else -> AlarmReasonCode.WEATHER_CONDITION_NOT_MET
            },
        )
    }

    /**
     * Room writes are too slow for a broadcast's main-thread budget, so they
     * finish on an IO dispatcher while [goAsync] holds the receiver alive.
     */
    private fun appendHistoryAsync(
        context: Context,
        type: AlarmHistoryType,
        reason: String,
        decision: AlarmDecision?,
        reasonCode: AlarmReasonCode,
    ) {
        // Null when the receiver was invoked directly instead of dispatched,
        // which the debug QA path does.
        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                LocalAlarmHistoryStore(appContext).append(
                    type = type,
                    reason = reason,
                    decision = decision,
                    reasonCode = reasonCode,
                )
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun startAlarmService(
        context: Context,
        reason: String,
        alarmId: Long,
        snoozeCount: Int,
        weatherSnapshot: WeatherSnapshot?,
        weatherSnapshots: List<WeatherSnapshot>,
        carriedForecastJson: String?,
        carriedExtras: Intent?,
    ) {
        val forecastJson = carriedForecastJson
            ?: weatherSnapshots
                .ifEmpty { weatherSnapshot?.let(::listOf).orEmpty() }
                .toJsonArray()
                .toString()

        val serviceIntent = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            // A snooze already carries the original weather extras.
            .apply { carriedExtras?.extras?.let(::putExtras) }
            .putExtra(AlarmActivity.EXTRA_REASON, reason)
            .putExtra(AlarmService.EXTRA_SNOOZE_COUNT, snoozeCount)
            .putExtra(AlarmActivity.EXTRA_FORECAST_SNAPSHOTS, forecastJson)
            .apply {
                weatherSnapshot?.let { weather ->
                    putExtra(AlarmActivity.EXTRA_WEATHER_CONDITION, weather.condition.name)
                    putExtra(AlarmActivity.EXTRA_PRECIPITATION_PROBABILITY, weather.precipitationProbability)
                    putExtra(AlarmActivity.EXTRA_PRECIPITATION_AMOUNT_MM, weather.precipitationAmountMm)
                    putExtra(AlarmActivity.EXTRA_WEATHER_SOURCE, weather.source)
                    putExtra(AlarmActivity.EXTRA_TARGET_TIME, weather.targetDateTime.toLocalTime().toString())
                }
            }

        runCatching {
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    private fun isFresh(decision: AlarmDecision): Boolean {
        return Duration.between(decision.decidedAt, Instant.now()) < MAX_DECISION_AGE
    }

    companion object {
        const val EXTRA_TEST_ALARM = "test_alarm"
        const val EXTRA_SNOOZE_ALARM = "snooze_alarm"
        const val EXTRA_ALARM_ID = "alarm_id"

        /**
         * A decision older than this belongs to a previous alarm run and must
         * not be trusted; the fail policy applies instead.
         */
        private val MAX_DECISION_AGE: Duration = Duration.ofHours(6)
    }
}

private fun List<WeatherSnapshot>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { weather ->
            array.put(
                JSONObject()
                    .put("targetTime", weather.targetDateTime.toLocalTime().toString())
                    .put("condition", weather.condition.name)
                    .put("precipitationProbability", weather.precipitationProbability)
                    .put("precipitationAmountMm", weather.precipitationAmountMm),
            )
        }
    }
}
