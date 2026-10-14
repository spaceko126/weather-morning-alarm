param(
    [string] $WeatherProxyBaseUrl = "",
    [int] $VersionCode = 1,
    [string] $VersionName = "0.1.0",
    [switch] $AllowNoWeatherProxy
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jdk = Join-Path $root ".tools\jdk-17"
$gradle = Join-Path $root "gradlew.bat"
$keystoreProperties = Join-Path $root "keystore.properties"

if (-not (Test-Path (Join-Path $jdk "bin\java.exe"))) {
    throw "Local JDK 17 not found: $jdk"
}
if (-not $AllowNoWeatherProxy) {
    if (-not $WeatherProxyBaseUrl.Trim()) {
        throw "Missing WeatherProxyBaseUrl. Production APK builds should use the deployed backend proxy. Use -AllowNoWeatherProxy only for an intentional no-proxy APK."
    }
    $proxyUri = $null
    if (-not [System.Uri]::TryCreate($WeatherProxyBaseUrl, [System.UriKind]::Absolute, [ref] $proxyUri) -or $proxyUri.Scheme -ne "https") {
        throw "WeatherProxyBaseUrl must be an absolute HTTPS URL."
    }
}
if (-not (Test-Path $keystoreProperties)) {
    throw "Missing keystore.properties. Copy keystore.properties.example, fill real values, and keep it private."
}

$gradleArgs = @(
    "assembleRelease",
    "-PappVersionCode=$VersionCode",
    "-PappVersionName=$VersionName"
)
if ($WeatherProxyBaseUrl.Trim()) {
    $gradleArgs += "-PweatherProxyBaseUrl=$WeatherProxyBaseUrl"
}

$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH

try {
    $env:JAVA_HOME = $jdk
    $env:PATH = (Join-Path $jdk "bin") + [System.IO.Path]::PathSeparator + $env:PATH
    & $gradle @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed: $($gradleArgs -join ' ')"
    }
} finally {
    if ($null -eq $originalJavaHome) {
        Remove-Item Env:\JAVA_HOME -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_HOME = $originalJavaHome
    }
    $env:PATH = $originalPath
}

"RELEASE_APK=app\build\outputs\apk\release\app-release.apk"
