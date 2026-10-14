package com.weathermorning.alarm.presentation.history

import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryReasonDisplayParserTest {
    @Test
    fun parsesSimpleLegacyReasons() {
        assertEquals(
            HistoryReasonDisplay.AlarmDisabled,
            HistoryReasonDisplayParser.parse("Alarm disabled"),
        )
        assertEquals(
            HistoryReasonDisplay.ReceiverTestAlarm,
            HistoryReasonDisplayParser.parse("Receiver test alarm"),
        )
    }

    @Test
    fun parsesPrecipitationThresholdReason() {
        assertEquals(
            HistoryReasonDisplay.PrecipitationThreshold(actual = 80, threshold = 40),
            HistoryReasonDisplayParser.parse("Precipitation probability 80% >= 40%"),
        )
    }

    @Test
    fun parsesNoTriggerReason() {
        assertEquals(
            HistoryReasonDisplay.NoTrigger(WeatherCondition.CLEAR, probability = 10),
            HistoryReasonDisplayParser.parse("No trigger: CLEAR, precipitation 10%"),
        )
    }

    @Test
    fun keepsUnknownReasonRaw() {
        assertEquals(
            HistoryReasonDisplay.Raw("New future reason"),
            HistoryReasonDisplayParser.parse("New future reason"),
        )
    }

    @Test
    fun parsesTypedCodeWithParameters() {
        assertEquals(
            HistoryReasonDisplay.NoTrigger(WeatherCondition.CLEAR, probability = 10),
            HistoryReasonDisplayParser.parseCode(
                code = AlarmReasonCode.NO_TRIGGER,
                condition = WeatherCondition.CLEAR,
                value1 = 10,
                value2 = null,
            ),
        )
        assertEquals(
            HistoryReasonDisplay.PrecipitationThreshold(actual = 80, threshold = 40),
            HistoryReasonDisplayParser.parseCode(
                code = AlarmReasonCode.PRECIPITATION_THRESHOLD,
                condition = null,
                value1 = 80,
                value2 = 40,
            ),
        )
    }

    @Test
    fun detectsReasonsThatAlreadyContainWeatherDetails() {
        assertTrue(HistoryReasonDisplayParser.hasWeatherDetails("No trigger: CLEAR, precipitation 10%"))
        assertTrue(HistoryReasonDisplayParser.hasWeatherDetails("Precipitation probability 80% >= 40%"))
        assertFalse(HistoryReasonDisplayParser.hasWeatherDetails("Rain forecast"))
    }
}
