package com.weathermorning.alarm.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.weathermorning.alarm.R
import com.weathermorning.alarm.alarm.NextAlarmCalculator
import com.weathermorning.alarm.data.local.LocalAlarmDecisionStore
import com.weathermorning.alarm.data.local.LocalAlarmHistoryStore
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.data.local.WeatherApiSettingsStore
import com.weathermorning.alarm.data.repository.BackendProxyWeatherRepository
import com.weathermorning.alarm.data.repository.HolidayRepository
import com.weathermorning.alarm.data.repository.WeatherResolution
import com.weathermorning.alarm.data.repository.WeatherRepositoryResolver
import com.weathermorning.alarm.domain.model.AlarmHistoryType
import com.weathermorning.alarm.domain.model.WeatherCheckWindow
import com.weathermorning.alarm.domain.usecase.DecideAlarmUseCase
import com.weathermorning.alarm.domain.usecase.DecideWeatherWindowUseCase
import com.weathermorning.alarm.domain.usecase.WeatherCheckTargets
import java.time.LocalDateTime

class WeatherCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val historyStore = LocalAlarmHistoryStore(applicationContext)
    private val singleHourDecision = DecideAlarmUseCase()
    private val windowDecision = DecideWeatherWindowUseCase(singleHourDecision)

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val PRECHECK_CHANNEL_ID = "weather_morning_precheck"
        private const val PRECHECK_NOTIFICATION_ID = 7002
    }

    /**
     * Below Android 12 expedited work runs as a foreground service, so
     * WorkManager needs a notification to show while the forecast is fetched.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                PRECHECK_CHANNEL_ID,
                applicationContext.getString(R.string.precheck_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        val notification = NotificationCompat.Builder(applicationContext, PRECHECK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_cloud)
            .setContentTitle(applicationContext.getString(R.string.precheck_notification_title))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
        return ForegroundInfo(PRECHECK_NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        val settingsStore = LocalAlarmSettingsStore(applicationContext)
        val requestedId = inputData.getLong(KEY_ALARM_ID, 0L)
        val settings = requestedId.takeIf { it > 0L }?.let(settingsStore::load)
            ?: settingsStore.loadAll().firstOrNull()
            ?: return Result.success()
        val decisionStore = LocalAlarmDecisionStore(applicationContext, settings.id)
        if (!settings.enabled) {
            val decision = singleHourDecision.decide(settings, weather = null)
            savePrecheckDecision(decisionStore, decision)
            return Result.success()
        }

        // The precheck already has network and runs daily, so it is the natural
        // place to keep the holiday cache warm for the scheduler.
        val holidays = HolidayRepository(applicationContext)
        val thisYear = LocalDateTime.now().year
        holidays.refresh(thisYear)
        holidays.refresh(thisYear + 1)

        val targetDateTime = NextAlarmCalculator.nextAlarmDateTime(
            settings = settings,
            fetchedHolidays = holidays.cachedHolidays(thisYear) + holidays.cachedHolidays(thisYear + 1),
        )
            ?: LocalDateTime.of(LocalDateTime.now().toLocalDate(), settings.alarmTime)
        val targetDateTimes = WeatherCheckTargets.forWindow(settings.weatherCheckWindow, targetDateTime)
        val weatherApiSettings = WeatherApiSettingsStore(applicationContext).load()
        val resolver = WeatherRepositoryResolver(
            backendProxyRepository = BackendProxyWeatherRepository.fromBuildConfig(applicationContext),
        )
        val weatherResolutions = targetDateTimes.map { target ->
            resolver.getForecast(
                settings = weatherApiSettings,
                alarmSettings = settings,
                targetDateTime = target,
            )
        }
        val weatherSnapshots = weatherResolutions.mapNotNull { it.result.getOrNull() }
        val lastFailure = weatherResolutions.lastOrNull { it.result.isFailure }?.result?.exceptionOrNull()
        val decision = if (settings.weatherCheckWindow == WeatherCheckWindow.ALARM_TIME) {
            weatherResolutions.first().result.fold(
                onSuccess = { singleHourDecision.decide(settings, it) },
                onFailure = { singleHourDecision.decide(settings, null, it) },
            )
        } else {
            windowDecision.decide(settings, weatherSnapshots, lastFailure)
        }
        val storedDecision = decision.copy(
            weatherSnapshots = weatherSnapshots.sortedBy { it.targetDateTime },
        )

        weatherResolutions.forEach { appendProviderDiagnosticIfNeeded(it) }
        savePrecheckDecision(decisionStore, storedDecision)

        // A saved fail-policy decision is better than nothing, so the decision
        // is stored before retrying. A later attempt overwrites it with real
        // weather if the network comes back before the alarm.
        val everyProviderFailed = weatherSnapshots.isEmpty()
        if (everyProviderFailed && runAttemptCount < MAX_RETRY_ATTEMPTS) {
            return Result.retry()
        }
        return Result.success()
    }

    private suspend fun savePrecheckDecision(
        decisionStore: LocalAlarmDecisionStore,
        decision: com.weathermorning.alarm.domain.model.AlarmDecision,
    ) {
        decisionStore.save(decision)
        historyStore.append(
            type = AlarmHistoryType.PRECHECK,
            reason = decision.reason,
            decision = decision,
        )
    }

    private suspend fun appendProviderDiagnosticIfNeeded(
        weatherResolution: WeatherResolution,
    ) {
        val failedBeforeSuccess = weatherResolution.failedAttemptsBeforeSuccess
        val successfulProvider = weatherResolution.successfulProvider
        if (failedBeforeSuccess.isNotEmpty() && successfulProvider != null) {
            historyStore.append(
                type = AlarmHistoryType.DIAGNOSTIC,
                reason = applicationContext.getString(
                    R.string.provider_fallback_success,
                    failedBeforeSuccess.joinToString(" -> ") { it.provider.name },
                    successfulProvider.name,
                ),
            )
            return
        }

        if (weatherResolution.result.isFailure && weatherResolution.attempts.isNotEmpty()) {
            historyStore.append(
                type = AlarmHistoryType.DIAGNOSTIC,
                reason = applicationContext.getString(
                    R.string.provider_fallback_failed,
                    weatherResolution.attempts.joinToString(" -> ") { it.provider.name },
                ),
            )
        }
    }
}
