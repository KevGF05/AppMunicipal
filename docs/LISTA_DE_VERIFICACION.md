# Lista de verificación de requerimientos

| Requerimiento | Implementación incluida |
|---|---|
| Arquitectura MVC | Modelos y repositorios, vistas XML y Activities como controladores |
| Base de datos | H2 persistente en backend, MySQL opcional y SQLite local en Android |
| Consumo de API REST | Retrofit consume endpoints Spring Boot |
| Interfaz profesional | Material Design 3, cards, campos Material y RecyclerView |
| Validaciones | En Android y backend, incluidas foto y ubicación obligatorias |
| Documentación técnica | `DOCUMENTACION_TECNICA.md` |
| Manual de usuario | `MANUAL_USUARIO.md` |
| Uso básico de Git | Repositorio e historial, más `GUIA_GIT.md` |

## Flujo funcional que debe demostrarse

- [ ] Backend responde en `/api/public/health`.
- [ ] Ciudadano crea reporte anónimo.
- [ ] Se genera un código de seguimiento aleatorio y no se muestra ningún token privado.
- [ ] Administrador inicia sesión.
- [ ] Administrador aprueba el reporte.
- [ ] Administrador asigna técnico y prioridad.
- [ ] Técnico inicia sesión.
- [ ] Técnico ve únicamente sus tickets.
- [ ] Técnico inicia el trabajo.
- [ ] Técnico toma foto final y cierra.
- [ ] Ciudadano consulta y observa estado cerrado.
