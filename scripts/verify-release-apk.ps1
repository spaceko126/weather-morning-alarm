param(
    [string] $ApkPath = "app\build\outputs\apk\release\app-release.apk",
    [int] $ExpectedVersionCode = 0,
    [string] $ExpectedVersionName = "",
    [string] $ExpectedWeatherProxyBaseUrl = "",
    [switch] $RequireWeatherProxy
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $root $ApkPath
$manifestPath = Join-Path $root "app\build\intermediates\merged_manifests\release\processReleaseManifest\AndroidManifest.xml"
$buildConfigPath = Join-Path $root "app\build\generated\source\buildConfig\release\com\weathermorning\alarm\BuildConfig.java"
$releaseLintPath = Join-Path $root "app\build\intermediates\lint_vital_intermediate_text_report\release\lintVitalReportRelease\lint-results-release.txt"
$buildTools = Get-ChildItem (Join-Path $root ".tools\android-sdk\build-tools") -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
if ($null -eq $buildTools) {
    throw "Android build-tools not found."
}
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
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

Assert-True (Test-Path $apk) "Release APK not found: $apk"
Assert-True ((Get-Item $apk).Length -gt 1MB) "Release APK looks too small: $apk"
Assert-True (Test-Path $manifestPath) "Merged release manifest not found: $manifestPath"
Assert-True (Test-Path $buildConfigPath) "Release BuildConfig not found: $buildConfigPath"
Assert-True (Test-Path $apksigner) "apksigner not found: $apksigner"

$previousErrorActionPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = "Continue"
    $apksignerOutput = & $apksigner verify --verbose --print-certs $apk 2>&1
    $apksignerExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($apksignerExitCode -ne 0) {
    $apksignerOutput | ForEach-Object { $_ }
    throw "APK signature verification failed."
}
Assert-True (($apksignerOutput -join "`n") -match "Verified using v2 scheme") "APK must be signed with v2 scheme."

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

"RELEASE_APK_OK=$apk"
"RELEASE_APK_BYTES=$((Get-Item $apk).Length)"
"RELEASE_APK_SIGNATURE=OK"
"RELEASE_PACKAGE=$($manifest.manifest.package)"
"RELEASE_VERSION_CODE=$versionCode"
"RELEASE_VERSION_NAME=$versionName"
"RELEASE_PROXY_URL=$proxyUrl"
"RELEASE_PERMISSIONS=$($permissions -join ',')"
($apksignerOutput | Where-Object { $_ -match "Signer #1 certificate SHA-256 digest" } | ForEach-Object { "RELEASE_APK_CERT_$($_)" })
