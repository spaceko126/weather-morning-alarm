param(
    [string]$PackageName = "com.weathermorning.alarm"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $root ".tools\android-sdk\platform-tools\adb.exe"
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$testScript = Join-Path $root "scripts\test-debug.ps1"

function Write-Status($Name, $Ok, $Detail = "") {
    $mark = if ($Ok) { "OK" } else { "FAIL" }
    if ($Detail) {
        Write-Host "$mark $Name - $Detail"
    } else {
        Write-Host "$mark $Name"
    }
}

Write-Host "Weather Morning Alarm device preflight"
Write-Host "Root: $root"
Write-Host ""

Write-Status "adb" (Test-Path $adb) $adb
Write-Status "debug APK" (Test-Path $apk) $apk
Write-Status "full verification script" (Test-Path $testScript) $testScript

if (Test-Path $adb) {
    Write-Host ""
    Write-Host "ADB version:"
    & $adb version

    Write-Host ""
    Write-Host "Connected devices:"
    & $adb devices -l
}

if (Test-Path $apk) {
    $item = Get-Item $apk
    Write-Host ""
    Write-Host "APK:"
    Write-Host "Path: $($item.FullName)"
    Write-Host "Size: $($item.Length) bytes"
    Write-Host "Updated: $($item.LastWriteTime)"
}

Write-Host ""
Write-Host "Before connecting phone:"
Write-Host "1. Any Galaxy/Android test phone is OK, including spare device."
Write-Host "2. Enable Developer options -> USB debugging."
Write-Host "3. Connect USB and accept RSA prompt."
Write-Host "4. Tell Codex: connected."
