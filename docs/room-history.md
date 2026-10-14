# Room History Storage

## Current State

Alarm history now uses Room instead of a SharedPreferences JSON array.

Room files:

```text
app/src/main/java/com/weathermorning/alarm/data/local/db/AppDatabase.kt
app/src/main/java/com/weathermorning/alarm/data/local/db/AlarmHistoryDao.kt
app/src/main/java/com/weathermorning/alarm/data/local/db/AlarmHistoryEntity.kt
app/src/main/java/com/weathermorning/alarm/data/local/LocalAlarmHistoryStore.kt
```

Database:

```text
weather_morning_alarm.db
```

Table:

```text
alarm_history
```

Schema:

```text
app/schemas/com.weathermorning.alarm.data.local.db.AppDatabase/1.json
```

## Build Notes

Room 2.8.4 uses KSP. AGP 9 built-in Kotlin conflicted with KSP in this project,
so the build was moved to:

```text
Android Gradle Plugin 8.13.0
Gradle 8.13
Kotlin 2.2.21
KSP 2.2.21-2.0.5
Room 2.8.4
```

## API Compatibility

`LocalAlarmHistoryStore` kept the same public calls:

```kotlin
append(...)
load()
clear()
```

That kept `AlarmReceiver`, `WeatherCheckWorker`, and `WeatherMorningApp`
changes small.

## Known Limits

- DAO calls are wrapped in `runBlocking(Dispatchers.IO)` to preserve current
  synchronous call sites.
- Long term, UI should observe history through Flow/ViewModel.
- Future schema changes need migrations.

