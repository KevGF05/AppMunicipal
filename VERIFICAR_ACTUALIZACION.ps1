$ErrorActionPreference = 'Stop'
$root = Get-Location
Write-Host "Verificando actualizacion en: $root" -ForegroundColor Cyan

$checks = @(
  @{ Name='Editar/desactivar tipos'; Path='android-app/app/src/main/java/com/example/appmunicipal/ui/admin/AdminReportTypesActivity.kt'; Pattern='mostrarEdicion' },
  @{ Name='Boton desactivar tipos'; Path='android-app/app/src/main/java/com/example/appmunicipal/ui/admin/ReportTypeAdapter.kt'; Pattern='Desactivar' },
  @{ Name='Dashboard tecnico'; Path='android-app/app/src/main/java/com/example/appmunicipal/ui/tecnico/TecnicoDashboardActivity.kt'; Pattern='cardTicketsAsignados' },
  @{ Name='Historial tecnico'; Path='android-app/app/src/main/java/com/example/appmunicipal/ui/tecnico/TecnicoHistorialActivity.kt'; Pattern='class TecnicoHistorialActivity' },
  @{ Name='Pausar ticket Android'; Path='android-app/app/src/main/java/com/example/appmunicipal/ui/tecnico/TecnicoDetalleActivity.kt'; Pattern='pausar' },
  @{ Name='Estado PAUSADO backend'; Path='backend/src/main/java/com/municipal/backend/model/ReportStatus.java'; Pattern='PAUSADO' },
  @{ Name='Endpoint pausar backend'; Path='backend/src/main/java/com/municipal/backend/controller/TechnicianController.java'; Pattern='pausar' },
  @{ Name='Editar tipo backend'; Path='backend/src/main/java/com/municipal/backend/controller/ReportTypeController.java'; Pattern='update' }
)

$allOk = $true
foreach ($c in $checks) {
  if (-not (Test-Path $c.Path)) {
    Write-Host "[FALTA] $($c.Name): archivo no existe -> $($c.Path)" -ForegroundColor Red
    $allOk = $false
    continue
  }
  $match = Select-String -Path $c.Path -Pattern $c.Pattern -SimpleMatch -Quiet
  if ($match) {
    Write-Host "[OK] $($c.Name)" -ForegroundColor Green
  } else {
    Write-Host "[FALTA] $($c.Name): no se encontro '$($c.Pattern)'" -ForegroundColor Red
    $allOk = $false
  }
}

Write-Host ""
if ($allOk) {
  Write-Host "ACTUALIZACION COPIADA CORRECTAMENTE." -ForegroundColor Green
  Write-Host "Ahora haga Sync, Clean Project y Rebuild Project en Android Studio." -ForegroundColor Yellow
} else {
  Write-Host "LA ACTUALIZACION NO QUEDO COPIADA COMPLETA." -ForegroundColor Red
  Write-Host "Vuelva a extraer el ZIP directamente sobre la raiz AppMunicipalSistemaEntrega." -ForegroundColor Yellow
}
