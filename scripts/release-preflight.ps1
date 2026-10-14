param(
    [int] $VersionCode = 6,
    [string] $VersionName = "0.3.0",
    [string] $WeatherProxyBaseUrl = "",
    [switch] $AllowNoWeatherProxy,
    [switch] $IncludeDeviceTests
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend\weather-proxy"
$gradle = Join-Path $root "gradlew.bat"

if (-not $AllowNoWeatherProxy) {
    if (-not $WeatherProxyBaseUrl.Trim()) {
        throw "Missing WeatherProxyBaseUrl. Use -AllowNoWeatherProxy only for an intentional local no-proxy preflight."
    }
    $proxyUri = $null
    if (-not [System.Uri]::TryCreate($WeatherProxyBaseUrl, [System.UriKind]::Absolute, [ref] $proxyUri) -or $proxyUri.Scheme -ne "https") {
        throw "WeatherProxyBaseUrl must be an absolute HTTPS URL."
    }
}

Push-Location $backend
try {
    npm test
    if ($LASTEXITCODE -ne 0) {
        throw "Backend tests failed."
    }
    npm run dryrun:staging
    if ($LASTEXITCODE -ne 0) {
        throw "Wrangler staging dry-run failed."
    }
    npm run dryrun:production
    if ($LASTEXITCODE -ne 0) {
        throw "Wrangler production dry-run failed."
    }
} finally {
    Pop-Location
}

& (Join-Path $root "scripts\test-debug.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Debug validation failed."
}

& (Join-Path $root "scripts\process-store-screenshots.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Store screenshot processing failed."
}

& (Join-Path $root "scripts\validate-store-assets.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Store asset validation failed."
}

& (Join-Path $root "scripts\check-sensitive-files.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Sensitive file check failed."
}

if ($IncludeDeviceTests) {
    & (Join-Path $root "scripts\test-device.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Device validation failed."
    }
}

$gradleArgs = @(
    "bundleRelease",
    "-PappVersionCode=$VersionCode",
    "-PappVersionName=$VersionName",
    "--console=plain"
)
if ($WeatherProxyBaseUrl.Trim()) {
    $gradleArgs += "-PweatherProxyBaseUrl=$WeatherProxyBaseUrl"
}
& $gradle @gradleArgs
if ($LASTEXITCODE -ne 0) {
    throw "Release bundle build failed."
}

$verifyScript = Join-Path $root "scripts\verify-release-artifacts.ps1"
if ($WeatherProxyBaseUrl.Trim() -and -not $AllowNoWeatherProxy) {
    & $verifyScript `
        -ExpectedVersionCode $VersionCode `
        -ExpectedVersionName $VersionName `
        -ExpectedWeatherProxyBaseUrl $WeatherProxyBaseUrl `
        -RequireWeatherProxy
} elseif ($WeatherProxyBaseUrl.Trim()) {
    & $verifyScript `
        -ExpectedVersionCode $VersionCode `
        -ExpectedVersionName $VersionName `
        -ExpectedWeatherProxyBaseUrl $WeatherProxyBaseUrl
} elseif (-not $AllowNoWeatherProxy) {
    & $verifyScript `
        -ExpectedVersionCode $VersionCode `
        -ExpectedVersionName $VersionName `
        -RequireWeatherProxy
} else {
    & $verifyScript `
        -ExpectedVersionCode $VersionCode `
        -ExpectedVersionName $VersionName
}
if ($LASTEXITCODE -ne 0) {
    throw "Release artifact verification failed."
}

"RELEASE_PREFLIGHT_OK=1"
"RELEASE_PREFLIGHT_VERSION_CODE=$VersionCode"
"RELEASE_PREFLIGHT_VERSION_NAME=$VersionName"
"RELEASE_PREFLIGHT_PROXY=$WeatherProxyBaseUrl"
