package com.weathermorning.alarm.alarm

import java.time.LocalDate
import java.time.Month

/**
 * Korean public holidays.
 *
 * Fixed-date holidays are known offline. Lunar ones — Lunar New Year, Chuseok,
 * Buddha's Birthday — and substitute holidays move every year, so they come
 * from the backend's `/holidays` route via
 * [com.weathermorning.alarm.data.repository.HolidayRepository] and are passed in
 * here. With no fetched set the fixed dates still apply on their own.
 */
object KoreanPublicHolidayCalendar {
    fun isPublicHoliday(date: LocalDate, fetchedHolidays: Set<LocalDate> = emptySet()): Boolean {
        return isFixedPublicHoliday(date) || date in fetchedHolidays
    }

    fun isFixedPublicHoliday(date: LocalDate): Boolean {
        return when (date.month) {
            Month.JANUARY -> date.dayOfMonth == 1
            Month.MARCH -> date.dayOfMonth == 1
            Month.MAY -> date.dayOfMonth == 5
            Month.JUNE -> date.dayOfMonth == 6
            Month.AUGUST -> date.dayOfMonth == 15
            Month.OCTOBER -> date.dayOfMonth == 3 || date.dayOfMonth == 9
            Month.DECEMBER -> date.dayOfMonth == 25
            else -> false
        }
    }
}
