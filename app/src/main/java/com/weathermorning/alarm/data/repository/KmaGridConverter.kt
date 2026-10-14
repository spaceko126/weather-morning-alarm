package com.weathermorning.alarm.data.repository

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

object KmaGridConverter {
    fun toGrid(latitude: Double, longitude: Double): KmaGrid {
        val re = 6371.00877
        val grid = 5.0
        val slat1 = 30.0.toRadians()
        val slat2 = 60.0.toRadians()
        val olon = 126.0.toRadians()
        val olat = 38.0.toRadians()
        val xo = 43.0
        val yo = 136.0

        val sn = ln(slat1.cosValue() / slat2.cosValue()) /
            ln((PI * 0.25 + slat2 * 0.5).tanValue() / (PI * 0.25 + slat1 * 0.5).tanValue())
        val sf = (tan(PI * 0.25 + slat1 * 0.5).pow(sn) * slat1.cosValue()) / sn
        val ro = re / grid * sf / tan(PI * 0.25 + olat * 0.5).pow(sn)

        val ra = re / grid * sf / tan(PI * 0.25 + latitude.toRadians() * 0.5).pow(sn)
        var theta = longitude.toRadians() - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        val nx = floor(ra * kotlin.math.sin(theta) + xo + 0.5).toInt()
        val ny = floor(ro - ra * kotlin.math.cos(theta) + yo + 0.5).toInt()
        return KmaGrid(nx, ny)
    }

    private fun Double.toRadians(): Double = this / 180.0 * PI
    private fun Double.tanValue(): Double = tan(this)
    private fun Double.cosValue(): Double = kotlin.math.cos(this)
}

data class KmaGrid(
    val nx: Int,
    val ny: Int,
)
