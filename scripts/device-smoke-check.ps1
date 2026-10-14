param(
    [string]$PackageName = "com.weathermorning.alarm",
    [string]$LauncherActivity = ".presentation.MainActivity",
    [string[]]$RequiredText = @(),
    [switch]$Launch,
    [switch]$SaveLog
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $root ".tools\android-sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    throw "adb not found"
}

$deviceLines = & $adb devices
$authorizedDevices = $deviceLines | Select-String -Pattern "`tdevice$"
$unauthorizedDevices = $deviceLines | Select-String -Pattern "`tunauthorized$"

Write-Host "Devices:"
& $adb devices -l

if ($unauthorizedDevices) {
    Write-Host "`nDevice connected but unauthorized. Unlock phone and accept USB debugging RSA prompt."
    exit 3
}

if (-not $authorizedDevices) {
    Write-Host "`nNo authorized Android device found."
    exit 2
}

Write-Host "`nDevice model:"
& $adb shell getprop ro.product.manufacturer
& $adb shell getprop ro.product.model
& $adb shell getprop ro.build.version.release
& $adb shell getprop ro.build.version.sdk

Write-Host "`nPackage:"
& $adb shell pm list packages $PackageName

Write-Host "`nGranted permissions:"
& $adb shell dumpsys package $PackageName | Select-String -Pattern "POST_NOTIFICATIONS|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|SCHEDULE_EXACT_ALARM"

Write-Host "`nExact alarm appops:"
& $adb shell appops get $PackageName SCHEDULE_EXACT_ALARM 2>$null

if ($Launch) {
    Write-Host "`nLaunch:"
    & $adb shell am start -n "$PackageName/$LauncherActivity"
    Start-Sleep -Seconds 1

    Write-Host "`nFocused app:"
    & $adb shell dumpsys window |
        Select-String -Pattern "mCurrentFocus|mFocusedApp" |
        Select-Object -First 5

    Write-Host "`nRequired UI text:"
    $xml = & $adb exec-out uiautomator dump /dev/tty
    foreach ($text in $RequiredText) {
        if ($xml -match [regex]::Escape($text)) {
            Write-Host "OK $text"
        } else {
            Write-Host "MISS $text"
        }
    }

    Write-Host "`nMainActivity stack records:"
    $hist = & $adb shell dumpsys activity activities |
        Select-String -Pattern "\* Hist\s+#\d+: ActivityRecord\{.*$PackageName/$LauncherActivity"
    Write-Host $hist.Count
}

Write-Host "`nRecent app logs:"
& $adb logcat -d -t 200 | Select-String -Pattern $PackageName

if ($SaveLog) {
    $outDir = Join-Path $root "device-logs"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $outFile = Join-Path $outDir "$stamp-$PackageName-logcat.txt"
    & $adb logcat -d -t 1000 > $outFile
    Write-Host "`nSaved logcat:"
    Write-Host $outFile
}
