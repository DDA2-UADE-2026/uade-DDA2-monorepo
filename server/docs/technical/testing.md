# Tests del backend

Ejecutar los comandos desde `server`, con un **JDK 17 instalado**: el proyecto usa ese toolchain para compilar y ejecutar los tests. El wrapper descarga la versión de Gradle del proyecto; no hace falta instalar Gradle por separado.

## Suite local y tiempos

En PowerShell:

```powershell
$tiempo = Measure-Command {
    .\gradlew.bat test --rerun --console=plain | Out-Host
}
"Tiempo total: {0:N1} segundos" -f $tiempo.TotalSeconds
Start-Process .\build\reports\tests\test\index.html
```

`--rerun` vuelve a ejecutar los tests aunque sus resultados estén actualizados. La medición incluye el arranque de Gradle y cualquier compilación o descarga pendiente; una primera ejecución puede tardar más. El reporte HTML muestra resultados y duración por clase y método. El resultado de la ejecución aparece como `BUILD SUCCESSFUL` o `BUILD FAILED`.

En Linux o macOS:

```bash
time ./gradlew test --rerun --console=plain
```

Para ejecutar una clase concreta:

```powershell
.\gradlew.bat test --tests '*JwtServiceTest' --rerun --console=plain
```

La suite normal usa H2 en memoria, HTTP local y adaptadores controlados. No requiere levantar el backend, PostgreSQL ni Docker. Cinco pruebas de migración se omiten intencionalmente en H2: una de resoluciones, tres de beneficios y una de planes de intervención. Se ejecutan mediante las tareas PostgreSQL siguientes.

## PostgreSQL desechable

Preparar bases exclusivas para pruebas en `127.0.0.1:55439`. Estas tareas crean y eliminan tablas mediante `create-drop`; no usar una base de desarrollo con datos a conservar. No toman `DB_URL` del backend.

| Tarea | Base | Usuario | Variable de contraseña |
| --- | --- | --- | --- |
| `testApplicationsPostgres` | `application_test` | `application_test` | `APPLICATION_TEST_POSTGRES_PASSWORD` |
| `testApplicationResolutionsPostgres` | `application_resolution_test` | `application_test` | `APPLICATION_TEST_POSTGRES_PASSWORD` |
| `testBenefitGrantsPostgres` | `benefit_grant_test` | `application_test` | `APPLICATION_TEST_POSTGRES_PASSWORD` |
| `testInterventionPlansPostgres` | `intervention_plan_test` | `application_test` | `APPLICATION_TEST_POSTGRES_PASSWORD` |
| `testCentersPostgres` | `center_test` con la configuración de abajo | `application_test` con la configuración de abajo | `CENTER_TEST_POSTGRES_PASSWORD` |
| `testAppointmentsPostgres` | `appointment_test` | `appointment_test`, configurable | `APPOINTMENT_TEST_POSTGRES_PASSWORD` |

Cargar las contraseñas de esas bases en las variables indicadas. Si todas usan el mismo usuario `application_test`, configurar también:

```powershell
$env:APPOINTMENT_TEST_POSTGRES_USER = 'application_test'
$env:CENTER_TEST_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:55439/center_test'
$env:CENTER_TEST_POSTGRES_USER = 'application_test'
$env:CENTER_TEST_DDL_AUTO = 'create-drop'
```

La tarea de centros tiene un modo previo para un clon local en `127.0.0.1:5433/center_test_real` con `update`. Para usar una base vacía desechable junto con las demás tareas, aplicar explícitamente la configuración anterior.

Ejecutar cada tarea y forzar su repetición:

```powershell
$tareas = @(
    'testApplicationsPostgres', 'testApplicationResolutionsPostgres',
    'testBenefitGrantsPostgres', 'testInterventionPlansPostgres',
    'testCentersPostgres', 'testAppointmentsPostgres'
)
foreach ($tarea in $tareas) {
    .\gradlew.bat $tarea --rerun --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Falló $tarea" }
}
```

Cada tarea genera su reporte en `build/reports/tests/<tarea>/index.html`. Al terminar, retirar únicamente el entorno desechable creado para estas pruebas.

## Core remoto

`test` excluye la etiqueta `core-smoke`. `testCoreConnectivity` es una prueba opt-in que publica y recibe un evento real: requiere accesos, permisos, IP habilitada y una base técnica desechable. Sus variables y preparación están en [Prueba remota de publicación y recepción](core-integration.md#prueba-remota-de-publicación-y-recepción). No es necesaria para ejecutar la suite local ni para generar OpenAPI.

## Windows: rutas con acentos o ñ

Java 17 interpreta los archivos de argumentos `@argfile` con la codificación nativa de Windows. Si Gradle corre con un JDK más nuevo y escribe el classpath como UTF-8, una ruta como `4to año` puede quedar mal interpretada y producir `ClassNotFoundException` para todas las clases de test, aun después de compilar correctamente.

`gradlew.bat` detecta `native.encoding` y configura la codificación del proceso de Gradle para que esos archivos sean compatibles con Java 17. Las tareas `Test` mantienen UTF-8 para el contenido procesado por los tests. Conservar este ajuste al regenerar el wrapper.

Si el IDE ejecuta Gradle directamente y reaparece el problema, ejecutar el wrapper desde la terminal. También se puede configurar **Gradle JVM en JDK 17**, siempre que el IDE no fuerce UTF-8 para el proceso de Gradle. Las opciones explícitas de `JAVA_OPTS`, `GRADLE_OPTS` o `org.gradle.jvmargs` pueden reemplazar la codificación del wrapper. No hace falta renombrar ni mover la carpeta del proyecto.
