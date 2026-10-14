param(
    [int] $VersionCode = 6,
    [string] $VersionName = "0.3.0",
    [string] $WeatherProxyBaseUrl = "https://<your-worker>.workers.dev"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$targetRoot = Join-Path $root "release-submission"
$target = Join-Path $targetRoot "play-console-v$VersionCode-$VersionName"

function Assert-InWorkspace {
    param([string] $Path)
    $resolvedRoot = [System.IO.Path]::GetFullPath($root)
    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $resolvedPath.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside workspace: $resolvedPath"
    }
}

function Copy-SubmissionFile {
    param(
        [string] $Source,
        [string] $Destination
    )

    $src = Join-Path $root $Source
    if (-not (Test-Path -LiteralPath $src -PathType Leaf)) {
        throw "Missing required submission file: $Source"
    }
    $dest = Join-Path $target $Destination
    $destDir = Split-Path -Parent $dest
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
    Copy-Item -LiteralPath $src -Destination $dest -Force
    $item = Get-Item -LiteralPath $dest
    "SUBMISSION_FILE=$Destination bytes=$($item.Length)"
}

& (Join-Path $root "scripts\check-play-console-package.ps1") `
    -ExpectedVersionCode $VersionCode `
    -ExpectedVersionName $VersionName `
    -ExpectedWeatherProxyBaseUrl $WeatherProxyBaseUrl
$packageCheckSucceeded = $?
if (-not $packageCheckSucceeded) {
    throw "Play Console package check failed."
}

Assert-InWorkspace $targetRoot
Assert-InWorkspace $target

if (Test-Path -LiteralPath $target) {
    Remove-Item -LiteralPath $target -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $target | Out-Null

Copy-SubmissionFile "app\build\outputs\bundle\release\app-release.aab" "upload\app-release.aab"
Copy-SubmissionFile "store-assets\feature-graphic.png" "graphics\feature-graphic.png"
Copy-SubmissionFile "store-assets\screenshots-clean\01-alarm-list.png" "graphics\screenshots\01-alarm-list.png"
Copy-SubmissionFile "store-assets\screenshots-clean\02-alarm-edit.png" "graphics\screenshots\02-alarm-edit.png"
Copy-SubmissionFile "store-assets\screenshots-clean\03-time-picker.png" "graphics\screenshots\03-time-picker.png"
Copy-SubmissionFile "store-assets\screenshots-clean\04-location-search.png" "graphics\screenshots\04-location-search.png"
Copy-SubmissionFile "store-assets\screenshots-clean\05-weather-rules.png" "graphics\screenshots\05-weather-rules.png"
Copy-SubmissionFile "docs\play-console-submission-pack.md" "docs\play-console-submission-pack.md"
Copy-SubmissionFile "docs\play-console-form-answers.md" "docs\play-console-form-answers.md"
Copy-SubmissionFile "docs\store-listing-draft.md" "docs\store-listing-draft.md"
Copy-SubmissionFile "docs\play-store-privacy-draft.md" "docs\play-store-privacy-draft.md"
Copy-SubmissionFile "docs\play-console-permission-declarations.md" "docs\play-console-permission-declarations.md"
Copy-SubmissionFile "docs\release-notes.md" "docs\release-notes.md"
Copy-SubmissionFile "docs\privacy-policy-public.md" "docs\privacy-policy-public.md"

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

$manifest = @(
    "Weather Morning Alarm Play Console Submission",
    "Generated: $(Get-Date -Format s)",
    "Package: com.weathermorning.alarm",
    "versionCode: $VersionCode",
    "versionName: $VersionName",
    "Production backend: $WeatherProxyBaseUrl",
    "Privacy policy: https://<your-worker>.workers.dev/privacy",
    "",
    "Upload AAB:",
    "upload\app-release.aab",
    "",
    "Graphics:",
    "graphics\feature-graphic.png",
    "graphics\screenshots\*.png",
    "",
    "Docs:",
    "docs\play-console-submission-pack.md",
    "docs\play-console-form-answers.md",
    "docs\store-listing-draft.md",
    "docs\play-store-privacy-draft.md",
    "docs\play-console-permission-declarations.md",
    "docs\release-notes.md",
    "docs\privacy-policy-public.md",
    "",
    "Private signing files are intentionally excluded."
)

$manifestPath = Join-Path $target "MANIFEST.txt"
$manifest | Set-Content -LiteralPath $manifestPath -Encoding UTF8

"PLAY_SUBMISSION_PACKAGE_OK=1"
"PLAY_SUBMISSION_PACKAGE=$target"
