# Paquete de reemplazo — AppMunicipalSistema v3.0

74 archivos que sustituyen a los actuales. La estructura de carpetas del ZIP es la
misma que la de tu proyecto, así que se extrae encima y cada archivo cae en su lugar.

**No hay ningún cambio de backend.** Todo lo que sigue vive en `android-app/`.

---

## 1. Cómo instalarlo

1. **Haz un commit o una copia del proyecto antes de nada.** Si algo no te gusta,
   quieres poder volver con un `git checkout` en lugar de reescribir a mano.
2. Extrae el ZIP sobre la raíz del proyecto (la carpeta que contiene `android-app/`
   y `backend/`). Acepta sobrescribir.
3. En Android Studio: **File → Sync Project with Gradle Files** (hay una dependencia
   nueva).
4. **Build → Clean Project**, luego **Build → Rebuild Project**.

El paso 3 es obligatorio: sin él, `SwipeRefreshLayout` no existe todavía y todos los
layouts de listas fallarán al inflar.

### Archivos que se agregan (no reemplazan nada)

| Archivo | Para qué |
|---|---|
| `res/layout/activity_mis_reportes.xml` | Pantalla "Mis reportes" |
| `res/layout/item_mi_reporte.xml` | Fila de esa lista |
| `res/layout/dialog_motivo.xml` | Campo de motivo de rechazo y de pausa |
| `res/values/colors_estado.xml` (+ night) | Colores de estado y prioridad |
| `res/values/styles.xml` | Estilos reutilizables |
| `res/menu/menu_dashboard.xml` | Menú "Cerrar sesión" de la barra |
| `ui/publico/MisReportesActivity.kt` y su adapter | Pantalla nueva |
| `util/ToolbarSetup.kt`, `util/FechaFormat.kt` | Utilidades nuevas |
| 6 iconos en `res/drawable/` | Flecha, copiar, compartir, carpeta, papelera, estado vacío |

### Lo único que cambia fuera de `src/main`

`app/build.gradle.kts`:

```kotlin
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
```

Más `versionCode 16 → 17` y `versionName "2.6" → "3.0"`. Nada más: no se agregó
Coil ni ninguna otra librería de terceros.

---

## 2. Cambios de interfaz

### 2.1 Barra superior en lugar de botón "Volver"

Antes cada pantalla tenía un `MaterialButton` con el texto "Volver" o "← Dashboard"
ocupando una fila completa del contenido, con 44dp de alto. El mínimo táctil
recomendado por Android es 48dp, así que además de gastar espacio era un objetivo
pequeño.

Ahora las 25 pantallas usan una `MaterialToolbar` con la flecha estándar, conectada
al `onBackPressedDispatcher`. Eso hace que la flecha y el gesto de retroceso del
teléfono se comporten igual — antes podían llevarte a sitios distintos.

**Detalle de implementación:** la barra *no* se registra con `setSupportActionBar()`.
Se usa como una vista normal, con `setNavigationOnClickListener` y `app:menu` en el
XML. Es menos código y evita tener que sincronizar el menú con el ciclo de vida del
ActionBar. Está en `util/ToolbarSetup.kt`.

En los dashboards, el nombre de la sesión pasó al subtítulo de la barra y
"Cerrar sesión" pasó al menú de tres puntos. En las listas del técnico, el contador
("3 tickets activos") también pasó al subtítulo.

### 2.2 Estado y prioridad como etiquetas de color

Antes el estado de un ticket era texto gris igual que el resto de la tarjeta, así que
para saber si algo estaba pausado o cerrado había que leerlo. Ahora cada estado tiene
su par de colores fondo/texto, definidos en `colors_estado.xml` con variante para modo
oscuro, y se aplican desde `util/UiFormat.kt`:

```kotlin
UiFormat.aplicarEstado(binding.textEstado, report.status)
UiFormat.aplicarPrioridad(binding.textPrioridad, report.priority)
```

Los estados activos (`EN_PROCESO`, `ASIGNADO`) usan tonos que llaman la atención;
`CERRADO` usa gris; `RECHAZADO` y `URGENTE` usan rojo. La misma función sirve para las
etiquetas activo/inactivo de técnicos y tipos de reporte.

### 2.3 Deslizar para actualizar

