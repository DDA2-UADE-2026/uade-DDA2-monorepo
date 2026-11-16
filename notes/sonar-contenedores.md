# Revisión de avisos Sonar: contenedores y HTTP local

## Cambios de ejecución

- [Frontend](../client/Dockerfile): Nginx sin privilegios, UID/GID `101:101`, puerto interno `8080`, sin permisos de escritura sobre los assets.
- [Backend](../server/Dockerfile): usuario dedicado `10001:10001`. El [entrypoint local](../server/local-entrypoint.sh) copia los certificados públicos del JDK a un truststore privado temporal (`0700` para el directorio, `0600` para el archivo) e importa allí la CA local. No modifica el JDK ni desactiva la validación TLS.
- [Core mock](../infra/local/core-mock/Dockerfile): usuario dedicado `10002:10002`, mantiene el puerto `8443`; código propiedad de root, legible pero no modificable por el proceso.
- [Preparación de secretos](../infra/local/init/init.sh): asigna únicamente los directorios y archivos explícitos de servidor/mock a sus respectivos consumidores; directorios `0700`, archivos `0600`. También corrige permisos de volúmenes existentes sin rotar credenciales. Los montajes de los consumidores siguen siendo de solo lectura. La clave privada de la CA no se comparte ni cambia de propietario.

### Acción necesaria en Dokploy

Antes de desplegar la nueva imagen frontend, cambiar el puerto de destino del dominio/proxy de **80 a 8080** y adaptar los healthchecks internos externos al repositorio. Aplicar tanto al despliegue de desarrollo como al de producción. La URL pública y su HTTPS no cambian.

En [Compose](../docker-compose.yml), el mapeo es `127.0.0.1:5173:8080`: el acceso local sigue siendo `http://localhost:5173`. El puerto del backend continúa siendo `8080` en su propio contenedor.

## Avisos que requieren revisión, no supresión automática

Estos textos sirven como evidencia para revisar los avisos en Sonar. Este cambio **no modifica el estado de los avisos en Sonar** ni los clasifica como falsos positivos. El estado final debe reflejar la revisión y aceptación del riesgo por el equipo, según las opciones disponibles en Sonar.

### Inicializador ejecutado como root

Ubicación: [imagen de inicialización](../infra/local/init/Dockerfile).

Justificación sugerida:

> Uso intencional y limitado al inicializador local de una sola ejecución. Necesita preparar volúmenes privados y asignar distintos UID/GID al servidor y al mock, también al migrar archivos existentes propiedad de root. No sirve tráfico ni publica puertos, no monta el socket Docker ni directorios del host. Finaliza antes del arranque de sus consumidores, que se ejecutan sin root y montan los secretos en solo lectura. No se relajan los permisos de la CA ni se exponen credenciales en logs.

Este caso es una excepción de mínimo privilegio para una tarea concreta, no una autorización general para ejecutar servicios persistentes como root. Reevaluar si se agregan comandos descargados, montajes del host, acceso al daemon o tareas de larga duración.

### HTTP en el filtro QR de Vite

Ubicación: [configuración de desarrollo](../client/vite.config.ts), filtro de `vite-plugin-qrcode`.

Justificación sugerida:

> El literal HTTP selecciona URLs de la red de desarrollo para mostrar un QR. El filtro no realiza peticiones, no transmite credenciales y no configura el TLS de producción. Su uso se limita a un entorno de desarrollo controlado con datos y credenciales de prueba. Producción debe mantener HTTPS en su proxy. No se reemplaza el literal por HTTPS sin configurar certificados ni se oculta el aviso mediante concatenación de cadenas.

El tráfico HTTP de desarrollo no está cifrado: no usar accesos reales, datos personales reales ni redes no confiables para sesiones que requieran protección. Si hace falta acceso remoto seguro, configurar TLS real o retirar el acceso LAN/QR; no basta con cambiar el texto del filtro.

## Verificación

Pruebas sin daemon ni credenciales reales:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infra/local -p 'test_*.py' -v
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infra/local/core-mock -p 'test_*.py' -v
sh -n infra/local/init/init.sh
sh -n server/local-entrypoint.sh
```

Las pruebas de permisos remapean los volúmenes a directorios temporales y registran `chown` sin cambiar propietarios del host. Las del entrypoint sustituyen Java/keytool para comprobar la copia privada, permisos, reinicio y fallo cerrado. No sustituyen una prueba de UID/propiedad efectivos dentro de Docker.

Con Docker iniciado, validar en un proyecto aislado con credenciales ficticias y puertos libres: build de las imágenes, UID no cero de los tres servicios, acceso a secretos sin imprimirlos, salud HTTP/HTTPS, arranque con volúmenes nuevos y migración/reinicio con volúmenes previamente root/0600, conservando certificados y credenciales. No usar los volúmenes de trabajo ni borrar datos para esta prueba. El [README](../README.md) describe el smoke local y sus límites.
