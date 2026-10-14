param(
    [int] $ExpectedVersionCode = 6,
    [string] $ExpectedVersionName = "0.3.0",
    [string] $ExpectedWeatherProxyBaseUrl = "https://<your-worker>.workers.dev"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

function Assert-File {
    param(
        [string] $Path,
        [string] $Label
    )

    $fullPath = Join-Path $root $Path
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw "$Label missing: $Path"
    }
    $item = Get-Item -LiteralPath $fullPath
    if ($item.Length -le 0) {
        throw "$Label empty: $Path"
    }
    "PLAY_PACKAGE_FILE_OK=$Path bytes=$($item.Length)"
}

function Assert-TextContains {
    param(
        [string] $Path,
        [string] $Needle,
        [string] $Label
    )

    $fullPath = Join-Path $root $Path
    $text = Get-Content -Raw -Encoding UTF8 -LiteralPath $fullPath
    if (-not $text.Contains($Needle)) {
        throw "$Label missing expected text in $Path"
    }
    "PLAY_PACKAGE_TEXT_OK=$Path label=$Label"
}

function Assert-TextHasHangul {
    param(
        [string] $Path,
        [string] $Label
    )

    $fullPath = Join-Path $root $Path
    $text = Get-Content -Raw -Encoding UTF8 -LiteralPath $fullPath
    $hasHangul = $false
    foreach ($char in $text.ToCharArray()) {
        $code = [int][char] $char
        if ($code -ge 0xAC00 -and $code -le 0xD7A3) {
            $hasHangul = $true
            break
        }
    }
    if (-not $hasHangul) {
        throw "$Label has no Hangul text in $Path"
    }
    "PLAY_PACKAGE_HANGUL_OK=$Path label=$Label"
}

function Assert-NoReplacementCharacter {
    param(
        [string] $Path
    )

    $fullPath = Join-Path $root $Path
    $text = Get-Content -Raw -Encoding UTF8 -LiteralPath $fullPath
    $replacementChar = [string][char]0xFFFD
    if ($text.Contains($replacementChar)) {
        throw "Unicode replacement character found in $Path"
    }
    "PLAY_PACKAGE_ENCODING_OK=$Path"
}

Assert-File "app\build\outputs\bundle\release\app-release.aab" "Release AAB"
Assert-File "store-assets\feature-graphic.png" "Feature graphic"
Assert-File "store-assets\screenshots-clean\01-alarm-list.png" "Screenshot 1"
Assert-File "store-assets\screenshots-clean\02-alarm-edit.png" "Screenshot 2"
Assert-File "store-assets\screenshots-clean\03-time-picker.png" "Screenshot 3"
Assert-File "store-assets\screenshots-clean\04-location-search.png" "Screenshot 4"
Assert-File "store-assets\screenshots-clean\05-weather-rules.png" "Screenshot 5"
Assert-File "docs\store-listing-draft.md" "Store listing draft"
Assert-File "docs\play-store-privacy-draft.md" "Privacy draft"
Assert-File "docs\play-console-permission-declarations.md" "Permission declaration draft"
Assert-File "docs\release-notes.md" "Release notes"
Assert-File "docs\play-console-submission-pack.md" "Submission pack"
Assert-File "docs\play-console-form-answers.md" "Play Console form answers"
Assert-File "app\src\main\res\values-ko\strings.xml" "Korean strings"

Assert-TextHasHangul "docs\store-listing-draft.md" "store listing Korean"
Assert-TextHasHangul "docs\release-notes.md" "release notes Korean"
Assert-TextHasHangul "docs\play-console-submission-pack.md" "submission pack Korean"
Assert-TextHasHangul "docs\play-console-form-answers.md" "form answers Korean"
Assert-TextHasHangul "app\src\main\res\values-ko\strings.xml" "Android Korean resources"
Assert-TextContains "docs\play-console-submission-pack.md" $ExpectedWeatherProxyBaseUrl "submission pack proxy"
Assert-TextContains "docs\play-console-submission-pack.md" "<CONTACT_EMAIL>" "developer contact"

Assert-NoReplacementCharacter "docs\store-listing-draft.md"
Assert-NoReplacementCharacter "docs\release-notes.md"
Assert-NoReplacementCharacter "docs\play-console-submission-pack.md"
Assert-NoReplacementCharacter "docs\play-console-form-answers.md"
Assert-NoReplacementCharacter "app\src\main\res\values-ko\strings.xml"

& (Join-Path $root "scripts\verify-release-artifacts.ps1") `
    -ExpectedVersionCode $ExpectedVersionCode `
    -ExpectedVersionName $ExpectedVersionName `
    -ExpectedWeatherProxyBaseUrl $ExpectedWeatherProxyBaseUrl `
    -RequireWeatherProxy
$verifySucceeded = $?
if (-not $verifySucceeded) {
    throw "Release artifact verification failed."
}

& (Join-Path $root "scripts\validate-store-assets.ps1")
$assetsSucceeded = $?
if (-not $assetsSucceeded) {
    throw "Store asset validation failed."
}

"PLAY_CONSOLE_PACKAGE_OK=1"
