-- APP MUNICIPAL - LIMPIEZA PARA DEMOSTRACION
-- IMPORTANTE: esta operación elimina tickets, historial, notificaciones y sesiones.
-- CONSERVA los usuarios (administrador/técnicos) y los tipos de reporte.
-- Las fotografías del bucket de Storage se eliminan aparte desde Supabase Storage.

BEGIN;

TRUNCATE TABLE
    historial_reportes,
    reportes,
    notificaciones,
    sesiones
RESTART IDENTITY;

COMMIT;

-- Verificación. Debe devolver 0 en las cuatro tablas de datos temporales.
SELECT
    (SELECT COUNT(*) FROM reportes) AS reportes,
    (SELECT COUNT(*) FROM historial_reportes) AS historial,
    (SELECT COUNT(*) FROM notificaciones) AS notificaciones,
    (SELECT COUNT(*) FROM sesiones) AS sesiones,
    (SELECT COUNT(*) FROM usuarios) AS usuarios_conservados,
    (SELECT COUNT(*) FROM tipos_reporte) AS tipos_reporte_conservados;
