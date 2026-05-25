$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkSource = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
$apkTarget = Join-Path $projectRoot "MediGuard-debug.apk"

Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
    Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force
    Write-Host "APK ready: $apkTarget"
}
finally {
    Pop-Location
}
