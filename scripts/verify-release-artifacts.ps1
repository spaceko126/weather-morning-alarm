param(
    [string] $AabPath = "app\build\outputs\bundle\release\app-release.aab",
    [int] $ExpectedVersionCode = 0,
    [string] $ExpectedVersionName = "",
    [string] $ExpectedWeatherProxyBaseUrl = "",
    [switch] $RequireWeatherProxy
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$aab = Join-Path $root $AabPath
$manifestPath = Join-Path $root "app\build\intermediates\merged_manifests\release\processReleaseManifest\AndroidManifest.xml"
$buildConfigPath = Join-Path $root "app\build\generated\source\buildConfig\release\com\weathermorning\alarm\BuildConfig.java"
$releaseLintPath = Join-Path $root "app\build\intermediates\lint_vital_intermediate_text_report\release\lintVitalReportRelease\lint-results-release.txt"
$androidNs = "http://schemas.android.com/apk/res/android"

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Get-AndroidAttr {
    param(
        [System.Xml.XmlElement] $Node,
        [string] $Name
    )
    return $Node.GetAttribute($Name, $androidNs)
}

function Get-BuildConfigString {
    param(
        [string] $Content,
        [string] $Name
    )
    $pattern = "public static final String $Name = `"([^`"]*)`";"
    if ($Content -match $pattern) {
        return $Matches[1]
    }
    return $null
}

function Get-BuildConfigInt {
    param(
        [string] $Content,
        [string] $Name
    )
    $pattern = "public static final int $Name = ([0-9]+);"
    if ($Content -match $pattern) {
        return [int] $Matches[1]
    }
    return $null
}

Assert-True (Test-Path $aab) "Release AAB not found: $aab"
Assert-True ((Get-Item $aab).Length -gt 1MB) "Release AAB looks too small: $aab"
Assert-True (Test-Path $manifestPath) "Merged release manifest not found: $manifestPath"
Assert-True (Test-Path $buildConfigPath) "Release BuildConfig not found: $buildConfigPath"

[xml] $manifest = Get-Content -Raw $manifestPath
$application = [System.Xml.XmlElement] $manifest.manifest.application
$permissions = @($manifest.manifest.'uses-permission' | ForEach-Object { Get-AndroidAttr $_ "name" })
$buildConfig = Get-Content -Raw $buildConfigPath

Assert-True ($manifest.manifest.package -eq "com.weathermorning.alarm") "Unexpected package: $($manifest.manifest.package)"
Assert-True ($manifest.manifest.'uses-sdk'.GetAttribute("minSdkVersion", $androidNs) -eq "26") "Unexpected minSdkVersion."
Assert-True ($manifest.manifest.'uses-sdk'.GetAttribute("targetSdkVersion", $androidNs) -eq "36") "Unexpected targetSdkVersion."
Assert-True ((Get-AndroidAttr $application "allowBackup") -eq "false") "allowBackup must be false."
Assert-True ((Get-AndroidAttr $application "dataExtractionRules") -eq "@xml/data_extraction_rules") "Missing dataExtractionRules."
Assert-True ((Get-AndroidAttr $application "fullBackupContent") -eq "@xml/backup_rules") "Missing fullBackupContent backup rules."

$requiredPermissions = @(
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.SCHEDULE_EXACT_ALARM",
    "android.permission.USE_FULL_SCREEN_INTENT",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.WAKE_LOCK",
    "android.permission.VIBRATE",
    "android.permission.INTERNET",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_FINE_LOCATION"
)
foreach ($permission in $requiredPermissions) {
    Assert-True ($permissions -contains $permission) "Missing permission: $permission"
}
Assert-True (-not ($permissions -contains "android.permission.USE_EXACT_ALARM")) "USE_EXACT_ALARM must not be requested."

$mainActivity = @($application.activity | Where-Object { (Get-AndroidAttr $_ "name") -eq "com.weathermorning.alarm.presentation.MainActivity" })[0]
$alarmActivity = @($application.activity | Where-Object { (Get-AndroidAttr $_ "name") -eq "com.weathermorning.alarm.presentation.AlarmActivity" })[0]
Assert-True ($null -ne $mainActivity) "MainActivity missing."
Assert-True ($null -ne $alarmActivity) "AlarmActivity missing."
Assert-True ((Get-AndroidAttr $mainActivity "exported") -eq "true") "MainActivity must be exported for launcher."
Assert-True ((Get-AndroidAttr $alarmActivity "exported") -eq "false") "AlarmActivity must not be exported."

foreach ($receiverName in @(
    "com.weathermorning.alarm.alarm.AlarmReceiver",
    "com.weathermorning.alarm.alarm.WeatherPrecheckReceiver",
    "com.weathermorning.alarm.alarm.AlarmRescheduleReceiver"
)) {
    $receiver = @($application.receiver | Where-Object { (Get-AndroidAttr $_ "name") -eq $receiverName })[0]
    Assert-True ($null -ne $receiver) "Receiver missing: $receiverName"
    Assert-True ((Get-AndroidAttr $receiver "exported") -eq "false") "Receiver must not be exported: $receiverName"
}

# WM-116: the alarm rings from this service, so a release that lost it or its
# foreground type would be silent whenever the ring screen is not shown.
$alarmService = @($application.service | Where-Object { (Get-AndroidAttr $_ "name") -eq "com.weathermorning.alarm.alarm.AlarmService" })[0]
Assert-True ($null -ne $alarmService) "AlarmService missing."
Assert-True ((Get-AndroidAttr $alarmService "exported") -eq "false") "AlarmService must not be exported."
Assert-True ((Get-AndroidAttr $alarmService "foregroundServiceType") -eq "mediaPlayback") "AlarmService needs foregroundServiceType=mediaPlayback."
"RELEASE_ALARM_SERVICE=OK"

$versionCode = Get-BuildConfigInt $buildConfig "VERSION_CODE"
$versionName = Get-BuildConfigString $buildConfig "VERSION_NAME"
$proxyUrl = Get-BuildConfigString $buildConfig "WEATHER_PROXY_BASE_URL"
if ($ExpectedVersionCode -gt 0) {
    Assert-True ($versionCode -eq $ExpectedVersionCode) "Unexpected VERSION_CODE: $versionCode"
}
if ($ExpectedVersionName.Trim()) {
    Assert-True ($versionName -eq $ExpectedVersionName) "Unexpected VERSION_NAME: $versionName"
}
if ($RequireWeatherProxy) {
    Assert-True ($proxyUrl -match "^https://") "Release BuildConfig WEATHER_PROXY_BASE_URL must be HTTPS."
}
if ($ExpectedWeatherProxyBaseUrl.Trim()) {
    Assert-True ($proxyUrl -eq $ExpectedWeatherProxyBaseUrl.TrimEnd("/")) "Unexpected WEATHER_PROXY_BASE_URL: $proxyUrl"
}

if (Test-Path $releaseLintPath) {
    $lint = Get-Content -Raw $releaseLintPath
    Assert-True ($lint -match "No issues found\.") "Release lint vital report has issues: $releaseLintPath"
}

"RELEASE_AAB_OK=$aab"
"RELEASE_AAB_BYTES=$((Get-Item $aab).Length)"
"RELEASE_PACKAGE=$($manifest.manifest.package)"
"RELEASE_VERSION_CODE=$versionCode"
"RELEASE_VERSION_NAME=$versionName"
"RELEASE_PROXY_URL=$proxyUrl"
"RELEASE_PERMISSIONS=$($permissions -join ',')"
"RELEASE_BACKUP_EXCLUDED=OK"
"RELEASE_EXPORTED_COMPONENTS=OK"
"RELEASE_LINT=OK"
