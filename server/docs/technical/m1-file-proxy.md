# Proxy administrativo de archivos de M1

M8 expone `GET /api/admin/applications/{applicationId}/external-documents/{requestId}/file` para descargar una entrega externa. La ruta acepta la sesión de M8 por cookie o Bearer y exige `applications:management:documents:view`, tanto en el JWT como en el rol vigente en la base.

La implementación está **desactivada por defecto**. M1 todavía debe confirmar la consulta por UUID y una autorización de servicio para lectura administrativa. Su ruta actual `/documentos/{id}/archivo` usa un ID numérico: no se configura con el UUID recibido en los eventos. Tampoco se deriva un ID numérico, se utiliza el token ciudadano ni se comparte la clave de firma de M8.

## Recorrido

1. M8 comprueba usuario activo, rol y permiso actuales.
2. Comprueba que exista la solicitud y que `requestId` pertenezca a ella. `requestId` identifica la referencia documental de M1; no es el `eventId` ni el `documentId`.
3. Exige entrega `COMPLETED` con `documentId` UUID. Puede descargarse antes o después de la revisión humana.
4. Finaliza la transacción local y resuelve el UUID en la plantilla configurada del servidor. El navegador no decide el origen, la ruta remota, la credencial ni el UUID que se consulta. Cada descarga vuelve a leer la referencia vigente.
5. Consulta M1 con una credencial Bearer de servicio independiente. No sigue redirecciones ni reintenta la descarga a nivel de aplicación.
6. Consume y cierra la respuesta remota antes de devolver los bytes. El archivo se mantiene temporalmente en memoria, hasta el límite configurado, sin copia persistente ni transacción de base de datos durante HTTP.

M8 devuelve `Content-Disposition: attachment`, un nombre propio `documento-{documentId}.pdf/png/jpg/bin`, tamaño real y `Cache-Control: private, no-store`. Conserva únicamente los tipos PDF, PNG y JPEG reconocidos; otros binarios salen como `application/octet-stream`. Rechaza respuestas textuales/JSON, cuerpos vacíos, redirecciones, codificaciones no admitidas y tamaños superiores al límite. Spring normaliza gzip antes de entregar el cuerpo al cliente: el límite se aplica a los bytes descomprimidos. No realiza análisis antivirus ni valida el contenido semántico del documento.

No copia `Authorization`, `Set-Cookie`, `Location`, nombres ni otros encabezados remotos al navegador. La descarga no cambia la revisión, no escribe bytes en la base ni publica eventos en Core. Para visualizar el contenido, el frontend puede descargarlo como blob mediante esta ruta de M8; CORS expone `Content-Disposition` a los orígenes permitidos para obtener el nombre del adjunto.

## Configuración pendiente de acuerdo

| Variable | Predeterminado / condición |
| --- | --- |
| `M1_DOCUMENTS_ENABLED` | `false`. |
| `M1_DOCUMENTS_BASE_URL` | Origen HTTPS de M1; HTTP permitido solo en loopback para pruebas. |
| `M1_DOCUMENTS_FILE_PATH_TEMPLATE` | Vacía. Debe ser la ruta acordada, con un único segmento literal `{documentId}`, sin origen, query ni fragmento. |
| `M1_DOCUMENTS_SERVICE_BEARER_TOKEN` | Vacía. Token de servicio autorizado por M1, sin el prefijo `Bearer`. |
| `M1_DOCUMENTS_CONNECT_TIMEOUT` | `5s`. |
| `M1_DOCUMENTS_READ_TIMEOUT` | `15s`, incluida la lectura del cuerpo. |
| `M1_DOCUMENTS_MAX_FILE_BYTES` | `10485760` (10 MiB); configurable entre 1 y ese máximo. |

La configuración se valida al habilitarla; arrancar no realiza consultas HTTP. Es independiente de `M1_AUTH_ENABLED` y de `CORE_ENABLED`. Este adaptador implementa autorización Bearer de servicio: si M1 acuerda otro mecanismo, debe adaptarse antes de habilitarlo. No se obtuvo ni configuró una credencial real y no se activó ningún despliegue.

## Errores

Los errores usan el contrato `application/problem+json` de M8, con mensajes fijos sin el cuerpo ni las credenciales de M1.

| HTTP | Código / motivo |
| --- | --- |
| 400 | Parámetros UUID inválidos. |
| 401 / 403 | Sesión, usuario o permisos de M8 inválidos. |
| 404 | `APPLICATION_NOT_FOUND`, `M8_DOCUMENT_NOT_FOUND` (incluye referencia ajena) o `M1_DOCUMENT_FILE_NOT_FOUND`. |
| 409 | `M8_DOCUMENT_FILE_NOT_READY`: entrega pendiente o sin UUID. |
| 502 | `M1_DOCUMENT_ACCESS_DENIED`, `M1_DOCUMENT_TOO_LARGE`, `M1_DOCUMENT_INVALID_RESPONSE` o `M1_DOCUMENT_UPSTREAM_ERROR`. Un 401/403 de M1 indica un problema de la autorización de servicio. |
| 503 | `M1_DOCUMENT_DISABLED` o `M1_DOCUMENT_UNAVAILABLE` (incluye 429/503 remotos y fallos de conexión). |
| 504 | `M1_DOCUMENT_TIMEOUT`. |

## Verificación local

```powershell
.\gradlew.bat test --tests '*M1DocumentClientTest' --tests '*M1DocumentsConfigurationTest' --tests '*ExternalDocumentFileFlowTest' --tests '*ExternalDocumentationFlowTest' --tests '*OpenApiDocsGenerationTest' bootJar --console=plain --offline
```

El cliente HTTP se prueba únicamente con un servidor efímero en `127.0.0.1`. Las pruebas de endpoint usan H2, la seguridad real de M8 y un cliente sustituido; las de proxy deshabilitado no crean cliente documental. Core, AMQP y login M1 permanecen desactivados. Se comprueban permisos retirados, referencias ajenas, respuesta binaria, estados pendientes, límite declarado y chunked, timeout de encabezados/cuerpo, redirecciones y errores sin secretos.

Resultado: **112 casos aprobados, sin fallos ni omisiones**. `bootJar` y OpenAPI se generaron correctamente; la especificación queda en `build/openapi/openapi.json` y los reportes en `build/reports/tests/test/`.

No se ejecutaron pruebas contra M1, Core ni el backend desplegado. La interoperabilidad remota y el acuerdo de acceso por UUID siguen pendientes. No se requieren migraciones.
