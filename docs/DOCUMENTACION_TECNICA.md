# Documentación técnica

## 1. Objetivo

Desarrollar un sistema móvil que permita registrar anónimamente incidencias viales y gestionarlas como tickets municipales desde su creación hasta su cierre.

## 2. Tecnologías

### Android

- Kotlin.
- XML.
- Material Design 3.
- View Binding.
- Retrofit y Gson.
- OkHttp.
- Google Maps SDK for Android.
- SQLite mediante `SQLiteOpenHelper`.

### Backend

- Java 17.
- Spring Boot.
- Spring Web.
- Spring Security.
- Spring Data JPA.
- Bean Validation.
- H2 persistente.
- MySQL opcional.

## 3. Arquitectura general

```text
Android
  ├── Vistas XML
  ├── Activities como controladores
  ├── Repositorios
  ├── Retrofit
  └── SQLite local
          │ HTTP/JSON/multipart
          ▼
Spring Boot REST API
  ├── Controllers
  ├── Services
  ├── Repositories JPA
  ├── Autorización por roles
  ├── H2/MySQL
  └── Almacenamiento de imágenes
```

## 4. MVC académico en Android

### Modelo

- `model/ApiModels.kt`
- `data/remote/`
- `data/local/`
- `data/repository/`

### Vista

- Archivos XML en `app/src/main/res/layout/`.

### Controlador

- Activities en `ui/publico`, `ui/admin`, `ui/tecnico` y `ui/auth`.

Las Activities reciben eventos, validan la entrada visual y delegan el acceso a datos a `AppRepository`.

## 5. Capas del backend

- `controller`: define endpoints REST.
- `service`: aplica reglas de negocio y transiciones de estado.
- `repository`: acceso a datos mediante JPA.
- `model`: entidades y enumeraciones.
- `dto`: contratos JSON.
- `security`: validación del token Bearer y roles.
- `config`: seguridad, datos iniciales y archivos públicos.

## 6. Modelo de datos central

### usuarios

- id
- name
- email
- password_hash
- role
- active

### reportes

- id
- code
- tracking_token
- type
- description
- address
- latitude
- longitude
- initial_photo_path
- final_photo_path
- status
- priority
- tecnico_id
- closure_comment
- rejection_reason
- created_at
- updated_at

### historial_reportes

- id
- reporte_id
- previous_status
- new_status
- actor
- comment
- created_at

### sesiones

- token
- usuario_id
- expires_at

## 7. Estados

```text
CREADO
APROBADO
RECHAZADO
ASIGNADO
EN_PROCESO
CERRADO
```

Reglas principales:

- Solo un reporte creado puede aprobarse.
- Un reporte creado o aprobado puede rechazarse.
- Solo un reporte aprobado puede asignarse inicialmente.
- El técnico solo puede modificar tickets asignados a su usuario.
- Para cerrar se exige fotografía final y comentario.

## 8. Autenticación y autorización

El login devuelve un token aleatorio de sesión con vencimiento de ocho horas. Android lo envía mediante:

```http
Authorization: Bearer TOKEN
```

El backend aplica:

- `ROLE_ADMINISTRADOR` a `/api/admin/**`.
- `ROLE_TECNICO` a `/api/tecnico/**`.
- Acceso público a `/api/public/**` y `/api/auth/**`.

Las contraseñas se almacenan con BCrypt.

## 9. Reporte anónimo

El ciudadano no crea una cuenta. El servidor genera:

- Un código visible, por ejemplo `EP-2026-000001`.
- Un token privado de seguimiento.

La combinación de ambos permite consultar el ticket sin revelar todos los reportes.

## 10. Fotografías

Android crea la imagen mediante `FileProvider`. El reporte inicial elimina etiquetas EXIF de ubicación antes de enviarse. Retrofit envía la imagen como `multipart/form-data`.

El backend valida que exista un archivo de imagen y lo guarda en `backend/uploads`. La base de datos conserva únicamente la ruta.

## 11. Validaciones

### Android

- Tipo obligatorio.
- Ubicación y coordenadas obligatorias.
- Descripción de 10 a 500 caracteres.
- Foto inicial obligatoria.
- Consentimiento obligatorio.
- Foto final y comentario obligatorios al cerrar.

### Backend

- Repite las reglas críticas.
- Valida estados y rol.
- Impide que un técnico modifique tickets ajenos.
- Limita tamaño de archivos multipart.

## 12. Base de datos local Android

`SeguimientoDbHelper` crea la tabla `seguimientos` y conserva el último código y token. Esta base local no sustituye la base central, pero demuestra persistencia SQLite.

## 13. Limitaciones académicas

- El token de sesión no es JWT.
- El token se almacena en SharedPreferences sin cifrado.
- La consola H2 está habilitada para demostración.
- Se permite HTTP local para conectar con el backend.
- Las imágenes se sirven desde el mismo backend.
- No existen notificaciones push.

Estas decisiones facilitan la demostración en clase. Para publicación deben endurecerse según `PENDIENTE_PARA_PUBLICAR.md`.
