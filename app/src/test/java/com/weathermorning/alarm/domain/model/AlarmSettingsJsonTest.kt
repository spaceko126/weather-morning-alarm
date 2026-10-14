package com.weathermorning.alarm.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Guards the two invariants multi-alarm scheduling depends on: alarms keep
 * distinct ids, and each id maps to its own set of PendingIntent request codes.
 */
class AlarmSettingsJsonTest {
    @Test
    fun defaultAlarmUsesFirstId() {
        assertEquals(1L, AlarmSettings.Default.id)
    }

    @Test
    fun everyAlarmSlotGetsItsOwnRequestCode() {
        val codes = (1L..25L).flatMap { id -> (0..3).map { slot -> requestCode(id, slot) } }

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun differentAlarmsNeverShareTheAlarmSlot() {
        assertNotEquals(requestCode(1L, 0), requestCode(2L, 0))
    }

    @Test
    fun alarmsCanDifferPerId() {
        val morning = AlarmSettings.Default.copy(id = 1L, alarmTime = LocalTime.of(6, 30))
        val weekend = AlarmSettings.Default.copy(
            id = 2L,
            alarmTime = LocalTime.of(9, 0),
            repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )

        assertNotEquals(morning.id, weekend.id)
        assertEquals(LocalTime.of(6, 30), morning.alarmTime)
        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), weekend.repeatDays)
    }

    /** Mirrors AndroidAlarmScheduler.requestCode, which is private to the class. */
    private fun requestCode(alarmId: Long, slot: Int): Int = 10_000 + (alarmId.toInt() * 4) + slot
}
