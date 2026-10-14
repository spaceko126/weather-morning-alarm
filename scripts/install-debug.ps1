param(
    [string]$PackageName = "com.weathermorning.alarm"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sdk = Join-Path $root ".tools\android-sdk"
$adb = Join-Path $sdk "platform-tools\adb.exe"
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $adb)) {
    throw "adb not found: $adb"
}

if (-not (Test-Path $apk)) {
    throw "debug APK not found: $apk"
}

$deviceLines = & $adb devices
$authorizedDevices = $deviceLines | Select-String -Pattern "`tdevice$"
$unauthorizedDevices = $deviceLines | Select-String -Pattern "`tunauthorized$"

if ($unauthorizedDevices) {
    Write-Host "Android device connected but not authorized."
    Write-Host "Unlock phone and accept USB debugging RSA prompt."
    exit 3
}

if (-not $authorizedDevices) {
    Write-Host "No authorized Android device found."
    Write-Host "Connect Galaxy by USB, enable Developer options -> USB debugging, then accept RSA prompt."
    exit 2
}

Write-Host "Installing APK:"
Write-Host $apk
& $adb install -r $apk

# Best-effort runtime permission grants. Some permissions are special app access
# and must still be enabled by the user in Settings.
& $adb shell pm grant $PackageName android.permission.POST_NOTIFICATIONS 2>$null
& $adb shell pm grant $PackageName android.permission.ACCESS_FINE_LOCATION 2>$null
& $adb shell pm grant $PackageName android.permission.ACCESS_COARSE_LOCATION 2>$null
& $adb shell appops set $PackageName SCHEDULE_EXACT_ALARM allow 2>$null

& $adb shell monkey -p $PackageName 1

Write-Host "Installed and launched $PackageName."
Write-Host "In app: check permissions, press Test alarm, press Stop, confirm History updates."
Write-Host "If exact alarm still says needs permission, tap Open alarm permission in the app."
