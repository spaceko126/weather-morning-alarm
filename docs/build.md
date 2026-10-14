# Build Notes

## Local Toolchain

This project can build with a local, repo-adjacent toolchain:

- Gradle wrapper 8.13
- Android SDK command-line tools `14742923`
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0

The downloaded tools live under `.tools/`, which is ignored by git.

The build currently uses Android Gradle Plugin 8.13.0 because AGP 9 built-in
Kotlin conflicted with KSP during the Room migration.

Unit tests need JDK 17 on this machine. A repo-local Temurin JDK 17 is installed
under `.tools/jdk-17` and used by `scripts/test-debug.ps1`.

## Build Command

```powershell
$env:ANDROID_HOME = (Resolve-Path '.tools\android-sdk').Path
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat assembleDebug
```

## Full Local Verification

```powershell
.\scripts\test-debug.ps1
```

This runs:

- `testDebugUnitTest` with repo-local JDK 17
- `assembleDebug`
- `lintDebug`

## APK Output

```text
app/build/outputs/apk/debug/app-debug.apk
```
