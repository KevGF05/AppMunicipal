# Backend App Municipal

API REST creada con Spring Boot, Spring Security y Spring Data JPA.

## Ejecución rápida

En Windows:

```text
run-backend.bat
```

En Linux/macOS:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

La primera ejecución descarga Maven y dependencias.

## Base de datos predeterminada

H2 persistente:

```text
data/appmunicipal.mv.db
```

Consola local:

```text
http://localhost:8080/h2-console
```

Datos de conexión:

```text
JDBC URL: jdbc:h2:file:./data/appmunicipal
Usuario: sa
Contraseña: dejar vacía
```

La consola es solo para demostración local.

## MySQL opcional

Desde la carpeta raíz:

```bash
docker compose up -d mysql
```

Después ejecute en Windows:

```text
backend\run-backend-mysql.bat
```

## Endpoints principales

```text
GET    /api/public/health
POST   /api/public/reportes
GET    /api/public/reportes/{codigo}?token=...
POST   /api/auth/login
GET    /api/admin/reportes
PATCH  /api/admin/reportes/{id}/aprobar
PATCH  /api/admin/reportes/{id}/rechazar
PATCH  /api/admin/reportes/{id}/asignar
GET    /api/admin/tecnicos
GET    /api/tecnico/reportes
PATCH  /api/tecnico/reportes/{id}/iniciar
POST   /api/tecnico/reportes/{id}/cerrar
```

Consulte `../docs/API_REST.md` para los cuerpos y respuestas.
