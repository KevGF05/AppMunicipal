$ErrorActionPreference = "Stop"

$checks = @(
    @{ File = "backend\src\main\java\com\municipal\backend\dto\TrackingResponse.java"; Text = "String pauseReason"; Name = "TrackingResponse incluye pauseReason" },
    @{ File = "backend\src\main\java\com\municipal\backend\service\ReportService.java"; Text = "ReportStatus.PAUSADO ? report.getPauseReason() : null"; Name = "Backend devuelve motivo solo en PAUSADO" },
    @{ File = "android-app\app\src\main\java\com\example\appmunicipal\model\ApiModels.kt"; Text = "val pauseReason: String?"; Name = "Android recibe pauseReason" },
    @{ File = "android-app\app\src\main\java\com\example\appmunicipal\ui\publico\SeguimientoActivity.kt"; Text = "cardMotivoPausa"; Name = "Seguimiento muestra tarjeta de pausa" },
    @{ File = "android-app\app\src\main\res\layout\activity_seguimiento.xml"; Text = "Motivo de la pausa"; Name = "Layout incluye motivo de pausa" }
)

$failed = $false
foreach ($check in $checks) {
    $path = Join-Path (Get-Location) $check.File
    if ((Test-Path $path) -and (Select-String -Path $path -SimpleMatch $check.Text -Quiet)) {
        Write-Host "[OK] $($check.Name)" -ForegroundColor Green
    } else {
        Write-Host "[FALTA] $($check.Name)" -ForegroundColor Red
        $failed = $true
    }
}

if ($failed) {
    Write-Host "`nHay archivos que no fueron reemplazados correctamente." -ForegroundColor Yellow
    exit 1
}

Write-Host "`nTodo listo para compilar y hacer push." -ForegroundColor Cyan
