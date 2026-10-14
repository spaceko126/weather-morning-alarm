package com.weathermorning.alarm.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.weathermorning.alarm.R
import java.util.Locale

class CurrentLocationReader(private val context: Context) {
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun readLastKnown(): CurrentLocation? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java)
        return manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toCurrentLocation()
    }

    fun resolveDisplayName(
        location: CurrentLocation,
        onResult: (String) -> Unit,
    ) {
        Thread {
            val resolved = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.toDisplayName()
            }.getOrNull()
                ?: nearestKnownRegion(location)

            Handler(Looper.getMainLooper()).post {
                onResult(resolved)
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun requestSingleUpdate(
        timeoutMillis: Long = 15_000,
        onResult: (CurrentLocation?) -> Unit,
    ) {
        if (!hasPermission()) {
            onResult(null)
            return
        }

        val manager = context.getSystemService(LocationManager::class.java)
        val provider = chooseProvider(manager)
        if (provider == null) {
            onResult(null)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var completed = false
        @Suppress("OVERRIDE_DEPRECATION")
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (completed) return
                completed = true
                manager.removeUpdates(this)
                onResult(location.toCurrentLocation())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        handler.postDelayed({
            if (!completed) {
                completed = true
                manager.removeUpdates(listener)
                onResult(null)
            }
        }, timeoutMillis)

        runCatching {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }.onFailure {
            if (!completed) {
                completed = true
                manager.removeUpdates(listener)
                onResult(null)
            }
        }
    }

    private fun chooseProvider(manager: LocationManager): String? {
        return when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> manager.getProviders(true).firstOrNull()
        }
    }

    private fun Location.toCurrentLocation(): CurrentLocation {
        return CurrentLocation(
            latitude = latitude,
            longitude = longitude,
            provider = provider ?: "unknown",
        )
    }

    private fun Address.toDisplayName(): String? {
        val parts = listOf(
            adminArea,
            locality,
            subLocality,
            thoroughfare,
        ).mapNotNull { part ->
            part?.trim()?.takeIf { it.isNotBlank() }
        }.distinct()

        return parts.joinToString(" ").takeIf { it.isNotBlank() }
    }

    /** Returns a region *key*; the UI turns it into a name in the device language. */
    private fun nearestKnownRegion(location: CurrentLocation): String {
        val nearest = knownRegions.minByOrNull { region ->
            val latDelta = location.latitude - region.latitude
            val lonDelta = location.longitude - region.longitude
            (latDelta * latDelta) + (lonDelta * lonDelta)
        }
        return nearest?.key ?: context.getString(R.string.current_location_label)
    }

    private data class KnownRegion(
        val key: String,
        val latitude: Double,
        val longitude: Double,
    )

    companion object {
        private val knownRegions = listOf(
            KnownRegion("Seoul", 37.5665, 126.9780),
            KnownRegion("Incheon", 37.4563, 126.7052),
            KnownRegion("Suwon", 37.2636, 127.0286),
            KnownRegion("Busan", 35.1796, 129.0756),
            KnownRegion("Daegu", 35.8714, 128.6014),
            KnownRegion("Daejeon", 36.3504, 127.3845),
            KnownRegion("Gwangju", 35.1595, 126.8526),
            KnownRegion("Ulsan", 35.5384, 129.3114),
            KnownRegion("Jeju", 33.4996, 126.5312),
        )
    }
}

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val provider: String,
)
