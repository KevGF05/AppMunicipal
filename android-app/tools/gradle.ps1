param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Version = "8.9"
$Tools = Join-Path $Root ".tools"
$GradleHome = Join-Path $Tools "gradle-$Version"
$GradleCmd = Join-Path $GradleHome "bin\gradle.bat"
$Zip = Join-Path $Tools "gradle-$Version-bin.zip"
if (-not (Test-Path $GradleCmd)) {
    New-Item -ItemType Directory -Force -Path $Tools | Out-Null
    $Url = "https://services.gradle.org/distributions/gradle-$Version-bin.zip"
    Write-Host "Descargando Gradle $Version..."
    Invoke-WebRequest -Uri $Url -OutFile $Zip
    Expand-Archive -Path $Zip -DestinationPath $Tools -Force
}
& $GradleCmd @GradleArgs
exit $LASTEXITCODE
