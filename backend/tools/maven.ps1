param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Version = "3.9.9"
$Tools = Join-Path $Root ".tools"
$MavenHome = Join-Path $Tools "apache-maven-$Version"
$MavenCmd = Join-Path $MavenHome "bin\mvn.cmd"
$Zip = Join-Path $Tools "apache-maven-$Version-bin.zip"
if (-not (Test-Path $MavenCmd)) {
    New-Item -ItemType Directory -Force -Path $Tools | Out-Null
    $Url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$Version/apache-maven-$Version-bin.zip"
    Write-Host "Descargando Apache Maven $Version..."
    Invoke-WebRequest -Uri $Url -OutFile $Zip
    Expand-Archive -Path $Zip -DestinationPath $Tools -Force
}
& $MavenCmd @MavenArgs
exit $LASTEXITCODE
