package com.weathermorning.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the alarm whenever the system drops pending alarms.
 *
 * A reboot is the obvious case, but a Play Store update also clears them, and
 * the app would otherwise stay silent until the user next opened it.
 */
class AlarmRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in RESCHEDULE_ACTIONS) return
        AndroidAlarmScheduler(context).scheduleAll()
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
