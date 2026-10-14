param(
    [string]$PackageName = "com.weathermorning.alarm"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jdk = Join-Path $root ".tools\jdk-17"
$gradle = Join-Path $root "gradlew.bat"
$adb = Join-Path $root ".tools\android-sdk\platform-tools\adb.exe"

if (-not (Test-Path (Join-Path $jdk "bin\java.exe"))) {
    throw "Local JDK 17 not found: $jdk"
}

if (-not (Test-Path $adb)) {
    throw "adb not found: $adb"
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
    Write-Host "Connect Galaxy by USB and enable USB debugging."
    exit 2
}

$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH

try {
    $env:JAVA_HOME = $jdk
    $env:PATH = (Join-Path $jdk "bin") + [System.IO.Path]::PathSeparator + $env:PATH
    & $gradle connectedDebugAndroidTest --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed: connectedDebugAndroidTest"
    }
} finally {
    if ($null -eq $originalJavaHome) {
        Remove-Item Env:\JAVA_HOME -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_HOME = $originalJavaHome
    }
    $env:PATH = $originalPath
}

Write-Host "Device instrumentation tests passed for $PackageName."
