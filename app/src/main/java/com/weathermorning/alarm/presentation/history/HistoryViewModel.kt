package com.weathermorning.alarm.presentation.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weathermorning.alarm.data.local.LocalAlarmHistoryStore
import com.weathermorning.alarm.domain.model.AlarmHistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyStore = LocalAlarmHistoryStore(application)

    val entries: StateFlow<List<AlarmHistoryEntry>> =
        historyStore.observeLatest(LocalAlarmHistoryStore.MAX_ENTRIES)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun clear() {
        viewModelScope.launch {
            historyStore.clear()
        }
    }
}
