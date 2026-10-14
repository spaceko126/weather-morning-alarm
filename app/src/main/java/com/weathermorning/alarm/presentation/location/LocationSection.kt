package com.weathermorning.alarm.presentation.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weathermorning.alarm.R
import com.weathermorning.alarm.data.repository.GeocodedPlace
import com.weathermorning.alarm.data.repository.GeocodingSearchRepository
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.presentation.components.ChipFlow
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText
import com.weathermorning.alarm.presentation.components.SelectableChip
import kotlinx.coroutines.delay

/** What the picker hands back, whether it came from a preset or a search. */
internal data class PickedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Composable
internal fun LocationPickerDialog(
    settings: AlarmSettings,
    query: String,
    status: String,
    onQueryChange: (String) -> Unit,
    onLocationSelect: (PickedLocation) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_location)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        text = {
            LocationPickerContent(
                settings = settings,
                query = query,
                status = status,
                onQueryChange = onQueryChange,
                onLocationSelect = onLocationSelect,
                onUseCurrentLocation = onUseCurrentLocation,
            )
        },
    )
}

@Composable
private fun LocationPickerContent(
    settings: AlarmSettings,
    query: String,
    status: String,
    onQueryChange: (String) -> Unit,
    onLocationSelect: (PickedLocation) -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            // The region list plus an open keyboard overflows a short dialog.
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DialogSectionTitle(stringResource(R.string.current_region))
        InfoText(
            text = localizedLocationName(settings.locationName),
        )
        CompactButton(
            label = stringResource(R.string.use_current_location),
            onClick = onUseCurrentLocation,
            modifier = Modifier.fillMaxWidth(),
        )
        if (status.isNotBlank()) {
            InfoText(status)
        }
        DialogSectionTitle(stringResource(R.string.region_search))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_region)) },
            singleLine = true,
        )
        SearchResults(
            query = query,
            onSelect = onLocationSelect,
        )
        DialogSectionTitle(stringResource(R.string.region_presets))
        PresetPicker(
            selected = settings.locationName,
            onSelect = onLocationSelect,
        )
    }
}

@Composable
private fun SearchResults(
    query: String,
    onSelect: (PickedLocation) -> Unit,
) {
    val repository = remember { GeocodingSearchRepository() }
    var results by remember { mutableStateOf<List<GeocodedPlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    // Debounced so a search does not fire on every keystroke.
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            searching = false
            failed = false
            return@LaunchedEffect
        }
        searching = true
        failed = false
        delay(350)
        repository.search(trimmed)
            .onSuccess { results = it }
            .onFailure {
                results = emptyList()
                failed = true
            }
        searching = false
    }

    when {
        query.trim().length < 2 -> Unit
        searching -> InfoText(stringResource(R.string.region_searching))
        failed -> InfoText(stringResource(R.string.region_search_failed))
        results.isEmpty() -> InfoText(stringResource(R.string.no_matching_region))
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            results.forEach { place ->
                CompactButton(
                    label = place.displayName,
                    onClick = {
                        onSelect(
                            PickedLocation(place.displayName, place.latitude, place.longitude),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PresetPicker(selected: String, onSelect: (PickedLocation) -> Unit) {
    ChipFlow {
        RegionPreset.All.forEach { region ->
            val label = stringResource(region.displayNameRes)
            SelectableChip(
                label = label,
                selected = selected == region.key || selected == label,
                onClick = {
                    onSelect(PickedLocation(region.key, region.latitude, region.longitude))
                },
            )
        }
    }
}
