param(
    [Parameter(Mandatory = $true)]
    [string] $BaseUrl,

    [double] $Latitude = 37.5665,
    [double] $Longitude = 126.9780,
    [string] $Target = "",
    [string] $Locale = "ko-KR",
    [string] $ClientId = "install_smoke_test",
    [switch] $SkipPrivacy,
    [switch] $RequirePrivacyContact
)

$ErrorActionPreference = "Stop"

$base = $BaseUrl.TrimEnd("/")
if (-not $Target.Trim()) {
    $Target = (Get-Date).Date.AddDays(1).AddHours(7).ToString("yyyy-MM-ddTHH:mm:ss")
}
$queryParts = @(
    "latitude=$([Uri]::EscapeDataString($Latitude.ToString([System.Globalization.CultureInfo]::InvariantCulture)))"
    "longitude=$([Uri]::EscapeDataString($Longitude.ToString([System.Globalization.CultureInfo]::InvariantCulture)))"
    "target=$([Uri]::EscapeDataString($Target))"
    "locale=$([Uri]::EscapeDataString($Locale))"
)
$uri = "$base/weather/forecast?$($queryParts -join '&')"

$response = Invoke-WebRequest `
    -Uri $uri `
    -Headers @{ "x-weather-client-id" = $ClientId } `
    -UseBasicParsing `
    -TimeoutSec 20

$json = $response.Content | ConvertFrom-Json
if (-not $json.condition) {
    throw "Missing condition in proxy response."
}
if (-not $json.source) {
    throw "Missing source in proxy response."
}
if ($null -eq $json.precipitationProbability) {
    throw "Missing precipitationProbability in proxy response."
}
if ($null -eq $json.providerAttempts) {
    throw "Missing providerAttempts in proxy response."
}

"PROXY_SMOKE_STATUS=$($response.StatusCode)"
"PROXY_SMOKE_CONDITION=$($json.condition)"
"PROXY_SMOKE_SOURCE=$($json.source)"
"PROXY_SMOKE_PRECIPITATION=$($json.precipitationProbability)"
"PROXY_SMOKE_PROVIDER_ATTEMPTS=$($json.providerAttempts.Count)"
"PROXY_SMOKE_CACHE=$($response.Headers['x-weather-cache'])"

if (-not $SkipPrivacy) {
    $privacyUri = "$base/privacy"
    $privacyResponse = Invoke-WebRequest `
        -Uri $privacyUri `
        -UseBasicParsing `
        -TimeoutSec 20

    if ($privacyResponse.StatusCode -ne 200) {
        throw "Privacy route returned HTTP $($privacyResponse.StatusCode)."
    }
    if (($privacyResponse.Headers["content-type"] -join ",") -notmatch "text/html") {
        throw "Privacy route did not return HTML."
    }
    if ($privacyResponse.Content -notmatch "Weather Morning Alarm Privacy Policy") {
        throw "Privacy route missing policy title."
    }
    if ($privacyResponse.Content -notmatch "App data is excluded from Android backup") {
        throw "Privacy route missing Android backup exclusion text."
    }
    if ($RequirePrivacyContact -and $privacyResponse.Content -match "Contact email must be added") {
        throw "Privacy contact email is still the placeholder."
    }
    if ($RequirePrivacyContact -and $privacyResponse.Content -notmatch "[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}") {
        throw "Privacy contact email is missing."
    }

    "PRIVACY_SMOKE_STATUS=$($privacyResponse.StatusCode)"
    "PRIVACY_SMOKE_CONTENT_TYPE=$($privacyResponse.Headers['content-type'])"
    "PRIVACY_SMOKE_TITLE=OK"
    "PRIVACY_SMOKE_BACKUP_EXCLUSION=OK"
    if ($RequirePrivacyContact) {
        "PRIVACY_SMOKE_CONTACT=OK"
    }
}
