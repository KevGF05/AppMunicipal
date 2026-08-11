# Aplicación Android App Municipal

Aplicación en Kotlin con XML, Material Design, arquitectura MVC académica y consumo de una API REST.

## Módulos

- Público: crear un reporte anónimo y consultar su seguimiento.
- Administrador: aprobar, rechazar y asignar tickets.
- Técnico: consultar trabajos asignados, iniciarlos y cerrarlos con fotografía final.

## Configuración

Copie `local.properties.example` como `local.properties` y coloque:

```properties
MAPS_API_KEY=SU_CLAVE_REAL
API_BASE_URL=http://10.0.2.2:8080/
```

`10.0.2.2` permite que el emulador acceda al backend que se ejecuta en la computadora. Para un teléfono físico use la IP local de la computadora, por ejemplo `http://192.168.1.50:8080/`.

## Requisitos

- Android Studio.
- JDK 17.
- Emulador API 34 con Google Play o teléfono Android API 24+.
- Backend ejecutándose antes de enviar o consultar reportes.

## Arquitectura MVC académica

- Modelo: `model/`, `data/remote/`, `data/local/` y `data/repository/`.
- Vista: archivos XML en `res/layout/`.
- Controlador: Activities en `ui/`.

## Base de datos local

`SeguimientoDbHelper` utiliza SQLite para conservar el último código y estado consultado. La base de datos central se encuentra en el backend.

## Seguridad de demostración

- La clave de Maps no se incluye en el repositorio.
- Las fotos se generan en almacenamiento privado antes de enviarse.
- Se eliminan metadatos GPS EXIF de la fotografía ciudadana.
- La app no solicita nombre, correo ni identidad al ciudadano.
- Los módulos privados requieren un token de sesión emitido por el backend.

Para producción, cambie el backend a HTTPS y desactive el tráfico HTTP sin cifrar.


## Seguimiento ciudadano

La pantalla pública solicita solamente el código de seguimiento. No se muestra ni se solicita un token privado. Por privacidad, la respuesta pública contiene únicamente el código, el estado, la fecha de actualización y un mensaje general.
