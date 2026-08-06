# Lo pendiente para publicar la aplicación

La entrega funciona localmente para una demostración académica. Para que cualquier persona descargue la app y reporte desde cualquier lugar todavía se necesita lo siguiente.

## 1. Desplegar el backend

Debe alojarse Spring Boot en un servidor público, por ejemplo una máquina virtual, servicio de contenedores o plataforma compatible con Java.

El servidor necesita:

- URL pública.
- HTTPS.
- Base de datos administrada.
- Carpeta o almacenamiento de objetos para fotografías.
- Copias de seguridad.
- Monitoreo.

## 2. Cambiar la URL de Android

En `local.properties` o en la configuración de compilación:

```properties
API_BASE_URL=https://api.su-municipalidad.hn/
```

Después debe compilarse una nueva APK o versión de Google Play.

## 3. Reforzar seguridad

- Deshabilitar `android:usesCleartextTraffic`.
- Desactivar la consola H2.
- Sustituir usuarios y contraseñas de demostración.
- Usar secretos mediante variables de entorno.
- Cifrar tokens locales.
- Implementar recuperación y rotación de contraseñas.
- Agregar rate limiting y CAPTCHA al reporte anónimo.
- Escanear archivos cargados.
- Aplicar política de retención de fotografías.
- Registrar auditoría de accesos.

## 4. Base de datos productiva

Use MySQL o PostgreSQL en el servidor. El perfil MySQL y `docker-compose.yml` sirven como punto de partida, pero las contraseñas deben cambiarse.

## 5. Fotografías

Para producción conviene usar almacenamiento de objetos con URLs protegidas, no servir directamente la carpeta local del servidor.

## 6. Google Maps

La clave debe restringirse por paquete y certificado de firma de la versión publicada. El SHA-1 de debug no es el mismo que el de producción.

## 7. Distribución

- Crear clave de firma de Android.
- Generar Android App Bundle.
- Preparar política de privacidad.
- Crear ficha de Google Play.
- Realizar pruebas cerradas.

Ninguno de estos elementos puede venir listo en un ZIP porque depende de cuentas, dominios, certificados, presupuesto e infraestructura de la municipalidad.
