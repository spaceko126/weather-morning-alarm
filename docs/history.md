# Alarm History

## Current Scope

The app stores a lightweight local history in SharedPreferences.

Entry types:

- `PRECHECK`: weather precheck completed and decision saved.
- `RING`: actual alarm receiver rang.
- `SKIP`: actual alarm receiver skipped because condition was not met.
- `TEST`: receiver-based test alarm fired.

Stored fields:

- id
- occurred time
- type
- reason
- weather condition
- precipitation probability
- source

## Storage

Current storage:

```text
LocalAlarmHistoryStore -> Room -> alarm_history
```

Limit:

```text
50 entries
```

## UI

The main screen observes Room through `HistoryViewModel` and shows the latest 10
entries.

- Clear

## Known Limits

- History is embedded in the main settings screen.
- No filter/detail view yet.
- Test alarm currently uses `PendingIntent.getBroadcast(...).send()` to exercise
  `AlarmReceiver`.
