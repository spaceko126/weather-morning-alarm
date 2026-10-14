package com.weathermorning.alarm.presentation

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weathermorning.alarm.R
import com.weathermorning.alarm.alarm.AlarmReceiver
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.location.CurrentLocationReader
import com.weathermorning.alarm.presentation.components.AppSection
import com.weathermorning.alarm.presentation.components.CompactButton
import com.weathermorning.alarm.presentation.components.InfoText
import com.weathermorning.alarm.presentation.history.HistoryDialog
import com.weathermorning.alarm.presentation.history.HistorySection
import com.weathermorning.alarm.presentation.history.HistoryViewModel
import com.weathermorning.alarm.presentation.location.LocationPickerDialog
import com.weathermorning.alarm.presentation.onboarding.OnboardingDialog
import com.weathermorning.alarm.presentation.onboarding.OnboardingState
import com.weathermorning.alarm.presentation.permissions.PermissionDialog
import com.weathermorning.alarm.presentation.permissions.PrivacyDialog
import com.weathermorning.alarm.presentation.permissions.SupportSection
import com.weathermorning.alarm.presentation.settings.AlarmEditDialog
import com.weathermorning.alarm.presentation.settings.SettingsViewModel
import com.weathermorning.alarm.presentation.summary.SummarySection
import com.weathermorning.alarm.presentation.theme.WeatherMorningTheme
import com.weathermorning.alarm.presentation.theme.appBackgroundBrush