Los botones "Actualizar" desaparecieron de los dos dashboards y de las cuatro listas.
En su lugar hay `SwipeRefreshLayout`, que es el gesto que la gente ya espera en
cualquier lista de Android.

### 2.4 Filtros como chips

`AdminTicketsActivity` y `AdminTecnicosActivity` usaban un `AutoCompleteTextView`
desplegable. Había que abrirlo para saber qué filtro estaba activo. Ahora son chips
visibles: se ve el filtro actual sin tocar nada y se cambia con un toque.

Esto además arregla un problema real de mantenimiento. El filtro se resolvía comparando
el texto en español del desplegable:

```kotlin
// antes
when (binding.autoFiltro.text.toString()) {
    "Asignados / en proceso / pausa" -> ...
```

Si alguien corregía una tilde en ese texto, el filtro dejaba de funcionar sin ningún
error visible. Ahora se compara el id del chip, que no depende del idioma.

### 2.5 Estado vacío con ilustración

Un `TextView` que decía "No hay reportes" pasó a ser un bloque centrado con icono,
título y una línea explicando qué hacer. En tipos de reporte, el texto cambia según si
estás viendo activos o desactivados.

### 2.6 Botón flotante para la acción principal

En técnicos y tipos de reporte, "Crear técnico" y "Agregar tipo" pasaron a
`ExtendedFloatingActionButton`. Se encogen a solo el icono al desplazar la lista para no
tapar la última tarjeta.

### 2.7 Una sola paleta de colores

Había dos paletas paralelas con valores idénticos: `municipal_*` y `dashboard_*`. Para
no tener que editar cada layout, `dashboard_*` ahora son **alias** que apuntan a
`municipal_*`. Si más adelante quieres unificar del todo, basta con buscar y reemplazar;
mientras tanto, un solo cambio de color se refleja en toda la app.

También:

- `android:fontFamily="sans"` no es un valor válido en Android (se ignoraba
  silenciosamente y caía al tipo por defecto). Corregido a `sans-serif`.
- `forceDarkAllowed` estaba en `true` mientras el proyecto tenía su propio `values-night`.
  Eso hace que Android intente invertir colores *encima* de tu tema oscuro y el
  resultado es impredecible. Ahora está en `false`.

### 2.8 Textos fuera de los layouts

Había 133 textos escritos directamente en los XML y solo 7 en `strings.xml`. Ahora todos
están en `strings.xml` (325 strings). Los plurales que se resolvían con `if` en Kotlin:

```kotlin
// antes
binding.textCantidad.text = if (activos.size == 1) "1 ticket activo"
                            else "${activos.size} tickets activos"
// ahora
resources.getQuantityString(R.plurals.tickets_activos_contador, activos.size, activos.size)
```

Son 5 bloques `<plurals>`. Ojo: los strings van **sin tildes**, porque no revisé la
codificación de tu editor y un archivo mal guardado rompe la compilación de recursos de
forma difícil de diagnosticar. Si tu proyecto ya maneja UTF-8 bien —cámbialas y
compila para confirmar.

### 2.9 Diálogos con layout propio

Los campos de motivo (rechazo y pausa) se creaban en código:

```kotlin
val input = TextInputEditText(this).apply {
    setPadding(48, 16, 48, 16)   // pixeles crudos, no dp
}
```

Esos números son píxeles físicos, así que el diálogo se veía distinto en cada densidad
de pantalla. Ahora es `dialog_motivo.xml` normal, y el error aparece bajo el campo en
lugar de en un Snackbar después de cerrar el diálogo.

Además, aprobar un reporte ahora pide confirmación: era la única acción destructiva sin
diálogo previo.

### 2.10 Carga de imágenes

`ImageLoader` decodificaba el bitmap completo a memoria sin escalar. Una foto de 12 MP
son ~48 MB en RAM, suficiente para provocar `OutOfMemoryError` en un teléfono de gama
baja con varias fotos en pantalla. Ahora hace *downsampling* al tamaño de la vista,
guarda en un `LruCache` y usa un tag en el `ImageView` para evitar que una descarga
lenta pinte la foto equivocada cuando el RecyclerView recicla la fila.

### 2.11 Insets

El inset superior se aplica ahora a la barra, no a la raíz. Con la raíz, el color verde
de la barra empezaba *debajo* de la barra de estado y quedaba una franja del color de
fondo arriba.

### 2.12 RecyclerView

