# Guía de presentación en clase

## Introducción sugerida

> App Municipal permite que cualquier ciudadano registre anónimamente un problema vial con ubicación y evidencia fotográfica. El reporte se envía mediante una API REST a una base de datos central, donde un administrador lo revisa y lo asigna a un técnico. El técnico documenta la reparación y cierra el ticket.

## Requerimientos demostrados

### Arquitectura MVC

- Modelo: DTO, repositorios y fuentes de datos.
- Vista: layouts XML con Material Design.
- Controlador: Activities que coordinan las acciones.

### Base de datos

- H2 persistente en el backend.
- SQLite local para el seguimiento ciudadano.
- Perfil MySQL opcional.

### API REST

- Retrofit en Android.
- Spring Boot en el servidor.
- JSON para login y administración.
- Multipart para fotografías.

### Validaciones

- Pin obligatorio.
- Foto inicial obligatoria.
- Detalle mínimo.
- Consentimiento.
- Motivo al rechazar.
- Técnico y prioridad al asignar.
- Foto final y comentario al cerrar.

### Git

Muestre:

```bash
git status
git log --oneline
```

## Demostración completa

1. Muestre que el backend responde en `/api/public/health`.
2. En Android cree un reporte anónimo.
3. Muestre el código de seguimiento generado.
4. Inicie sesión como administrador.
5. Apruebe y asigne el reporte.
6. Cierre sesión e ingrese como técnico.
7. Inicie el trabajo.
8. Tome la foto final y cierre el ticket.
9. Consulte el seguimiento ciudadano.
10. Muestre la base de datos en H2 Console si el docente lo solicita.

## Puntos técnicos para explicar

- El ciudadano no se autentica y no entrega datos personales.
- El código de seguimiento contiene una sección aleatoria y permite consultar únicamente el estado público del ticket.
- El administrador y técnico reciben permisos diferentes.
- El servidor valida las reglas aunque alguien modifique la aplicación.
- La fotografía se guarda como archivo y la base almacena su ruta.
- El emulador usa `10.0.2.2` para conectarse al servidor local.

## Limitación que debe declararse

La entrega funciona localmente. Una versión pública necesita desplegar el backend con HTTPS, una base de datos productiva y almacenamiento seguro de imágenes.
