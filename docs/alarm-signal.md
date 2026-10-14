# Alarm Signal

## Current Behavior

When `AlarmActivity` opens:

- screen stays awake.
- lockscreen display is requested.
- default system alarm tone loops.
- vibration pattern repeats.

When user presses `Stop` or the activity is destroyed:

- ringtone stops.
- vibration stops.
- alarm notification is canceled.

## Implementation

Signal control lives in:

```text
app/src/main/java/com/weathermorning/alarm/alarm/AlarmSignalController.kt
```

It uses:

- `RingtoneManager.TYPE_ALARM`
- `MediaPlayer`
- `AudioAttributes.USAGE_ALARM`
- `VibrationEffect.createWaveform`

The notification fallback channel also uses alarm audio attributes and vibration.

## Known Limits

- No custom alarm sound picker yet.
- No volume ramp-up.
- No snooze.
- No foreground service.
- Needs Galaxy lockscreen/audio-mode testing.

