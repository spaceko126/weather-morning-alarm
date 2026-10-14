param(
    [string]$PackageName = "com.weathermorning.alarm",
    [string]$KeyPath = "",
    [string]$Serial = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $root ".tools\android-sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    throw "adb not found: $adb"
}

if ([string]::IsNullOrWhiteSpace($KeyPath)) {
    $keyFile = Get-ChildItem -LiteralPath "$env:USERPROFILE\Desktop" |
        Where-Object { $_.Extension -eq ".txt" -or $_.Name -like "*key*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($keyFile) {
        $KeyPath = $keyFile.FullName
    }
}

if ([string]::IsNullOrWhiteSpace($KeyPath) -or -not (Test-Path -LiteralPath $KeyPath)) {
    throw "KMA key file not found: $KeyPath"
}

$key = (Get-Content -Raw -Encoding UTF8 -LiteralPath $KeyPath).Trim()
if ([string]::IsNullOrWhiteSpace($key)) {
    throw "KMA key file is empty: $KeyPath"
}

function Escape-Xml {
    param([string]$Value)
    return [System.Security.SecurityElement]::Escape($Value)
}

$adbArgs = @()
if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    $adbArgs += @("-s", $Serial)
}

& $adb @adbArgs shell pm list packages $PackageName | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Package not available on device: $PackageName"
}

& $adb @adbArgs shell "am force-stop $PackageName" | Out-Null
& $adb @adbArgs shell "run-as $PackageName sh -c 'mkdir -p shared_prefs'"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create app shared_prefs directory"
}

$prefs = @"
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="provider">KMA</string>
    <string name="kma_service_key">$(Escape-Xml $key)</string>
</map>
"@

$prefs | & $adb @adbArgs shell "run-as $PackageName sh -c 'cat > shared_prefs/weather_api_settings.xml'"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to write weather API settings"
}

Write-Host "KMA key installed into app private settings. Key value was not printed."
