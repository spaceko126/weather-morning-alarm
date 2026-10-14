# Scheduling Tests

## Goal

Keep alarm date calculation testable without Android framework classes.

## Current Structure

- `AndroidAlarmScheduler`
  - Owns `AlarmManager`, `PendingIntent`, precheck alarm, and final alarm scheduling.
- `NextAlarmCalculator`
  - Pure JVM calculation for next eligible alarm date/time.
  - Returns `null` when alarm is disabled.
  - Returns `null` when repeat day set is empty.

## Unit Tests

`NextAlarmCalculatorTest` covers:

- disabled alarm
- empty repeat days
- selected today with future time
- selected today with past time
- nearest selected future weekday

## Local JDK

Gradle 8.13 Android unit test task failed under the machine default JDK 24.
A repo-local Temurin JDK 17 was installed under:

```text
.tools/jdk-17
```

Use:

```powershell
.\scripts\test-debug.ps1
```

The script runs unit tests with JDK 17, then restores the shell environment and
runs `assembleDebug lintDebug` with the default Java environment to avoid Android
lint treating the temporary Java path as a property file issue.

## Validation

```text
.\scripts\test-debug.ps1
testDebugUnitTest: 5 tests, 0 failures, 0 errors
assembleDebug: success
lintDebug: success
```

## Next

- Add tests for precheck timing.
- Add tests for decision engine edge cases.
- Add instrumentation tests after Galaxy connection.
