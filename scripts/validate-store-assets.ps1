param(
    [string] $FeatureGraphicPath = "store-assets\feature-graphic.png",
    [string] $ScreenshotsDir = "store-assets\screenshots-clean"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$featureGraphic = Join-Path $root $FeatureGraphicPath
$screenshots = Join-Path $root $ScreenshotsDir

Add-Type -AssemblyName System.Drawing

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Get-ImageInfo {
    param([string] $Path)
    Assert-True (Test-Path $Path) "Image not found: $Path"
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $image = [System.Drawing.Image]::FromStream($stream)
        try {
            [pscustomobject]@{
                Path = $Path
                Width = $image.Width
                Height = $image.Height
                Bytes = (Get-Item $Path).Length
            }
        } finally {
            $image.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

$feature = Get-ImageInfo $featureGraphic
Assert-True ($feature.Width -eq 1024) "Feature graphic width must be 1024."
Assert-True ($feature.Height -eq 500) "Feature graphic height must be 500."

Assert-True (Test-Path $screenshots) "Screenshots directory not found: $screenshots"
$screenshotFiles = @(Get-ChildItem -Path $screenshots -Filter "*.png" -File | Sort-Object Name)
Assert-True ($screenshotFiles.Count -ge 2) "At least 2 screenshot PNG files are expected."

"FEATURE_GRAPHIC_OK=$($feature.Path)"
"FEATURE_GRAPHIC_SIZE=$($feature.Width)x$($feature.Height)"
"FEATURE_GRAPHIC_BYTES=$($feature.Bytes)"

foreach ($file in $screenshotFiles) {
    $info = Get-ImageInfo $file.FullName
    Assert-True ($info.Width -ge 320) "Screenshot width is too small: $($file.FullName)"
    Assert-True ($info.Height -ge 320) "Screenshot height is too small: $($file.FullName)"
    Assert-True ($info.Bytes -gt 1024) "Screenshot file is too small: $($file.FullName)"
    "SCREENSHOT_OK=$($file.Name) $($info.Width)x$($info.Height) $($info.Bytes) bytes"
}

"STORE_ASSETS_OK=1"
