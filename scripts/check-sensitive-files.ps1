$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$checks = @(
    @{ Path = "keystore.properties"; Kind = "release signing config" },
    @{ Path = "release\weather-morning-upload.jks"; Kind = "release upload keystore" },
    @{ Path = "release"; Kind = "release private folder" }
)

$found = @()
foreach ($check in $checks) {
    $path = Join-Path $root $check.Path
    if (Test-Path $path) {
        $found += [pscustomobject]@{
            Path = $check.Path
            Kind = $check.Kind
        }
    }
}

$gitignore = Join-Path $root ".gitignore"
if (Test-Path $gitignore) {
    $gitignoreText = Get-Content -Raw $gitignore
    foreach ($requiredIgnore in @(
        "keystore.properties",
        "*.jks",
        "*.keystore",
        "release/",
        "device-logs/",
        "store-assets/screenshots/*.png"
    )) {
        if ($gitignoreText -notmatch [regex]::Escape($requiredIgnore)) {
            throw "Missing .gitignore rule: $requiredIgnore"
        }
    }
}

if ($found.Count -eq 0) {
    "SENSITIVE_LOCAL_FILES=none"
} else {
    foreach ($item in $found) {
        "SENSITIVE_LOCAL_FILE_PRESENT=$($item.Path) kind=$($item.Kind)"
    }
}
"SENSITIVE_IGNORE_RULES=OK"
