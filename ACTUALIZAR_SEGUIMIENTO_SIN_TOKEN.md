# Actualización: seguimiento solo con código

Esta versión elimina el token privado de la interfaz ciudadana.

## Qué cambia

- La confirmación muestra solo el código y el estado inicial.
- La consulta pública solicita únicamente el código.
- La API ya no recibe el parámetro `token`.
- La respuesta pública muestra solo estado, última actualización y mensaje general.
- Los reportes nuevos usan códigos con una sección aleatoria.
- La fotografía final del técnico queda autorizada en `file_paths.xml`.

## Cómo actualizar sin perder reportes

1. Detenga el backend con `Ctrl + C`.
2. Haga una copia de `backend/data` y `backend/uploads`.
3. Reemplace los archivos del proyecto con esta versión.
4. Vuelva a colocar su `android-app/local.properties`.
5. Copie nuevamente `backend/data` y `backend/uploads` si reemplazó la carpeta completa.
6. Inicie el backend con `INICIAR_BACKEND.bat`.
7. En Android Studio ejecute Sync, Clean Project y vuelva a instalar la app.

Los reportes existentes continúan funcionando. Para ellos ya no será necesario ingresar el token anterior.