@Composable
fun WeatherMorningApp() {
    val context = LocalContext.current
    val locationReader = remember { CurrentLocationReader(context) }
    val historyViewModel: HistoryViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val alarms by settingsViewModel.alarms.collectAsState()
    val historyEntries by historyViewModel.entries.collectAsState()

    var editingAlarmId by remember { mutableStateOf<Long?>(null) }
    // Edits live here until the user saves, so leaving the editor really does
    // leave the alarm as it was.
    var draft by remember { mutableStateOf<AlarmSettings?>(null) }
    var showTimePickerFor by remember { mutableStateOf<Long?>(null) }
    var locationQuery by remember { mutableStateOf("") }
    var locationStatus by remember { mutableStateOf("") }
    var locationPickerFor by remember { mutableStateOf<Long?>(null) }
    var ringtonePickerFor by remember { mutableStateOf<Long?>(null) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val onboardingState = remember { OnboardingState(context) }
    var showOnboarding by remember { mutableStateOf(!onboardingState.isComplete()) }
    var batteryUnrestricted by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }

    fun saveCurrentLocationName(
        alarmId: Long,
        current: com.weathermorning.alarm.location.CurrentLocation,
    ) {
        locationStatus = context.getString(R.string.current_location_searching)
        locationReader.resolveDisplayName(current) { displayName ->
            draft = draft?.takeIf { it.id == alarmId }?.copy(
                locationName = displayName,
                latitude = current.latitude,
                longitude = current.longitude,
            ) ?: draft
            locationStatus = context.getString(R.string.current_location_saved)
        }
    }

    fun saveCurrentLocation(alarmId: Long) {
        val current = locationReader.readLastKnown()
        if (current != null) {
            saveCurrentLocationName(alarmId, current)
            return
        }

        locationStatus = context.getString(R.string.current_location_searching)
        locationReader.requestSingleUpdate { fresh ->
            if (fresh == null) {
                locationStatus = context.getString(R.string.current_location_unavailable)
            } else {
                saveCurrentLocationName(alarmId, fresh)
            }
        }
    }

    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var exactAlarmAllowed by remember { mutableStateOf(alarmManager.canScheduleExactAlarmsCompat()) }
    var fullScreenIntentAllowed by remember { mutableStateOf(context.canUseFullScreenIntent()) }
    var notificationAllowed by remember { mutableStateOf(context.canPostNotifications()) }

    // All three are granted in system settings, so the app only learns the new
    // answer once the user navigates back to it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmAllowed = alarmManager.canScheduleExactAlarmsCompat()
                fullScreenIntentAllowed = context.canUseFullScreenIntent()
                notificationAllowed = context.canPostNotifications()
                batteryUnrestricted = context.isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationAllowed = granted || context.canPostNotifications()
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val target = locationPickerFor
        if (granted && target != null) {
            saveCurrentLocation(target)
        } else if (!granted) {
            locationStatus = context.getString(R.string.location_permission_denied)
        }
    }
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val target = ringtonePickerFor ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pickedUri = result.data?.pickedRingtoneUri()
        if (pickedUri == null) {
            draft = draft?.takeIf { it.id == target }?.copy(
                alarmSoundEnabled = false,
                alarmSoundUri = "",
                alarmSoundTitle = "",
            ) ?: draft
            return@rememberLauncherForActivityResult
        }

        val title = runCatching {
            RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
        }.getOrNull().orEmpty()
        draft = draft?.takeIf { it.id == target }?.copy(
            alarmSoundEnabled = true,
            alarmSoundUri = pickedUri.toString(),
            alarmSoundTitle = title.ifBlank { context.getString(R.string.alarm_sound_default) },
        ) ?: draft
    }

    fun openRingtonePicker(alarm: AlarmSettings) {
        ringtonePickerFor = alarm.id
        val existingUri = alarm.alarmSoundUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtoneLauncher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.choose_alarm_sound))
            },
        )
    }

    fun runAlarmTest() {
        if (!exactAlarmAllowed || !notificationAllowed || !fullScreenIntentAllowed) {
            showPermissionsDialog = true
            return
        }
        scheduleAlarmTest(
            alarmManager = alarmManager,
            context = context,
            alarmId = alarms.firstOrNull()?.id ?: 1L,
        )
        Toast.makeText(context, R.string.test_alarm_scheduled, Toast.LENGTH_SHORT).show()
    }

    WeatherMorningTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush())
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(alarms.size, key = { alarms[it].id }) { index ->
                val alarm = alarms[index]
                SummarySection(
                    settings = alarm,
                    onToggleAlarm = {
                        settingsViewModel.update(alarm.id) { it.copy(enabled = !it.enabled) }
                    },
                    onOpenEdit = {
                        draft = alarm
                        editingAlarmId = alarm.id
                    },
                )
            }
            item("add-alarm") {
                AppSection(stringResource(R.string.alarm_list_title)) {
                    if (alarms.isEmpty()) {
                        InfoText(stringResource(R.string.alarm_list_empty))
                    }
                    CompactButton(
                        label = stringResource(R.string.add_alarm),
                        onClick = {
                            val id = settingsViewModel.addAlarm()
                            draft = settingsViewModel.alarms.value.firstOrNull { it.id == id }
                            editingAlarmId = id
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item("history") {
                HistorySection(
                    entries = historyEntries,
                    onOpen = { showHistoryDialog = true },
                )
            }
            item("support") {
                SupportSection(
                    onRunAlarmTest = ::runAlarmTest,
                    onOpenPermissions = { showPermissionsDialog = true },
                    onOpenPrivacy = { showPrivacyDialog = true },
                )
            }
        }

        val editing = draft?.takeIf { it.id == editingAlarmId }
        if (editing != null) {
            AlarmEditDialog(
                draft = editing,
                saved = alarms.firstOrNull { it.id == editing.id },
                onDraftChange = { draft = it },
                onSetTimeClick = { showTimePickerFor = editing.id },
                onChooseAlarmSound = { openRingtonePicker(editing) },
                onOpenLocation = { locationPickerFor = editing.id },
                onSave = {
                    settingsViewModel.save(editing)
                    editingAlarmId = null
                    draft = null
                },
                onDiscard = {
                    editingAlarmId = null
                    draft = null
                },
                onDelete = {
                    settingsViewModel.delete(editing.id)
                    editingAlarmId = null
                    draft = null
                },
                showTimePicker = showTimePickerFor == editing.id,
                onTimePickerDismiss = { showTimePickerFor = null },
                onTimeConfirm = { time ->
                    draft = editing.copy(alarmTime = time)
                    showTimePickerFor = null
                },
            )
        }

        val locationTarget = draft?.takeIf { it.id == locationPickerFor }
        if (locationTarget != null) {
            LocationPickerDialog(
                settings = locationTarget,
                query = locationQuery,
                status = locationStatus,
                onQueryChange = { locationQuery = it },
                onLocationSelect = { picked ->
                    draft = locationTarget.copy(
                        locationName = picked.name,
                        latitude = picked.latitude,
                        longitude = picked.longitude,
                    )
                    locationPickerFor = null
                },
                onUseCurrentLocation = {
                    if (locationReader.hasPermission()) {
                        saveCurrentLocation(locationTarget.id)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
                onDismiss = { locationPickerFor = null },
            )
        }

        if (showPermissionsDialog) {
            PermissionDialog(
                exactAlarmAllowed = exactAlarmAllowed,
                notificationAllowed = notificationAllowed,
                fullScreenIntentAllowed = fullScreenIntentAllowed,
                onOpenExactAlarmPermission = { context.openExactAlarmSettings() },
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onOpenFullScreenIntentPermission = { context.openFullScreenIntentSettings() },
                onDismiss = { showPermissionsDialog = false },
            )
        }

        if (showOnboarding) {
            OnboardingDialog(
                exactAlarmAllowed = exactAlarmAllowed,
                notificationAllowed = notificationAllowed,
                fullScreenIntentAllowed = fullScreenIntentAllowed,
                batteryUnrestricted = batteryUnrestricted,
                onOpenExactAlarmPermission = { context.openExactAlarmSettings() },
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onOpenFullScreenIntentPermission = { context.openFullScreenIntentSettings() },
                onOpenBatterySettings = { context.openBatterySettings() },
                onFinish = {
                    onboardingState.markComplete()
                    showOnboarding = false
                },
            )
        }

        if (showPrivacyDialog) {
            PrivacyDialog(onDismiss = { showPrivacyDialog = false })
        }

        if (showHistoryDialog) {
            HistoryDialog(
                entries = historyEntries,
                onClear = { historyViewModel.clear() },
                onDismiss = { showHistoryDialog = false },
            )
        }
    }
}

private fun Intent.pickedRingtoneUri(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
}

private fun android.content.Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    runCatching {
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:$packageName".toUri()
            },
        )
    }
}

