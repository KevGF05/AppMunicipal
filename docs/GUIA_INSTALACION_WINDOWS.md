# Guía de instalación en Windows

## Requisitos

1. Android Studio instalado.
2. JDK 17 o superior.
3. Emulador Android API 34 con Google Play.
4. Conexión a Internet durante la primera compilación.
5. Clave de Google Maps SDK for Android.

## Parte A. Ejecutar el backend

1. Abra la carpeta `backend`.
2. Haga doble clic en `run-backend.bat`.
3. La primera vez se descargará Maven y luego las dependencias de Spring Boot.
4. Espere hasta ver un mensaje indicando que el servidor inició en el puerto 8080.
5. Abra en el navegador:

```text
http://localhost:8080/api/public/health
```

Debe obtener una respuesta con estado `OK`.

No cierre la ventana del backend mientras utiliza la aplicación.

## Parte B. Configurar Google Maps

1. Abra Google Cloud Console.
2. Cree o seleccione un proyecto.
3. Active `Maps SDK for Android`.
4. Cree una clave de API.
5. Restrinja la clave para:

```text
Paquete: com.example.appmunicipal
SHA-1: el SHA-1 de la variante debug
```

6. En Android Studio puede obtener el SHA-1 ejecutando:

```powershell
.\gradlew signingReport
```

## Parte C. Configurar Android Studio

1. Abra únicamente la carpeta `android-app`.
2. Espere la sincronización inicial.
3. Copie `local.properties.example` y renómbrelo `local.properties`.
4. Verifique que contenga:

```properties
MAPS_API_KEY=SU_CLAVE_REAL
API_BASE_URL=http://10.0.2.2:8080/
```

5. Android Studio debe agregar o conservar `sdk.dir`.
6. Ejecute `File > Sync Project with Gradle Files`.
7. Seleccione el emulador API 34.
8. Presione Run.

## Usar un teléfono físico

El teléfono y la computadora deben estar en la misma red Wi-Fi. En la computadora ejecute:

```powershell
ipconfig
```

Busque la dirección IPv4, por ejemplo `192.168.1.50`, y cambie:

```properties
API_BASE_URL=http://192.168.1.50:8080/
```

Permita Java o el puerto 8080 en el Firewall de Windows cuando se solicite.

## Reiniciar los datos de demostración

1. Detenga el backend.
2. Elimine la carpeta `backend/data`.
3. Elimine la carpeta `backend/uploads` si también desea borrar las fotografías.
4. Inicie nuevamente el backend.

Las cuentas de demostración se crearán otra vez automáticamente.
