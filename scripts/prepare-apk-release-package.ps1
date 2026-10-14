param(
    [int] $VersionCode = 6,
    [string] $VersionName = "0.3.0",
    [string] $WeatherProxyBaseUrl = "https://<your-worker>.workers.dev"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$targetRoot = Join-Path $root "release-apk"
$target = Join-Path $targetRoot "weather-morning-v$VersionCode-$VersionName"

function Assert-InWorkspace {
    param([string] $Path)
    $resolvedRoot = [System.IO.Path]::GetFullPath($root)
    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $resolvedPath.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside workspace: $resolvedPath"
    }
}

function Copy-ReleaseFile {
    param(
        [string] $Source,
        [string] $Destination
    )

    $src = Join-Path $root $Source
    if (-not (Test-Path -LiteralPath $src -PathType Leaf)) {
        throw "Missing required APK release file: $Source"
    }
    $dest = Join-Path $target $Destination
    $destDir = Split-Path -Parent $dest
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
    Copy-Item -LiteralPath $src -Destination $dest -Force
    $item = Get-Item -LiteralPath $dest
    "APK_RELEASE_FILE=$Destination bytes=$($item.Length)"
}

& (Join-Path $root "scripts\verify-release-apk.ps1") `
    -ExpectedVersionCode $VersionCode `
    -ExpectedVersionName $VersionName `
    -ExpectedWeatherProxyBaseUrl $WeatherProxyBaseUrl `
    -RequireWeatherProxy
if (-not $?) {
    throw "Release APK verification failed."
}

Assert-InWorkspace $targetRoot
Assert-InWorkspace $target

if (Test-Path -LiteralPath $target) {
    Remove-Item -LiteralPath $target -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $target | Out-Null

Copy-ReleaseFile "app\build\outputs\apk\release\app-release.apk" "weather-morning-v$VersionCode-$VersionName.apk"
Copy-ReleaseFile "docs\privacy-policy-public.md" "docs\privacy-policy-public.md"
Copy-ReleaseFile "docs\release-notes.md" "docs\release-notes.md"

$readme = @(
    "Weather Morning Alarm APK Release",
    "Generated: $(Get-Date -Format s)",
    "Package: com.weathermorning.alarm",
    "versionCode: $VersionCode",
    "versionName: $VersionName",
    "Production backend: $WeatherProxyBaseUrl",
    "Privacy policy: https://<your-worker>.workers.dev/privacy",
    "",
    "Install file:",
    "weather-morning-v$VersionCode-$VersionName.apk",
    "",
    "Android install notes:",
    "1. Copy the APK to the Galaxy phone.",
    "2. Open the APK on the phone.",
    "3. Allow installation from this source if Android asks.",
    "4. Open the app and grant notification, location, exact alarm, and full-screen alarm permissions when prompted.",
    "",
    "Private signing files are intentionally excluded."
)

$readmePath = Join-Path $target "README.txt"
$readme | Set-Content -LiteralPath $readmePath -Encoding UTF8

$forbidden = @(
    "keystore.properties",
    "weather-morning-upload.jks",
    "weather-morning-upload.jks.sha256",
    "KMA_SERVICE_KEY"
)

$files = Get-ChildItem -LiteralPath $target -Recurse -File
foreach ($file in $files) {
    foreach ($marker in $forbidden) {
        if ($file.Name -eq $marker) {
            throw "Forbidden private file included: $($file.FullName)"
        }
    }
}

"APK_RELEASE_PACKAGE_OK=1"
"APK_RELEASE_PACKAGE=$target"
"APK_INSTALL_FILE=$(Join-Path $target "weather-morning-v$VersionCode-$VersionName.apk")"
