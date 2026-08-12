$ErrorActionPreference = "Stop"

$manifest = "android-app\app\src\main\AndroidManifest.xml"
$mapa = "android-app\app\src\main\java\com\example\appmunicipal\ui\publico\MapaUbicacionActivity.kt"
$registro = "android-app\app\src\main\java\com\example\appmunicipal\ui\publico\RegistroActividadActivity.kt"
$tecnico = "android-app\app\src\main\java\com\example\appmunicipal\ui\tecnico\TecnicoDetalleActivity.kt"

function Check-Text($path, $text, $label) {
    if ((Test-Path $path) -and (Select-String -Path $path -SimpleMatch -Pattern $text -Quiet)) {
        Write-Host "[OK] $label" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] $label" -ForegroundColor Red
    }
}

Check-Text $manifest 'android.permission.ACCESS_FINE_LOCATION' 'Permiso de ubicacion en Manifest'
Check-Text $manifest 'android.permission.ACCESS_COARSE_LOCATION' 'Permiso aproximado en Manifest'
Check-Text $manifest 'android.permission.CAMERA' 'Permiso de camara en Manifest'
Check-Text $mapa 'ActivityResultContracts.RequestMultiplePermissions()' 'Solicitud de ubicacion en tiempo de ejecucion'
Check-Text $mapa 'mapa.isMyLocationEnabled = true' 'Ubicacion del dispositivo habilitada tras autorizar'
Check-Text $registro 'ActivityResultContracts.RequestPermission()' 'Solicitud de camara para crear reporte'
Check-Text $tecnico 'ActivityResultContracts.RequestPermission()' 'Solicitud de camara para cierre tecnico'
