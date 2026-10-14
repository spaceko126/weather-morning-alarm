param(
    [string]$KeyPath = "",
    [double]$Latitude = 37.5665,
    [double]$Longitude = 126.9780
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($KeyPath)) {
    $keyFile = Get-ChildItem -LiteralPath "$env:USERPROFILE\Desktop" |
        Where-Object { $_.Extension -eq ".txt" -or $_.Name -like "*key*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($keyFile) {
        $KeyPath = $keyFile.FullName
    }
}

if ([string]::IsNullOrWhiteSpace($KeyPath) -or -not (Test-Path -LiteralPath $KeyPath)) {
    throw "KMA key file not found: $KeyPath"
}

$serviceKey = (Get-Content -Raw -Encoding UTF8 -LiteralPath $KeyPath).Trim()
if ([string]::IsNullOrWhiteSpace($serviceKey)) {
    throw "KMA key file is empty: $KeyPath"
}

function ConvertTo-KmaGrid {
    param(
        [double]$Lat,
        [double]$Lon
    )

    $re = 6371.00877
    $grid = 5.0
    $slat1 = 30.0 * [Math]::PI / 180.0
    $slat2 = 60.0 * [Math]::PI / 180.0
    $olon = 126.0 * [Math]::PI / 180.0
    $olat = 38.0 * [Math]::PI / 180.0
    $xo = 43.0
    $yo = 136.0

    $sn = [Math]::Log([Math]::Cos($slat1) / [Math]::Cos($slat2)) /
        [Math]::Log([Math]::Tan([Math]::PI * 0.25 + $slat2 * 0.5) / [Math]::Tan([Math]::PI * 0.25 + $slat1 * 0.5))
    $sf = ([Math]::Pow([Math]::Tan([Math]::PI * 0.25 + $slat1 * 0.5), $sn) * [Math]::Cos($slat1)) / $sn
    $ro = $re / $grid * $sf / [Math]::Pow([Math]::Tan([Math]::PI * 0.25 + $olat * 0.5), $sn)

    $latRad = $Lat * [Math]::PI / 180.0
    $lonRad = $Lon * [Math]::PI / 180.0
    $ra = $re / $grid * $sf / [Math]::Pow([Math]::Tan([Math]::PI * 0.25 + $latRad * 0.5), $sn)
    $theta = $lonRad - $olon
    if ($theta -gt [Math]::PI) { $theta -= 2.0 * [Math]::PI }
    if ($theta -lt -[Math]::PI) { $theta += 2.0 * [Math]::PI }
    $theta *= $sn

    [pscustomobject]@{
        Nx = [int][Math]::Floor($ra * [Math]::Sin($theta) + $xo + 0.5)
        Ny = [int][Math]::Floor($ro - $ra * [Math]::Cos($theta) + $yo + 0.5)
    }
}

function Get-KmaBaseTime {
    $now = Get-Date
    $available = $now.AddMinutes(-20)
    $baseTimes = @("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")
    $time = $null
    foreach ($candidate in $baseTimes) {
        $candidateTime = [datetime]::ParseExact($candidate, "HHmm", $null)
        if ($candidateTime.TimeOfDay -le $available.TimeOfDay) {
            $time = $candidate
        }
    }
    if (-not $time) {
        $time = "2300"
        $date = $available.Date.AddDays(-1)
    } else {
        $date = $available.Date
    }

    [pscustomobject]@{
        Date = $date.ToString("yyyyMMdd")
        Time = $time
    }
}

function Invoke-Kma {
    param(
        [string]$Key,
        [string]$Mode
    )

    $grid = ConvertTo-KmaGrid -Lat $Latitude -Lon $Longitude
    $base = Get-KmaBaseTime
    $target = (Get-Date).Date.AddHours(7)
    if ($target -lt (Get-Date)) {
        $target = $target.AddDays(1)
    }
    $targetDate = $target.ToString("yyyyMMdd")
    $targetTime = $target.ToString("HH00")
    $encodedKey = if ($Mode -eq "encoded-as-is") {
        $Key
    } else {
        [System.Uri]::EscapeDataString($Key)
    }

    $url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
        "?serviceKey=$encodedKey&pageNo=1&numOfRows=1000&dataType=JSON" +
        "&base_date=$($base.Date)&base_time=$($base.Time)&nx=$($grid.Nx)&ny=$($grid.Ny)"

    try {
        $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 15
        $header = $response.response.header
        $items = @($response.response.body.items.item)
        $pop = $items | Where-Object { $_.category -eq "POP" -and $_.fcstDate -eq $targetDate -and $_.fcstTime -eq $targetTime } | Select-Object -First 1
        $pty = $items | Where-Object { $_.category -eq "PTY" -and $_.fcstDate -eq $targetDate -and $_.fcstTime -eq $targetTime } | Select-Object -First 1
        [pscustomobject]@{
            Mode = $Mode
            ResultCode = [string]$header.resultCode
            ResultMsg = [string]$header.resultMsg
            Base = "$($base.Date) $($base.Time)"
            Grid = "$($grid.Nx),$($grid.Ny)"
            ItemCount = $items.Count
            Target = "$targetDate $targetTime"
            POP = if ($pop) { [string]$pop.fcstValue } else { "" }
            PTY = if ($pty) { [string]$pty.fcstValue } else { "" }
        }
    } catch {
        [pscustomobject]@{
            Mode = $Mode
            ResultCode = "REQUEST_FAILED"
            ResultMsg = $_.Exception.Message
            Base = ""
            Grid = ""
            ItemCount = 0
            Target = ""
            POP = ""
            PTY = ""
        }
    }
}

$results = @(
    Invoke-Kma -Key $serviceKey -Mode "decode-key-encode-in-request"
    Invoke-Kma -Key $serviceKey -Mode "encoded-as-is"
)

$results | Format-Table -AutoSize
$success = $results | Where-Object { $_.ResultCode -eq "00" } | Select-Object -First 1
if ($success) {
    Write-Host "KMA smoke check passed. Use mode: $($success.Mode)"
} else {
    Write-Host "KMA smoke check failed. Check whether the API application is approved and the key type is correct."
    exit 1
}
