param(
    [string]$PackageName = "com.weathermorning.alarm",
    [string]$LauncherActivity = ".presentation.MainActivity",
    [switch]$RunAlarm
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $root ".tools\android-sdk\platform-tools\adb.exe"
$stringsKo = Join-Path $root "app\src\main\res\values-ko\strings.xml"
$stringsDefault = Join-Path $root "app\src\main\res\values\strings.xml"

if (-not (Test-Path $adb)) {
    throw "adb not found"
}

function Get-AndroidString {
    param(
        [string]$Name,
        [string]$Fallback
    )

    foreach ($path in @($stringsKo, $stringsDefault)) {
        if (-not (Test-Path $path)) {
            continue
        }
        [xml]$xml = Get-Content -Raw -Encoding UTF8 $path
        $node = $xml.resources.string | Where-Object { $_.name -eq $Name } | Select-Object -First 1
        if ($node) {
            return [string]$node.InnerText
        }
    }

    return $Fallback
}

function Get-UiXml {
    return (& $adb exec-out uiautomator dump /dev/tty) -join "`n"
}

function Find-TextBounds {
    param(
        [string]$Xml,
        [string]$Text
    )

    $escaped = [regex]::Escape($Text)
    $match = [regex]::Match($Xml, "text=`"$escaped`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    if (-not $match.Success) {
        return $null
    }

    $left = [int]$match.Groups[1].Value
    $top = [int]$match.Groups[2].Value
    $right = [int]$match.Groups[3].Value
    $bottom = [int]$match.Groups[4].Value

    return [pscustomobject]@{
        X = [int](($left + $right) / 2)
        Y = [int](($top + $bottom) / 2)
        Bounds = "[$left,$top][$right,$bottom]"
    }
}

function Find-TextWithScroll {
    param(
        [string]$Text,
        [int]$MaxScrolls = 4
    )

    for ($i = 0; $i -le $MaxScrolls; $i++) {
        $xml = Get-UiXml
        $bounds = Find-TextBounds -Xml $xml -Text $Text
        if ($bounds) {
            return $bounds
        }
        & $adb shell input swipe 700 1800 700 450 400 | Out-Null
        Start-Sleep -Milliseconds 500
    }

    return $null
}

$testAlarmText = Get-AndroidString "test_alarm" "Test alarm"
$stopText = Get-AndroidString "stop" "Stop"
$historyText = Get-AndroidString "history" "History"

Write-Host "Launch app:"
& $adb shell am start -n "$PackageName/$LauncherActivity"
Start-Sleep -Seconds 1

Write-Host "Find test alarm button:"
$testAlarm = Find-TextWithScroll -Text $testAlarmText
if (-not $testAlarm) {
    throw "Test alarm button not found: $testAlarmText"
}
Write-Host "Found $testAlarmText at $($testAlarm.Bounds)"

if (-not $RunAlarm) {
    Write-Host "Dry run only. Add -RunAlarm to tap test alarm and stop it."
    exit 0
}

Write-Host "Tap test alarm:"
& $adb shell input tap $testAlarm.X $testAlarm.Y
Start-Sleep -Seconds 2

Write-Host "Find stop button:"
$xml = Get-UiXml
$stop = Find-TextBounds -Xml $xml -Text $stopText
if (-not $stop) {
    throw "Stop button not found: $stopText"
}
Write-Host "Found $stopText at $($stop.Bounds)"

Write-Host "Tap stop:"
& $adb shell input tap $stop.X $stop.Y
Start-Sleep -Seconds 1

Write-Host "Verify history screen:"
$history = Find-TextWithScroll -Text $historyText
if (-not $history) {
    throw "History label not found: $historyText"
}
Write-Host "Found $historyText at $($history.Bounds)"
