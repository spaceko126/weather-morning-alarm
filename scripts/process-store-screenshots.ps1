param(
    [string] $InputDir = "store-assets\screenshots",
    [string] $OutputDir = "store-assets\screenshots-clean",
    [int] $CropTop = 90,
    [int] $CropBottom = 145,
    [int] $CropLeft = 18,
    [int] $CropRight = 18,
    [string[]] $ScreenshotNames = @(
        "01-alarm-list.png",
        "02-alarm-edit.png",
        "03-time-picker.png",
        "04-location-search.png",
        "05-weather-rules.png"
    )
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$inputPath = Join-Path $root $InputDir
$outputPath = Join-Path $root $OutputDir

Add-Type -AssemblyName System.Drawing

if (-not (Test-Path $inputPath)) {
    throw "Input screenshot directory not found: $inputPath"
}
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
Get-ChildItem -Path $outputPath -Filter "*.png" -File | Remove-Item -Force

$files = @(
    foreach ($name in $ScreenshotNames) {
        $path = Join-Path $inputPath $name
        if (-not (Test-Path $path)) {
            throw "Expected screenshot not found: $path"
        }
        Get-Item $path
    }
)
if ($files.Count -eq 0) {
    throw "No PNG screenshots found: $inputPath"
}

foreach ($file in $files) {
    $source = [System.Drawing.Image]::FromFile($file.FullName)
    try {
        $width = $source.Width - $CropLeft - $CropRight
        $height = $source.Height - $CropTop - $CropBottom
        if ($width -lt 320 -or $height -lt 320) {
            throw "Crop result is too small for $($file.FullName): ${width}x${height}"
        }

        $bitmap = New-Object System.Drawing.Bitmap($width, $height)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.DrawImage(
                $source,
                [System.Drawing.Rectangle]::new(0, 0, $width, $height),
                [System.Drawing.Rectangle]::new($CropLeft, $CropTop, $width, $height),
                [System.Drawing.GraphicsUnit]::Pixel
            )
            $target = Join-Path $outputPath $file.Name
            $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
            "CLEAN_SCREENSHOT=$target ${width}x${height}"
        } finally {
            $graphics.Dispose()
            $bitmap.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}

"CLEAN_SCREENSHOTS_OK=1"
