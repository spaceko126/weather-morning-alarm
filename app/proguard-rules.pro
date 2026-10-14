# Compose and AndroidX ship their own consumer rules; these cover the pieces
# this app reaches by name rather than by reference.

# Entry points named only in AndroidManifest.xml.
-keep class com.weathermorning.alarm.alarm.AlarmService { *; }
-keep class com.weathermorning.alarm.alarm.AlarmReceiver { *; }
-keep class com.weathermorning.alarm.alarm.WeatherPrecheckReceiver { *; }
-keep class com.weathermorning.alarm.alarm.AlarmRescheduleReceiver { *; }
-keep class com.weathermorning.alarm.presentation.MainActivity { *; }
-keep class com.weathermorning.alarm.presentation.AlarmActivity { *; }

# WorkManager instantiates workers reflectively.
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }

# Enum names are persisted in SharedPreferences and read back with valueOf().
-keepclassmembers enum com.weathermorning.alarm.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# org.json is part of the platform; keep parser reflection quiet.
-dontwarn org.json.**
