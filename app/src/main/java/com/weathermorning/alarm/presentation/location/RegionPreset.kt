package com.weathermorning.alarm.presentation.location

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.weathermorning.alarm.R

/**
 * Presets are stored by [RegionPreset.key], never by their shown name, so the
 * saved location follows the device language instead of freezing whichever one
 * was active when it was picked. Geocoded names pass through unchanged.
 */
@Composable
internal fun localizedLocationName(stored: String): String {
    val preset = RegionPreset.All.firstOrNull { it.key.equals(stored.trim(), ignoreCase = true) }
        ?: return stored
    return stringResource(preset.displayNameRes)
}

internal data class RegionPreset(
    val key: String,
    @param:StringRes val displayNameRes: Int,
    val latitude: Double,
    val longitude: Double,
    val aliases: List<String> = emptyList(),
) {
    companion object {
        val All = listOf(
            RegionPreset("Seoul", R.string.region_seoul, 37.5665, 126.9780, listOf("seoul", "soul", "서울")),
            RegionPreset("Incheon", R.string.region_incheon, 37.4563, 126.7052, listOf("incheon", "inchon", "인천")),
            RegionPreset("Suwon", R.string.region_suwon, 37.2636, 127.0286, listOf("suwon", "gyeonggi", "수원", "경기")),
            RegionPreset("Busan", R.string.region_busan, 35.1796, 129.0756, listOf("busan", "pusan", "부산")),
            RegionPreset("Daegu", R.string.region_daegu, 35.8714, 128.6014, listOf("daegu", "taegu", "대구")),
            RegionPreset("Daejeon", R.string.region_daejeon, 36.3504, 127.3845, listOf("daejeon", "taejon", "대전")),
            RegionPreset("Gwangju", R.string.region_gwangju, 35.1595, 126.8526, listOf("gwangju", "kwangju", "광주")),
            RegionPreset("Ulsan", R.string.region_ulsan, 35.5384, 129.3114, listOf("ulsan", "울산")),
            RegionPreset("Jeju", R.string.region_jeju, 33.4996, 126.5312, listOf("jeju", "제주")),
        )
    }
}
