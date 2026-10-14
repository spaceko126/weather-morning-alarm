$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jdk = Join-Path $root ".tools\jdk-17"
$gradle = Join-Path $root "gradlew.bat"

if (-not (Test-Path (Join-Path $jdk "bin\java.exe"))) {
    throw "Local JDK 17 not found: $jdk"
}

function Invoke-Gradle {
    param([string[]] $GradleArgs)
    & $gradle @GradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed: $($GradleArgs -join ' ')"
    }
}

$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH

$env:JAVA_HOME = $jdk
$env:PATH = (Join-Path $jdk "bin") + [System.IO.Path]::PathSeparator + $env:PATH
Invoke-Gradle @("--stop")
Invoke-Gradle @("testDebugUnitTest")

if ($null -eq $originalJavaHome) {
    Remove-Item Env:\JAVA_HOME -ErrorAction SilentlyContinue
} else {
    $env:JAVA_HOME = $originalJavaHome
}
$env:PATH = $originalPath

Invoke-Gradle @("--stop")
Invoke-Gradle @("assembleDebug", "lintDebug")
