package com.weathermorning.alarm.alarm

import com.weathermorning.alarm.domain.model.AlarmSettings
import java.time.LocalDate
import java.time.LocalDateTime

object NextAlarmCalculator {
    fun nextAlarmDateTime(
        settings: AlarmSettings,
        now: LocalDateTime = LocalDateTime.now(),
        fetchedHolidays: Set<LocalDate> = emptySet(),
    ): LocalDateTime? {
        if (!settings.enabled || settings.repeatDays.isEmpty()) return null

        for (offset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(offset.toLong())
            val candidate = LocalDateTime.of(candidateDate, settings.alarmTime)
            val holidayBlocked = settings.excludePublicHolidays &&
                KoreanPublicHolidayCalendar.isPublicHoliday(candidateDate, fetchedHolidays)
            if (
                candidate.isAfter(now) &&
                settings.repeatDays.contains(candidate.dayOfWeek) &&
                !holidayBlocked
            ) {
                return candidate
            }
        }

        return null
    }
}
