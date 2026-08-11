# Preparación del backend para Supabase + Render

Este backend mantiene H2 y almacenamiento local para desarrollo normal. El perfil `supabase` cambia únicamente la ejecución de producción a:

- PostgreSQL de Supabase mediante Session pooler (puerto 5432).
- Supabase Storage mediante S3 con bucket privado `evidencias`.
- URLs firmadas temporales para que administradores y técnicos vean evidencias sin publicar el bucket ni exponer claves S3.

## Variables de entorno

En Render se configurarán las variables indicadas en `VARIABLES_SUPABASE_RENDER.example`.

No guarde contraseñas, Access Key ID ni Secret Access Key dentro del repositorio.

## Prueba local opcional del perfil Supabase

En PowerShell, defina las variables y luego ejecute desde `backend`:

```powershell
$env:SPRING_PROFILES_ACTIVE="supabase"
$env:SUPABASE_DB_HOST="..."
$env:SUPABASE_DB_PORT="5432"
$env:SUPABASE_DB_NAME="postgres"
$env:SUPABASE_DB_USER="postgres...."
$env:SUPABASE_DB_PASSWORD="..."
$env:SUPABASE_S3_ENDPOINT="https://....storage.supabase.co/storage/v1/s3"
$env:SUPABASE_S3_REGION="..."
$env:SUPABASE_S3_ACCESS_KEY="..."
$env:SUPABASE_S3_SECRET_KEY="..."
$env:SUPABASE_STORAGE_BUCKET="evidencias"
$env:APP_ADMIN_EMAIL="admin@municipalidad.hn"
$env:APP_ADMIN_PASSWORD="..."
$env:APP_TECNICO1_PASSWORD="..."
.\mvnw.cmd spring-boot:run
```

Para volver a H2 local, cierre esa terminal o elimine `SPRING_PROFILES_ACTIVE` y use `INICIAR_BACKEND.bat` desde la raíz del proyecto.
