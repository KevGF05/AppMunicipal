# API REST

Base local:

```text
http://localhost:8080/
```

## Salud

```http
GET /api/public/health
```

## Crear reporte anónimo

```http
POST /api/public/reportes
Content-Type: multipart/form-data
```

Partes:

```text
tipo
 descripcion
 direccion
 latitud
 longitud
 foto
```

Respuesta:

```json
{
  "id": 1,
  "code": "EP-2026-000001",
  "trackingToken": "uuid-privado",
  "status": "CREADO",
  "message": "Reporte registrado correctamente."
}
```

## Consultar seguimiento

```http
GET /api/public/reportes/EP-2026-000001?token=uuid-privado
```

## Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@municipalidad.hn",
  "password": "Admin123*"
}
```

Respuesta:

```json
{
  "token": "token-de-sesion",
  "name": "Administrador municipal",
  "role": "ADMINISTRADOR"
}
```

## Endpoints de administrador

Todos requieren:

```http
Authorization: Bearer token-de-sesion
```

### Listar

```http
GET /api/admin/reportes
GET /api/admin/reportes?estado=CREADO
```

### Obtener detalle

```http
GET /api/admin/reportes/{id}
```

### Aprobar

```http
PATCH /api/admin/reportes/{id}/aprobar
```

### Rechazar

```http
PATCH /api/admin/reportes/{id}/rechazar
Content-Type: application/json
```

```json
{
  "reason": "La evidencia no corresponde al reporte."
}
```

### Listar técnicos

```http
GET /api/admin/tecnicos
```

### Asignar

```http
PATCH /api/admin/reportes/{id}/asignar
Content-Type: application/json
```

```json
{
  "technicianId": 2,
  "priority": "ALTA",
  "observation": "Atender hoy."
}
```

Prioridades:

```text
BAJA
MEDIA
ALTA
URGENTE
```

## Endpoints de técnico

Todos requieren el token de una cuenta con rol técnico.

### Listar tickets propios

```http
GET /api/tecnico/reportes
```

### Obtener detalle

```http
GET /api/tecnico/reportes/{id}
```

### Iniciar

```http
PATCH /api/tecnico/reportes/{id}/iniciar
```

### Cerrar

```http
POST /api/tecnico/reportes/{id}/cerrar
Content-Type: multipart/form-data
```

Partes:

```text
comentario
fotoFinal
```
