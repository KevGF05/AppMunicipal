# Validación de la entrega

Antes de generar el ZIP se realizaron estas comprobaciones:

- Todos los archivos XML fueron analizados correctamente.
- La colección de Postman contiene JSON válido.
- Las referencias de View Binding utilizadas por las Activities coinciden con IDs existentes en sus layouts.
- El manifiesto declara todas las Activities y el `FileProvider`.
- El proyecto no incluye `local.properties` ni una clave real de Google Maps.
- Los archivos de datos, fotografías y compilación están excluidos mediante `.gitignore`.
- El repositorio Git contiene tres commits separados para backend, Android y documentación.
- Se realizó una revisión sintáctica estática de los archivos Kotlin y Java.

## Comprobación pendiente en la computadora del estudiante

No fue posible ejecutar una compilación completa de Gradle y Maven en el entorno donde se preparó el ZIP porque no permitió descargar dependencias externas y no tenía un Android SDK disponible. La primera ejecución en la computadora del estudiante descargará las dependencias necesarias.

Por ello, después de descomprimir se debe ejecutar:

```text
backend\test-backend.bat
```

Y en Android Studio:

```text
File > Sync Project with Gradle Files
Build > Clean Project
Build > Assemble Project
```

Cualquier incompatibilidad específica de la versión local de Android Studio puede corregirse paso a paso sin cambiar la arquitectura del sistema.
