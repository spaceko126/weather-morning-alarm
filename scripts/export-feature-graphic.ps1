param(
    [string] $OutputPath = "store-assets\feature-graphic.png"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root $OutputPath
$outDir = Split-Path -Parent $out
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Add-Type -AssemblyName System.Drawing

$bitmap = New-Object System.Drawing.Bitmap 1024, 500
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

try {
    $background = [System.Drawing.ColorTranslator]::FromHtml("#F7F7FB")
    $purple = [System.Drawing.ColorTranslator]::FromHtml("#5E4BB6")
    $black = [System.Drawing.ColorTranslator]::FromHtml("#161616")
    $gray = [System.Drawing.ColorTranslator]::FromHtml("#333333")
    $cloud = [System.Drawing.ColorTranslator]::FromHtml("#BDEBFF")

    $graphics.Clear($background)
    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush $purple), 0, 0, 360, 500)
    $graphics.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), 84, 114, 192, 192)
    $graphics.FillEllipse((New-Object System.Drawing.SolidBrush $purple), 108, 138, 144, 144)

    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::White), 18
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawLine($pen, 180, 152, 180, 214)
    $graphics.DrawLine($pen, 180, 214, 228, 243)

    $graphics.FillEllipse((New-Object System.Drawing.SolidBrush $cloud), 72, 130, 220, 120)
    $graphics.FillEllipse((New-Object System.Drawing.SolidBrush $cloud), 118, 88, 160, 150)
    $graphics.FillRectangle((New-Object System.Drawing.SolidBrush $cloud), 98, 170, 210, 86)

    $drop = New-Object System.Drawing.Drawing2D.GraphicsPath
    $drop.AddBezier(108, 328, 70, 380, 70, 444, 112, 444)
    $drop.AddBezier(154, 444, 154, 382, 108, 328, 108, 328)
    $graphics.FillPath((New-Object System.Drawing.SolidBrush $purple), $drop)

    $titleFont = New-Object System.Drawing.Font "Arial", 42, ([System.Drawing.FontStyle]::Bold)
    $bodyFont = New-Object System.Drawing.Font "Arial", 24, ([System.Drawing.FontStyle]::Regular)
    $tagFont = New-Object System.Drawing.Font "Arial", 25, ([System.Drawing.FontStyle]::Bold)
    $graphics.DrawString("Weather Alarm", $titleFont, (New-Object System.Drawing.SolidBrush $black), 410, 154)
    $graphics.DrawString("Wake earlier for rain or snow.", $bodyFont, (New-Object System.Drawing.SolidBrush $gray), 412, 224)
    $graphics.DrawString("Rain | Snow | Morning commute", $tagFont, (New-Object System.Drawing.SolidBrush $purple), 412, 294)

    $bitmap.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}

"FEATURE_GRAPHIC=$out"
