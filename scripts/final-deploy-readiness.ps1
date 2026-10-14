param(
    [int] $VersionCode = 6,
    [string] $VersionName = "0.3.0",
    [string] $WeatherProxyBaseUrl = "https://<your-worker>.workers.dev"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$report = Join-Path $root "docs\final-deploy-readiness.md"

function Run-Step {
    param(
        [string] $Name,
        [scriptblock] $Command
    )

    "DEPLOY_READINESS_STEP=$Name"
    & $Command
    $succeeded = $?
    if (-not $succeeded) {
        throw "Deploy readiness step failed: $Name"
    }
    "DEPLOY_READINESS_STEP_OK=$Name"
}

Run-Step "production-proxy-smoke" {
    & (Join-Path $root "scripts\proxy-smoke-check.ps1") `
        -BaseUrl $WeatherProxyBaseUrl `
        -RequirePrivacyContact
}

Run-Step "play-console-package" {
    & (Join-Path $root "scripts\check-play-console-package.ps1") `
        -ExpectedVersionCode $VersionCode `
        -ExpectedVersionName $VersionName `
        -ExpectedWeatherProxyBaseUrl $WeatherProxyBaseUrl
}

Run-Step "submission-folder" {
    & (Join-Path $root "scripts\prepare-play-submission-package.ps1") `
        -VersionCode $VersionCode `
        -VersionName $VersionName `
        -WeatherProxyBaseUrl $WeatherProxyBaseUrl
}

Run-Step "release-preflight" {
    & (Join-Path $root "scripts\release-preflight.ps1") `
        -VersionCode $VersionCode `
        -VersionName $VersionName `
        -WeatherProxyBaseUrl $WeatherProxyBaseUrl
}

$privateFiles = @(
    "keystore.properties",
    "release\weather-morning-upload.jks",
    "release\weather-morning-upload.jks.sha256"
)

$privateStatus = foreach ($path in $privateFiles) {
    $fullPath = Join-Path $root $path
    if (Test-Path -LiteralPath $fullPath) {
        "- Present locally, must be backed up privately: $path"
    } else {
        "- Missing local private file: $path"
    }
}

$submissionPath = "release-submission\play-console-v$VersionCode-$VersionName"
$reportLines = @(
    "# Final Deploy Readiness",
    "",
    "Date: $(Get-Date -Format yyyy-MM-dd)",
    "",
    "## Automated Checks",
    "",
    "- Production proxy smoke with privacy contact: pass",
    "- Play Console package check: pass",
    "- Upload-safe submission folder generation: pass",
    "- Full release preflight: pass",
    "",
    "## Release",
    "",
    "- Package: com.weathermorning.alarm",
    "- Version code: $VersionCode",
    "- Version name: $VersionName",
    "- Production backend: $WeatherProxyBaseUrl",
    "- Privacy policy: https://<your-worker>.workers.dev/privacy",
    "- Upload AAB: app/build/outputs/bundle/release/app-release.aab",
    "- Upload-safe folder: $submissionPath",
    "",
    "## Private Files",
    "",
    $privateStatus,
    "",
    "Do not upload private files to Play Console, GitHub, chat, email, or public storage.",
    "",
    "## Remaining User-Owned Work",
    "",
    "- Final UI approval.",
    "- Private backup of signing files.",
    "- Play Console app creation.",
    "- AAB upload.",
    "- Store listing, Data safety, permission declarations, content rating, and release notes.",
    "- Internal testing install from Play.",
    "",
    "## Go/No-Go",
    "",
    "Local deployment preparation is GO after UI approval and private signing-file backup."
)

$reportLines | Set-Content -LiteralPath $report -Encoding UTF8

"DEPLOY_READINESS_OK=1"
"DEPLOY_READINESS_REPORT=$report"
