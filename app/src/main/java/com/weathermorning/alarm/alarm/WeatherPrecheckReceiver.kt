package com.weathermorning.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.weathermorning.alarm.worker.WeatherCheckWorker
import com.weathermorning.alarm.worker.WeatherWorkNames
import java.time.Duration

class WeatherPrecheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Ordinary work can sit in Doze until the next maintenance window,
        // which may fall after the alarm itself. Expedited work runs now.
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L) ?: 0L
        val request = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
            .setInputData(workDataOf(WeatherCheckWorker.KEY_ALARM_ID to alarmId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF)
            .build()

        // Unique per alarm, so two alarms an hour apart do not cancel each
        // other's forecast check.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${WeatherWorkNames.SCHEDULED_PRECHECK}_$alarmId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private companion object {
        val RETRY_BACKOFF: Duration = Duration.ofSeconds(30)
    }
}
