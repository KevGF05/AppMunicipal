# Sistema App Municipal

Entrega académica completa para gestionar reportes viales como tickets.

## Qué incluye

- Aplicación Android en Kotlin y XML.
- Registro ciudadano anónimo, sin cuenta ni datos personales obligatorios.
- Google Maps con pin para seleccionar la ubicación.
- Fotografía inicial obligatoria.
- API REST con Spring Boot.
- Base de datos persistente H2 para ejecutar sin instalar MySQL.
- Perfil opcional para MySQL mediante Docker.
- Inicio de sesión del personal municipal.
- Panel del administrador para aprobar, rechazar y asignar reportes.
- Panel del técnico para iniciar y cerrar tickets.
- Fotografía final y comentario obligatorios al cerrar.
- Seguimiento ciudadano únicamente mediante un código de seguimiento.
- Historial de cambios del ticket.
- Interfaz Material Design.
- Validaciones en Android y en el servidor.
- Repositorio Git incluido con historial básico de commits.
- Documentación técnica, manual de usuario, guía de instalación y colección de Postman.

## Flujo implementado

```text
Ciudadano anónimo
    ↓ crea reporte con ubicación y fotografía
CREADO
    ↓ administrador aprueba o rechaza
APROBADO / RECHAZADO
    ↓ administrador asigna un técnico
ASIGNADO
    ↓ técnico inicia el trabajo
EN_PROCESO
    ↓ técnico adjunta foto final y comentario
CERRADO
```

## Inicio rápido en Windows

### 1. Iniciar el backend

Instale Java 17 o una versión posterior compatible y ejecute:

```text
backend\run-backend.bat
```

La primera ejecución necesita Internet para descargar Maven y las dependencias. Cuando aparezca que Spring Boot inició en el puerto 8080, compruebe en el navegador:

```text
http://localhost:8080/api/public/health
```

La respuesta esperada contiene `"status":"OK"`.

El modo predeterminado usa una base de datos H2 persistente. Los datos quedan en:

```text
backend\data\
```

Las fotografías quedan en:

```text
backend\uploads\
```

### 2. Configurar la aplicación Android

1. Abra la carpeta `android-app` en Android Studio.
2. Copie `local.properties.example` como `local.properties`.
3. Mantenga el `sdk.dir` que Android Studio genere.
4. Agregue su clave real de Google Maps:

```properties
MAPS_API_KEY=SU_CLAVE_REAL
```

5. Para el emulador, conserve:

```properties
API_BASE_URL=http://10.0.2.2:8080/
```

6. Sincronice Gradle y ejecute en un emulador API 34 con Google Play.

## Usuarios de demostración

### Administrador

```text
Correo: admin@municipalidad.hn
Contraseña: Admin123*
```

### Técnicos

```text
Correo: tecnico1@municipalidad.hn
Contraseña: Tecnico123*
```

```text
Correo: tecnico2@municipalidad.hn
Contraseña: Tecnico123*
```

Estas cuentas se crean automáticamente la primera vez que inicia el backend.

## Prueba completa sugerida

1. Abra la app y seleccione **Reportar un problema**.
2. Seleccione el tipo, coloque el pin, escriba el detalle y tome la fotografía.
3. Envíe el reporte y conserve el código de seguimiento.
4. Regrese al inicio, entre a **Acceso de personal municipal** e inicie sesión como administrador.
5. Abra el reporte, apruébelo y asígnelo a `Técnico vial 1`.
6. Cierre sesión e ingrese como `tecnico1@municipalidad.hn`.
7. Abra el ticket, inicie el trabajo, tome una foto final, escriba el comentario y ciérrelo.
8. Regrese a **Consultar seguimiento** y verifique que el estado sea `Cerrado`.

## Carpetas principales

```text
AppMunicipalSistema/
├── android-app/       Aplicación Android
├── backend/           API REST y base de datos
├── docs/              Manuales y documentación
├── diagramas/         Diagramas Mermaid
├── postman/           Colección para probar la API
└── docker-compose.yml MySQL opcional
```

## Lo que no puede venir configurado dentro del ZIP

- La clave personal de Google Maps.
- Una cuenta de facturación de Google Cloud.
- Un servidor público disponible desde cualquier lugar.
- Un dominio y certificado HTTPS.
- Cuentas reales de empleados municipales.
- Publicación en Google Play.

El paquete funciona para una demostración local. Para que cualquier ciudadano descargue la aplicación y reporte desde Internet, el backend deberá desplegarse en un servicio público y `API_BASE_URL` deberá apuntar a una dirección HTTPS. Consulte `docs/PENDIENTE_PARA_PUBLICAR.md`.