`ReportAdapter` usaba `notifyDataSetChanged()`, que redibuja todo y mata las animaciones.
Ahora es `ListAdapter` con `DiffUtil`.

---

## 3. Las dos cosas que no son solo UI

### 3.1 "Mis reportes" y el código de seguimiento

El código de seguimiento es el único vínculo entre el ciudadano y su reporte. Si lo
pierde, no hay forma de recuperarlo: no hay cuenta, no hay correo, no hay teléfono
registrado. Y no se podía copiar ni compartir; había que transcribirlo a mano de la
pantalla.

Cambios:

- Botones **Copiar** y **Compartir** en la pantalla de confirmación.
- Pantalla nueva **Mis reportes** (`InicioActivity` → botón nuevo), que lista todos los
  códigos guardados en el teléfono con su estado y fecha. Se puede tocar uno para
  consultarlo, copiarlo, o quitarlo de la lista.
- La base local ya guardaba todos los códigos, pero el código solo llamaba a `ultimo()`.
  Ahora `todos()` alimenta la pantalla nueva.

**Y un arreglo importante:** `SeguimientoDbHelper.onUpgrade` hacía esto:

```kotlin
db.execSQL("DROP TABLE IF EXISTS $TABLA")
onCreate(db)
```

Es decir, cada vez que subieras la versión del esquema, **todos los códigos guardados
del ciudadano se borraban**. Ahora la migración usa `ALTER TABLE` y conserva los datos.
La versión subió a 3 y se agregó una columna `tipo` para mostrar de qué era cada reporte.

> Si ya tienes la app instalada con datos de prueba, la migración se ejecuta sola al
> abrir. No hace falta desinstalar.

### 3.2 No perder el trabajo al rotar el teléfono

Si el ciudadano llenaba el formulario, tomaba la foto, elegía la ubicación en el mapa y
luego giraba el teléfono, Android destruía y recreaba la Activity y **se perdían la foto
y las coordenadas**. Había que empezar de nuevo.

Se agregó `onSaveInstanceState` en:

- `RegistroActividadActivity` — conserva la ruta de la foto y las coordenadas.
- `TecnicoDetalleActivity` — conserva la foto final de cierre.

En ambos casos se guarda la ruta del archivo y se comprueba que exista al restaurar,
porque el archivo temporal podría haberse limpiado.

---

## 4. Verificación que se hizo

No hay SDK de Android en el entorno donde se generó esto, así que **no está compilado**.
Lo que sí se verificó con un script:

- Los 26 XML están bien formados.
- Cada `@string`, `@color`, `@dimen`, `@style`, `@drawable`, `@plurals` referenciado
  existe (contando también los recursos originales que no se tocaron).
- Cada `R.string.x` / `R.id.x` usado desde Kotlin existe.
- Cada `binding.propiedad` corresponde a un `@+id` del layout de ese binding.
- Los argumentos de formato (`%1$s`, `%1$d`) coinciden con lo que pasa cada
  `getString()`.
- Ningún archivo Kotlin que *no* se reemplaza depende de las firmas que cambiaron
  (`UiFormat`, `ImageLoader`, `SeguimientoDbHelper`).
- Las activities del manifest existen, incluida la nueva.

Eso descarta los errores de referencias, que son la mayoría de lo que rompe al pegar
archivos. **No descarta errores de tipos de Kotlin.** El primer build es la prueba real.

---

## 5. Lo que quedó pendiente a propósito

De la revisión inicial, esto no entró porque toca el backend o excede el alcance que
acordamos:

| Pendiente | Por qué importa |
|---|---|
| `listarAdmin(null)` descarga todos los reportes y filtra en el cliente | El endpoint ya acepta `estado`. Con 500 tickets la lista tardará |
| Sin paginación | Mismo problema, más grave con el tiempo |
| Sin manejo de 401 | Si el token expira del lado del servidor, se ve un error genérico |
| Fotos sin comprimir (3–8 MB) | Sube lento con datos móviles |
| Sin envío offline con WorkManager | `work-runtime-ktx` ya está en las dependencias |
| Sin botón "Cómo llegar" | Un intent `geo:` de tres líneas, útil para el técnico en campo |
| Timeline de historial para el ciudadano | Hoy solo ve el estado actual |

Los tres primeros son los que más pesarían en una defensa del proyecto, porque son de
arquitectura y no de acabado.
