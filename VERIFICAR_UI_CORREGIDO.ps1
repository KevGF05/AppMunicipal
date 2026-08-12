$project = Get-Location

function Test-Contains($relative, $pattern, $label) {
    $path = Join-Path $project $relative
    if ((Test-Path $path) -and (Select-String -Path $path -Pattern $pattern -SimpleMatch -Quiet)) {
        Write-Host "[OK] $label" -ForegroundColor Green
    } else {
        Write-Host "[FALTA] $label" -ForegroundColor Red
    }
}

Test-Contains "android-app\app\src\main\res\layout\dialog_asignar.xml" "layoutObservacion" "Correccion de observacion en Asignar tecnico"
Test-Contains "android-app\app\src\main\res\layout\activity_admin_detalle.xml" 'android:layout_weight="1"' "Encabezado fijo detalle admin"
Test-Contains "android-app\app\src\main\res\layout\activity_tecnico_detalle.xml" 'android:layout_weight="1"' "Encabezado fijo detalle tecnico"
Test-Contains "android-app\app\src\main\java\com\example\appmunicipal\ui\admin\AdminReportTypesActivity.kt" "showingInactive" "Vista de reportes desactivados"
Test-Contains "android-app\app\src\main\res\layout\activity_admin_report_types.xml" "buttonVerDesactivados" "Boton reportes desactivados"
Test-Contains "android-app\app\src\main\res\layout\dialog_report_type.xml" "layoutNombreTipo" "Formulario Agregar tipo de reporte"

Write-Host ""
Write-Host "Verificacion terminada." -ForegroundColor Cyan
