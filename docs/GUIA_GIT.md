# Uso básico de Git

El paquete incluye un repositorio Git en la carpeta raíz.

## Ver el estado

```bash
git status
```

## Ver el historial

```bash
git log --oneline
```

## Crear una rama

```bash
git switch -c mejora-interfaz
```

## Guardar cambios

```bash
git add .
git commit -m "Mejorar pantalla de reportes"
```

## Regresar a la rama principal

```bash
git switch main
```

## Subir a GitHub opcionalmente

1. Cree un repositorio vacío en GitHub.
2. Ejecute:

```bash
git remote add origin URL_DEL_REPOSITORIO
git push -u origin main
```

Nunca agregue `android-app/local.properties`, la clave de Google Maps, bases de datos generadas ni fotografías. Los archivos `.gitignore` ya excluyen esos elementos.
