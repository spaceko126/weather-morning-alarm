package com.weathermorning.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.edit
import com.weathermorning.alarm.data.local.LocalAlarmDecisionStore
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.data.repository.HolidayRepository
import com.weathermorning.alarm.domain.model.AlarmSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val statePrefs = context.getSharedPreferences("alarm_schedule_state", Context.MODE_PRIVATE)

    /** Re-arms every saved alarm. Used after a reboot, update or time change. */
    fun scheduleAll() {
        LocalAlarmSettingsStore(context).loadAll().forEach(::scheduleNext)
    }

    override fun scheduleNext(settings: AlarmSettings) {
        cancelLegacySchedules()
        cancel(settings.id)

        val alarmAt = NextAlarmCalculator.nextAlarmDateTime(
            settings = settings,
            fetchedHolidays = holidaysForUpcomingYears(),
        )
        val fingerprint = scheduleFingerprint(settings, alarmAt)
        // A saved weather decision belongs to one specific alarm run. Keep it
        // when nothing that feeds the decision changed, so simply reopening the
        // app between the precheck and the alarm does not throw it away.
        val fingerprintKey = "$KEY_FINGERPRINT.${settings.id}"
        if (fingerprint != statePrefs.getString(fingerprintKey, null)) {
            LocalAlarmDecisionStore(context, settings.id).clear()
            statePrefs.edit { putString(fingerprintKey, fingerprint) }
        }

        if (alarmAt == null) return

        scheduleExact(
            requestCode = requestCode(settings.id, SLOT_PRECHECK_BACKUP),
            atMillis = alarmAt.minusMinutes(PRECHECK_BACKUP_MINUTES).toEpochMillis(),
            intent = precheckIntent(settings.id),
        )
        scheduleExact(
            requestCode = requestCode(settings.id, SLOT_PRECHECK),
            atMillis = alarmAt.minusMinutes(PRECHECK_MINUTES).toEpochMillis(),
            intent = precheckIntent(settings.id),
        )
        scheduleExact(
            requestCode = requestCode(settings.id, SLOT_ALARM),
            atMillis = alarmAt.toEpochMillis(),
            intent = alarmIntent(settings.id),
        )
    }

    /**
     * Re-rings one alarm after its snooze interval, carrying the original
     * weather payload so the ring screen still explains itself.
     */
    fun scheduleSnooze(settings: AlarmSettings, payload: Bundle?, nextSnoozeCount: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .apply { payload?.let(::putExtras) }
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, settings.id)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_ALARM, true)
            .putExtra(AlarmService.EXTRA_SNOOZE_COUNT, nextSnoozeCount)
        scheduleExact(
            requestCode = requestCode(settings.id, SLOT_SNOOZE),
            atMillis = System.currentTimeMillis() + settings.snoozeMinutes * 60_000L,
            intent = intent,
        )
    }

    override fun cancel() {
        LocalAlarmSettingsStore(context).loadAll().forEach { cancel(it.id) }
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntent(requestCode(alarmId, SLOT_PRECHECK_BACKUP), precheckIntent(alarmId)))
        alarmManager.cancel(pendingIntent(requestCode(alarmId, SLOT_PRECHECK), precheckIntent(alarmId)))
        alarmManager.cancel(pendingIntent(requestCode(alarmId, SLOT_ALARM), alarmIntent(alarmId)))
        // A pending snooze belongs to the run being replaced; turning the alarm
        // off or changing its time must not leave it behind.
        alarmManager.cancel(pendingIntent(requestCode(alarmId, SLOT_SNOOZE), alarmIntent(alarmId)))
    }

    /**
     * Cancels the alarms a pre-WM-128 build left behind.
     *
     * Those used fixed request codes for the single alarm. An upgrade cannot
     * reach them through the new per-alarm codes, so without this they keep
     * firing alongside the new schedule — and would still fire after the user
     * turned the alarm off.
     */
    private fun cancelLegacySchedules() {
        if (statePrefs.getBoolean(KEY_LEGACY_CLEARED, false)) return
        LEGACY_REQUEST_CODES.forEach { requestCode ->
            existingPendingIntent(requestCode, Intent(context, AlarmReceiver::class.java))
                ?.let(alarmManager::cancel)
            existingPendingIntent(requestCode, Intent(context, WeatherPrecheckReceiver::class.java))
                ?.let(alarmManager::cancel)
        }
        statePrefs.edit { putBoolean(KEY_LEGACY_CLEARED, true) }
    }

    /** Null when nothing was ever scheduled under this request code. */
    private fun existingPendingIntent(requestCode: Int, intent: Intent): PendingIntent? {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Cancels the alarm and drops the state it left behind. */
    fun forget(alarmId: Long) {
        cancel(alarmId)
        LocalAlarmDecisionStore(context, alarmId).clear()
        statePrefs.edit { remove("$KEY_FINGERPRINT.$alarmId") }
    }

    /** Cache-only read: scheduling happens on a broadcast's main thread. */
    private fun holidaysForUpcomingYears(): Set<LocalDate> {
        val repository = HolidayRepository(context)
        val thisYear = LocalDate.now().year
        return repository.cachedHolidays(thisYear) + repository.cachedHolidays(thisYear + 1)
    }

    private fun alarmIntent(alarmId: Long): Intent {
        return Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
    }

    private fun precheckIntent(alarmId: Long): Intent {
        return Intent(context, WeatherPrecheckReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
    }

    private fun scheduleExact(requestCode: Int, atMillis: Long, intent: Intent) {
        val operation = pendingIntent(requestCode, intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
    }

    private fun pendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * PendingIntent equality ignores extras, so each alarm needs its own request
     * code per slot or the alarms would overwrite one another.
     */
    private fun requestCode(alarmId: Long, slot: Int): Int {
        return REQUEST_BASE + (alarmId.toInt() * SLOTS_PER_ALARM) + slot
    }

    /** Everything a saved decision depends on. */
    private fun scheduleFingerprint(settings: AlarmSettings, alarmAt: LocalDateTime?): String {
        return listOf(
            alarmAt?.toString() ?: "none",
            settings.enabled,
            settings.latitude,
            settings.longitude,
            settings.triggerRain,
            settings.triggerSnow,
            settings.precipitationThreshold,
            settings.rainAmountThreshold,
            settings.weatherCheckWindow,
            settings.failPolicy,
        ).joinToString("|")
    }

    private fun LocalDateTime.toEpochMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    companion object {
        private const val PRECHECK_MINUTES = 20L
        private const val PRECHECK_BACKUP_MINUTES = 60L
        private const val REQUEST_BASE = 10_000
        private const val SLOTS_PER_ALARM = 4
        private const val SLOT_ALARM = 0
        private const val SLOT_PRECHECK = 1
        private const val SLOT_PRECHECK_BACKUP = 2
        private const val SLOT_SNOOZE = 3
        private const val KEY_FINGERPRINT = "schedule_fingerprint"
        private const val KEY_LEGACY_CLEARED = "legacy_schedules_cleared"
        private val LEGACY_REQUEST_CODES = listOf(1001, 1002, 1003, 1004)
    }
}
