package com.weathermorning.alarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.weathermorning.alarm.R
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.presentation.AlarmActivity

/**
 * Owns alarm sound and vibration.
 *
 * The ring screen cannot own the signal: when the device is awake and in use,
 * the full-screen intent is demoted to a heads-up notification and
 * [AlarmActivity] never starts. A foreground service keeps ringing in that case
 * and survives the ring screen being swiped away.
 */
class AlarmService : Service() {
    private var signalController: AlarmSignalController? = null
    private var payload: Bundle? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = LocalAlarmSettingsStore(this)
        val alarmId = (intent?.extras ?: payload)?.getLong(AlarmReceiver.EXTRA_ALARM_ID, 0L) ?: 0L
        val settings = alarmId.takeIf { it > 0L }?.let(store::load)
            ?: store.loadAll().firstOrNull()
            ?: AlarmSettings.Default
        if (intent?.action != ACTION_STOP && intent?.extras != null) {
            payload = intent.extras
        }

        // startForegroundService requires startForeground within a few seconds,
        // whatever the action turns out to be.
        startForegroundWith(buildNotification(settings))

        when (intent?.action) {
            ACTION_STOP -> stopRinging()
            ACTION_SNOOZE -> snooze(settings)
            else -> startRinging(settings)
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        signalController?.stop()
        signalController = null
        super.onDestroy()
    }

    private fun startRinging(settings: AlarmSettings) {
        if (signalController != null) return
        signalController = AlarmSignalController(this).also { it.start(settings) }
    }

    private fun snooze(settings: AlarmSettings) {
        AndroidAlarmScheduler(this).scheduleSnooze(
            settings = settings,
            payload = payload,
            nextSnoozeCount = snoozeCount() + 1,
        )
        stopRinging()
    }

    private fun stopRinging() {
        signalController?.stop()
        signalController = null
        sendBroadcast(
            Intent(ACTION_ALARM_HANDLED).setPackage(packageName),
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundWith(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun buildNotification(settings: AlarmSettings): Notification {
        val channelId = createChannel(settings)
        val reason = payload?.getString(AlarmActivity.EXTRA_REASON)
            ?: getString(R.string.alarm_default_reason)
        val ringScreen = PendingIntent.getActivity(
            this,
            REQUEST_RING_SCREEN,
            ringScreenIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(ringScreen, true)
            .setContentIntent(ringScreen)
            .setOngoing(true)
            // Sound and vibration come from AlarmSignalController, never from
            // the notification, so the two cannot double up.
            .setSilent(true)
            .setSound(null)
            .setVibrate(longArrayOf(0))
            .setDefaults(0)

        if (canSnooze(settings)) {
            builder.addAction(
                R.drawable.ic_snooze,
                getString(R.string.snooze),
                serviceAction(ACTION_SNOOZE, REQUEST_SNOOZE, withPayload = true),
            )
        }
        builder.addAction(
            R.drawable.ic_alarm,
            getString(R.string.stop),
            serviceAction(ACTION_STOP, REQUEST_STOP, withPayload = false),
        )
        return builder.build()
    }

    private fun ringScreenIntent(): Intent {
        return Intent(this, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { payload?.let(::putExtras) }
    }

    private fun serviceAction(
        action: String,
        requestCode: Int,
        withPayload: Boolean,
    ): PendingIntent {
        val intent = Intent(this, AlarmService::class.java)
            .setAction(action)
            .apply { if (withPayload) payload?.let(::putExtras) }
        return PendingIntent.getForegroundService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel(settings: AlarmSettings): String {
        val channelId = AlarmNotificationChannels.channelId(settings)
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
            vibrationPattern = null
        }
        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(channel)
            AlarmNotificationChannels
                .staleChannelIds(
                    allChannelIds = notificationChannels.map { it.id },
                    currentChannelId = channelId,
                )
                .forEach(::deleteNotificationChannel)
        }
        return channelId
    }

    private fun snoozeCount(): Int = payload?.getInt(EXTRA_SNOOZE_COUNT, 0) ?: 0

    private fun canSnooze(settings: AlarmSettings): Boolean {
        return settings.snoozeEnabled && snoozeCount() < settings.snoozeMaxCount
    }

    companion object {
        const val ACTION_START = "com.weathermorning.alarm.action.START_ALARM"
        const val ACTION_STOP = "com.weathermorning.alarm.action.STOP_ALARM"
        const val ACTION_SNOOZE = "com.weathermorning.alarm.action.SNOOZE_ALARM"

        /** Sent package-locally once the alarm is stopped or snoozed. */
        const val ACTION_ALARM_HANDLED = "com.weathermorning.alarm.action.ALARM_HANDLED"

        const val EXTRA_SNOOZE_COUNT = "snooze_count"

        private const val NOTIFICATION_ID = 7001
        private const val REQUEST_RING_SCREEN = 9201
        private const val REQUEST_SNOOZE = 9202
        private const val REQUEST_STOP = 9203
    }
}
