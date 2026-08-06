# Manual de usuario

## 1. Ciudadano anónimo

### Crear un reporte

1. Abra **App Municipal**.
2. Presione **Reportar un problema**.
3. Seleccione el tipo de incidente.
4. Presione **Seleccionar ubicación en el mapa**.
5. Toque el punto exacto donde se encuentra el problema.
6. Arrastre el pin si necesita corregir la posición.
7. Presione **Usar esta ubicación**.
8. Escriba una descripción de al menos 10 caracteres.
9. Presione **Tomar fotografía**.
10. Acepte el aviso de protección de datos.
11. Presione **Registrar actividad**.

La app no solicita nombre, correo, identidad ni teléfono.

### Guardar el código

Al enviar el reporte se muestran:

- Código de seguimiento.
- Token privado.
- Estado inicial.

El código y token se guardan también en la base local de la aplicación. No comparta el token con personas desconocidas.

### Consultar seguimiento

1. Desde el inicio presione **Consultar seguimiento**.
2. Revise el código y token precargados o escríbalos manualmente.
3. Presione **Consultar**.
4. La app mostrará el tipo, estado, prioridad, dirección y observaciones.

## 2. Administrador municipal

### Iniciar sesión

1. Desde el inicio presione **Acceso de personal municipal**.
2. Escriba el correo y contraseña del administrador.
3. Presione **Iniciar sesión**.

### Revisar reportes

1. Use el filtro para mostrar reportes creados, aprobados, asignados, en proceso, cerrados o rechazados.
2. Presione un reporte para ver su detalle, foto inicial, ubicación e historial.

### Aprobar

1. Abra un reporte con estado **Creado**.
2. Presione **Aprobar**.
3. El estado cambia a **Aprobado**.

### Rechazar

1. Abra un reporte creado o aprobado.
2. Presione **Rechazar**.
3. Escriba un motivo.
4. Confirme la acción.

### Asignar técnico

1. Abra un reporte aprobado.
2. Presione **Asignar**.
3. Seleccione el técnico.
4. Seleccione la prioridad.
5. Agregue una observación opcional.
6. Confirme.

## 3. Técnico municipal

### Consultar tickets

1. Inicie sesión con una cuenta de técnico.
2. La pantalla muestra únicamente los reportes asignados a esa cuenta.
3. Presione un ticket para abrirlo.

### Iniciar trabajo

1. Abra un ticket con estado **Asignado**.
2. Presione **Iniciar trabajo**.
3. El estado cambia a **En proceso**.

### Cerrar ticket

1. Abra el ticket asignado o en proceso.
2. Presione **Tomar foto final**.
3. Fotografie la reparación terminada.
4. Escriba un comentario de al menos 10 caracteres.
5. Presione **Cerrar ticket**.
6. El estado cambia a **Cerrado**.

## Mensajes frecuentes

### “No fue posible conectar con el servidor”

Verifique que el backend siga abierto y que `API_BASE_URL` sea correcto.

### El mapa aparece vacío

Verifique la clave de Google Maps, la facturación, `Maps SDK for Android`, el paquete y el SHA-1.

### No se habilita el botón de envío

La fotografía es obligatoria. También debe completar los campos y aceptar el aviso de protección de datos.