private fun android.content.Context.openFullScreenIntentSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    runCatching {
        startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = "package:$packageName".toUri()
            },
        )
    }
}

/**
 * Opens the system battery-optimisation list rather than requesting the
 * exemption directly: the direct request needs a permission Play restricts, and
 * an alarm only needs the user to flip the switch once.
 */
private fun android.content.Context.openBatterySettings() {
    runCatching {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }.onFailure {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                },
            )
        }
    }
}

private fun android.content.Context.isIgnoringBatteryOptimizations(): Boolean {
    val power = getSystemService(PowerManager::class.java) ?: return true
    return power.isIgnoringBatteryOptimizations(packageName)
}

private fun AlarmManager.canScheduleExactAlarmsCompat(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()
}

private fun android.content.Context.canPostNotifications(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}

private fun android.content.Context.canUseFullScreenIntent(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}

private fun scheduleAlarmTest(
    alarmManager: AlarmManager,
    context: android.content.Context,
    alarmId: Long,
) {
    val intent = Intent(context, AlarmReceiver::class.java)
        .putExtra(AlarmReceiver.EXTRA_TEST_ALARM, true)
        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        TEST_ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val triggerAt = System.currentTimeMillis() + TEST_ALARM_DELAY_MS
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
}

private const val TEST_ALARM_REQUEST_CODE = 9101
private const val TEST_ALARM_DELAY_MS = 5_000L
