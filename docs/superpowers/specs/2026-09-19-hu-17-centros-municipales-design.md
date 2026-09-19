# HU-17 - Gestionar centros municipales

## Estado del documento

- Historia: HU-17
- Estado: lista para revision
- Story points informados: 4
- Fecha: 2026-09-19
- Alcance: centros municipales, catalogo de servicios, horarios, asignaciones profesionales y agendas

## Historia de usuario

Como administrativo municipal autorizado, quiero registrar y administrar centros municipales, sus servicios, profesionales y horarios, para organizar la disponibilidad y la atencion comunitaria.

## Objetivo

Permitir que el municipio mantenga un catalogo central de servicios y configure en que centros se presta cada servicio, que profesionales lo atienden y en que horarios estan disponibles.

La configuracion resultante sera la fuente de disponibilidad para las historias posteriores de consulta ciudadana y gestion de turnos.

## Contexto del proyecto

El backend esta implementado con Kotlin, Spring Boot y JPA. Las funcionalidades se organizan por feature y separan controladores, servicios, validadores, repositorios, entidades, DTO y errores.

La entidad existente `User` representa a las personas del sistema y obtiene sus capacidades mediante roles y permisos. La HU-17 reutilizara `User` para administrativos y profesionales; no creara entidades de identidad alternativas.

Actualmente no existen entidades de centros, servicios municipales, horarios, asignaciones profesionales, disponibilidad ni turnos. Las rutas de gestion de centros ya estan previstas en `notes/routes.md`, pero sus pantallas son placeholders.

## Actores

### Administrativo municipal

Puede:

- crear, consultar, editar y desactivar servicios del catalogo municipal;
- crear, consultar, editar y desactivar centros;
- asignar o retirar servicios de cada centro;
- configurar los horarios de apertura de un centro;
- asignar profesionales a servicios concretos de un centro;
- configurar la disponibilidad semanal de cada asignacion profesional.

En esta especificacion, "administrativo" es el nombre funcional del actor y corresponde al rol existente `ADMIN`; no se creara un rol `ADMINISTRATIVO`. El usuario debe operar con `ADMIN` y con el permiso especifico requerido por cada operacion.

### Profesional de centro

Es un `User` activo con el rol `PROFESIONAL_CENTRO`. En esta historia puede ser asignado a centros, servicios y agendas, pero no administra su propia configuracion.

### Ciudadano

No participa directamente en la HU-17. Las historias posteriores utilizaran la configuracion activa para mostrar centros, servicios y horarios disponibles y para otorgar turnos.

## Alcance funcional

### Incluido

1. Administrar un catalogo municipal centralizado de servicios.
2. Administrar centros municipales y su ubicacion postal.
3. Asignar uno o mas servicios del catalogo a cada centro.
4. Definir uno o mas horarios semanales de apertura por dia.
5. Asignar profesionales existentes a un servicio especifico de un centro.
6. Definir una agenda semanal para cada asignacion profesional.
7. Validar rangos horarios, contencion y superposiciones antes de guardar.
8. Desactivar registros sin eliminar su informacion historica.
9. Auditar las mutaciones realizadas por administrativos.

### Fuera de alcance

- consulta publica o ciudadana de centros y disponibilidad;
- solicitud, reprogramacion y cancelacion de turnos;
- generacion anticipada de turnos o slots;
- registro de asistencia o atencion comunitaria;
- salas, consultorios, equipamiento o capacidad fisica;
- feriados, licencias y excepciones para fechas particulares;
- notificaciones ante cambios;
- sincronizacion con calendarios externos;
- creacion o edicion de usuarios profesionales;
- geolocalizacion o coordenadas del centro.

## Decisiones funcionales

### Catalogo central de servicios

Los servicios no se crean dentro de cada centro. El administrativo los crea una sola vez en el catalogo municipal y luego los asigna a todos los centros que correspondan.

Los centros comparten la misma definicion. Cambiar el nombre, descripcion o duracion de un servicio se refleja en todos los centros que lo ofrecen.

Desactivar un servicio lo deja fuera de la oferta activa de todos los centros, pero conserva sus asignaciones para mantener el historial.

### Agenda especifica por servicio

Cada franja de disponibilidad pertenece a una asignacion compuesta por profesional, centro y servicio. Un horario no queda automaticamente disponible para los demas servicios que presta ese profesional.

Ejemplo:

```text
Ana -> Centro Norte -> Asesoramiento juridico -> lunes 09:00 a 12:00
Ana -> Centro Norte -> Orientacion familiar   -> lunes 14:00 a 17:00
```

### Horarios recurrentes

Los horarios de centros y profesionales son semanales y recurrentes. Se configuran por dia de la semana y hora local del municipio.

La HU-17 no maneja fechas concretas, feriados, licencias ni excepciones. Esas necesidades requeriran una extension posterior del modelo.

### Desactivacion

Ninguno de los seis recursos de la HU-17 se elimina fisicamente. Se desactivan para conservar referencias historicas y preparar el modelo para futuros turnos.

Una entidad inactiva no puede utilizarse para crear nuevas asociaciones o disponibilidades. Las relaciones existentes se conservan, pero dejan de formar parte de la oferta efectiva.

## Modelo de dominio

### Vista general

```text
MunicipalCenter
 |-- 1:N -- CenterOpeningHour
 |
 `-- 1:N -- CenterService -- N:1 -- MunicipalService
                 |
                 `-- 1:N -- ProfessionalAssignment -- N:1 -- User
                                      |
                                      `-- 1:N -- ProfessionalAvailability
```

### `MunicipalCenter`

Representa un centro municipal.

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| `id` | UUID | Identificador generado por el sistema. |
| `name` | Texto | Obligatorio, de 1 a 150 caracteres luego de quitar espacios exteriores. |
| `normalizedName` | Texto | Generado por el sistema y unico; no se acepta desde los DTO. |
| `address` | Texto | Direccion postal obligatoria, de 1 a 255 caracteres. |
| `phone` | Texto | Contacto telefonico opcional, de hasta 30 caracteres. |
| `email` | Texto | Contacto de correo opcional, de hasta 180 caracteres y con formato valido. |
| `active` | Booleano | Indica si el centro forma parte de la oferta activa. |
| `createdAt` | Instante | Generado por el sistema. |
| `updatedAt` | Instante | Actualizado por el sistema. |

Relaciones:

- tiene cero o mas `CenterOpeningHour`;
- tiene cero o mas `CenterService`.

### `MunicipalService`

Representa un servicio del catalogo municipal compartido por todos los centros.

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| `id` | UUID | Identificador generado por el sistema. |
| `name` | Texto | Obligatorio, de 1 a 150 caracteres luego de quitar espacios exteriores. |
| `normalizedName` | Texto | Generado por el sistema y unico; no se acepta desde los DTO. |
| `description` | Texto | Descripcion funcional obligatoria, de 1 a 1000 caracteres. |
| `durationMinutes` | Entero | Duracion estandar estimada de una atencion; obligatoria y positiva. |
| `active` | Booleano | Indica si puede ofrecerse y recibir nuevas asignaciones. |
| `createdAt` | Instante | Generado por el sistema. |
| `updatedAt` | Instante | Actualizado por el sistema. |

Relaciones:

- puede asignarse a cero o mas centros mediante `CenterService`.

Para centros y servicios, `normalizedName` se obtiene aplicando normalizacion Unicode NFC, eliminacion de espacios exteriores, reemplazo de secuencias de espacios internos por uno solo y conversion a minusculas con `Locale.ROOT`. Los acentos se conservan, por lo que `Atencion` y `Atención` son nombres diferentes.

### `CenterService`

Representa que un centro ofrece un servicio del catalogo.

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| `id` | UUID | Identificador generado por el sistema. |
| `center` | `MunicipalCenter` | Centro que ofrece el servicio. |
| `service` | `MunicipalService` | Servicio compartido del catalogo. |
| `active` | Booleano | Indica si el servicio se ofrece actualmente en ese centro. |
| `createdAt` | Instante | Generado por el sistema. |
| `updatedAt` | Instante | Actualizado por el sistema. |

Restricciones:

- la combinacion centro-servicio es unica;
- una asociacion desactivada se reactiva en lugar de duplicarse;
- ambos extremos deben estar activos al crear o reactivar la asociacion.

### `CenterOpeningHour`

Representa una franja semanal de apertura del centro.

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| `id` | UUID | Identificador generado por el sistema. |
| `center` | `MunicipalCenter` | Centro al que pertenece. |
| `dayOfWeek` | Enum | Dia de lunes a domingo. |
| `startTime` | Hora local | Inicio inclusivo de la franja. |
| `endTime` | Hora local | Fin exclusivo de la franja. |
| `active` | Booleano | Permite retirar una franja sin eliminarla. |
| `createdAt` | Instante | Generado por el sistema. |
| `updatedAt` | Instante | Actualizado por el sistema. |

Un centro puede tener varias franjas en un mismo dia, por ejemplo `08:00-12:00` y `14:00-18:00`.

### `ProfessionalAssignment`

Representa la asignacion de un profesional a un servicio concreto de un centro.

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| `id` | UUID | Identificador generado por el sistema. |
| `professional` | `User` | Usuario activo con rol `PROFESIONAL_CENTRO`. |
| `centerService` | `CenterService` | Servicio activo ofrecido por un centro activo. |
| `active` | Booleano | Indica si el profesional sigue asignado. |
| `createdAt` | Instante | Generado por el sistema. |
| `updatedAt` | Instante | Actualizado por el sistema. |

Restricciones:

- la combinacion profesional-servicio-del-centro es unica;
- una asignacion desactivada se reactiva en lugar de duplicarse;
- el usuario, centro, servicio y `CenterService` deben estar activos al crear o reactivar la asignacion.

### `ProfessionalAvailability`

Representa una franja semanal en la que el profesional atiende el servicio de su asignacion.

| Campo | Tipo conceptual | Regla |
| --- | --- | --- |
| `id` | UUID | Identificador generado por el sistema. |
| `assignment` | `ProfessionalAssignment` | Asignacion profesional-centro-servicio. |
| `dayOfWeek` | Enum | Dia de lunes a domingo. |
| `startTime` | Hora local | Inicio inclusivo de la disponibilidad. |
| `endTime` | Hora local | Fin exclusivo de la disponibilidad. |
| `active` | Booleano | Permite retirar una franja sin eliminarla. |
| `createdAt` | Instante | Generado por el sistema. |
| `updatedAt` | Instante | Actualizado por el sistema. |

La disponibilidad es especifica del servicio de la asignacion. Para ofrecer otro servicio se requiere otra asignacion y sus propias franjas.

## Reglas de negocio

### RN-01 - Rango horario valido

En horarios de centro y disponibilidades profesionales, `startTime` debe ser estrictamente anterior a `endTime`.

No se admiten franjas vacias ni franjas que alcancen o atraviesen la medianoche. La representacion de horarios nocturnos queda fuera del alcance de la HU-17 porque `LocalTime` no permite expresar `24:00` como fin de una franja del mismo dia.

### RN-02 - Semantica de los limites

Los intervalos son semiabiertos: `[inicio, fin)`. El inicio pertenece a la franja y el final no.

Por lo tanto, `09:00-10:00` y `10:00-11:00` son adyacentes y no se consideran superpuestos.

### RN-03 - Horarios del centro sin superposicion

Dos franjas activas del mismo centro y dia se superponen cuando:

```text
existente.startTime < nueva.endTime
AND existente.endTime > nueva.startTime
```

Si se edita una franja, la consulta excluye el identificador de esa misma franja.

### RN-04 - Disponibilidad contenida en el horario del centro

Toda disponibilidad profesional debe quedar completamente cubierta, sin interrupciones, por una o mas franjas activas y adyacentes de apertura del mismo centro y dia.

Ejemplo:

```text
Centro:       lunes 08:00-12:00
Profesional:  lunes 09:00-11:00  -> valido
Profesional:  lunes 11:00-13:00  -> invalido
```

Si el centro tiene `08:00-12:00` y `12:00-18:00`, ambas franjas forman una cobertura continua y admiten una disponibilidad `10:00-14:00`.

### RN-05 - Agenda profesional sin superposicion global

Un profesional no puede tener disponibilidades efectivas superpuestas el mismo dia, aunque pertenezcan a centros o servicios diferentes.

La validacion considera todas las disponibilidades efectivas del mismo `User`, no solamente la asignacion que se esta editando. Una activacion debe comparar todas las disponibilidades que volveria efectivas, tanto entre si como contra las que ya eran efectivas.

### RN-06 - Servicio especifico por franja

Cada disponibilidad habilita solamente el servicio de su `ProfessionalAssignment`. No habilita automaticamente otros servicios del profesional.

### RN-07 - Entidades activas

Para crear o reactivar una relacion, todos sus antecedentes deben estar activos.

Una disponibilidad es efectiva solo cuando estan activos:

- la propia disponibilidad;
- su asignacion profesional;
- el `User` profesional;
- el rol `PROFESIONAL_CENTRO` del usuario;
- el `CenterService`;
- el centro;
- el servicio municipal.

Ademas, debe seguir cubierta sin interrupciones por horarios activos del centro.

### RN-08 - Cambios del catalogo compartido

Editar un `MunicipalService` modifica la definicion observada por todos los centros asociados. La relacion `CenterService` no copia nombre, descripcion ni duracion.

### RN-09 - Desactivacion y dependencias

Desactivar una entidad padre no elimina ni modifica fisicamente sus relaciones hijas. Las relaciones dejan de ser efectivas por la regla de entidades activas.

Reactivar una entidad padre no reactiva automaticamente relaciones que fueron desactivadas de manera explicita.

Antes de activar un usuario profesional, centro, servicio, `CenterService`, asignacion o disponibilidad, el sistema debe bloquear los centros y profesionales afectados y volver a validar RN-04, RN-05 y RN-07. La activacion completa se rechaza si alguna disponibilidad que volveria a ser efectiva queda fuera de cobertura o se superpone con otra.

Editar, desactivar o reactivar un horario de centro tambien debe preservar RN-04. La operacion se rechaza si deja alguna disponibilidad efectiva sin cobertura continua.

### RN-10 - Proteccion del profesional

No puede eliminarse fisicamente un `User` que posea asignaciones profesionales. Puede desactivarse mediante el mecanismo existente de usuarios.

El rol `PROFESIONAL_CENTRO` no puede retirarse mientras el usuario tenga asignaciones activas. Primero deben desactivarse esas asignaciones. Esto evita conservar agendas aparentemente activas para un usuario que ya no puede actuar como profesional.

## Casos de uso

### CU-01 - Crear un servicio municipal

1. El administrativo informa nombre, descripcion y duracion estimada.
2. El sistema normaliza y valida los datos.
3. El sistema comprueba que no exista otro servicio con el mismo nombre normalizado.
4. El sistema crea el servicio activo.
5. El sistema registra la auditoria y confirma el alta.

### CU-02 - Crear un centro municipal

1. El administrativo informa nombre, direccion y datos de contacto.
2. El sistema valida los datos y la unicidad del nombre normalizado.
3. El sistema crea el centro activo.
4. El sistema registra la auditoria y confirma el alta.

Los servicios, horarios y profesionales pueden configurarse despues del alta. Crear un centro no exige completar toda su configuracion en una unica operacion.

### CU-03 - Asignar un servicio a un centro

1. El administrativo selecciona un centro activo y un servicio activo del catalogo.
2. El sistema comprueba que la combinacion no este activa.
3. Si existia una relacion desactivada, la reactiva; de lo contrario, crea `CenterService`.
4. El sistema registra la auditoria y confirma la asignacion.

### CU-04 - Configurar horarios del centro

1. El administrativo selecciona un dia y define inicio y fin.
2. El sistema valida el orden del rango.
3. El sistema bloquea el centro y luego los profesionales afectados para serializar cambios concurrentes.
4. El sistema busca superposiciones activas del mismo centro y dia.
5. En ediciones, desactivaciones y reactivaciones, comprueba que las disponibilidades efectivas sigan cubiertas.
6. Si no hay conflicto, guarda el cambio y registra la auditoria.
7. Si hay conflicto, no guarda cambios e informa la franja o disponibilidad incompatible.

### CU-05 - Asignar un profesional

1. El administrativo selecciona un servicio activo de un centro activo.
2. Selecciona un `User` activo con rol `PROFESIONAL_CENTRO`.
3. El sistema crea o reactiva la asignacion unica.
4. El sistema registra la auditoria y confirma la asignacion.

### CU-06 - Configurar la agenda profesional

1. El administrativo selecciona una asignacion profesional activa.
2. Define dia, hora inicial y hora final.
3. El sistema valida el rango y que este contenido en el horario del centro.
4. El sistema bloquea primero el centro y despues al profesional para serializar cambios concurrentes.
5. El sistema busca superposiciones en todas las asignaciones del profesional.
6. Si no hay conflicto, guarda la disponibilidad y registra la auditoria.
7. Si hay conflicto, no guarda cambios e informa la franja y asignacion incompatibles.

### Flujo resumido

```text
Administrativo
      |
      | crea o selecciona servicio del catalogo
      v
MunicipalService
      |
      | asigna a
      v
MunicipalCenter ---- configura ----> CenterOpeningHour
      |
      | asigna profesional al servicio
      v
ProfessionalAssignment
      |
      | configura agenda
      v
ProfessionalAvailability
      |
      | valida rango, contencion y superposicion
      v
Guardar y auditar / Rechazar e informar conflicto
```

## Criterios de aceptacion

### CA-01 - Registrar centro

```gherkin
Dado un administrativo autorizado
Cuando registra un centro con nombre unico, direccion valida y datos de contacto validos cuando se informan
Entonces el sistema crea el centro activo
Y confirma la operacion
Y registra la auditoria
```

### CA-02 - Administrar catalogo y asignar servicios

```gherkin
Dado un servicio activo del catalogo municipal
Y un centro activo
Cuando el administrativo asigna el servicio al centro
Entonces el centro comienza a ofrecer ese mismo servicio compartido
Y no se crea una copia del servicio
```

### CA-03 - Propagar cambios del servicio

```gherkin
Dado un servicio asignado a varios centros
Cuando el administrativo modifica su definicion
Entonces todos los centros asociados reflejan la nueva definicion
```

### CA-04 - Registrar varios horarios por dia

```gherkin
Dado un centro activo
Cuando el administrativo registra las franjas 08:00-12:00 y 14:00-18:00 para el lunes
Entonces el sistema guarda ambas franjas
```

### CA-05 - Rechazar rango invalido

```gherkin
Dado un centro o una asignacion profesional
Cuando el administrativo intenta registrar una franja cuyo inicio es igual o posterior al fin
Entonces el sistema rechaza la operacion
Y explica que el rango horario es invalido
```

### CA-06 - Rechazar horarios superpuestos del centro

```gherkin
Dado que un centro abre el lunes de 08:00 a 12:00
Cuando el administrativo intenta agregar una franja el lunes de 11:00 a 14:00
Entonces el sistema rechaza la operacion por superposicion
Y devuelve la franja que produce el conflicto
```

### CA-07 - Permitir horarios adyacentes

```gherkin
Dado que existe una franja de 09:00 a 10:00
Cuando el administrativo registra otra franja de 10:00 a 11:00 para el mismo recurso y dia
Entonces el sistema acepta la nueva franja si cumple las demas reglas
```

### CA-08 - Asignar profesional a un servicio

```gherkin
Dado un profesional activo con rol PROFESIONAL_CENTRO
Y un servicio activo ofrecido por un centro activo
Cuando el administrativo realiza la asignacion
Entonces el sistema vincula al profesional con ese servicio del centro
Y no duplica una asignacion existente
```

### CA-09 - Exigir disponibilidad dentro del horario del centro

```gherkin
Dado que el centro abre el martes de 09:00 a 17:00
Cuando se registra disponibilidad profesional de 08:00 a 12:00
Entonces el sistema rechaza la operacion
Y explica que la disponibilidad queda fuera del horario del centro
```

### CA-10 - Rechazar superposicion profesional entre servicios

```gherkin
Dado que un profesional atiende un servicio el miercoles de 09:00 a 12:00
Cuando se registra para otro servicio una disponibilidad el miercoles de 11:00 a 13:00
Entonces el sistema rechaza la nueva disponibilidad
Y muestra la asignacion y franja que producen el conflicto
```

### CA-11 - Rechazar superposicion profesional entre centros

```gherkin
Dado que un profesional atiende en el Centro Norte el jueves de 10:00 a 13:00
Cuando se intenta asignarle disponibilidad en el Centro Sur el jueves de 12:00 a 15:00
Entonces el sistema rechaza la nueva disponibilidad
```

### CA-12 - Agenda especifica por servicio

```gherkin
Dado que un profesional ofrece dos servicios en un centro
Cuando se registra disponibilidad para uno de esos servicios
Entonces la franja queda asociada solamente a ese servicio
Y no habilita automaticamente el otro servicio
```

### CA-13 - Conservar relaciones al desactivar

```gherkin
Dado un servicio con centros, profesionales y disponibilidades asociadas
Cuando el administrativo desactiva el servicio
Entonces el sistema conserva todas sus relaciones
Pero deja de considerarlas parte de la oferta efectiva
Y no permite crear nuevas asignaciones para ese servicio
```

### CA-14 - Preservar agendas al modificar horarios del centro

```gherkin
Dado que un centro abre el lunes de 08:00 a 12:00
Y un profesional tiene disponibilidad efectiva de 09:00 a 11:00
Cuando el administrativo intenta reducir la apertura a 10:00-12:00
Entonces el sistema rechaza la modificacion
Y conserva sin cambios el horario anterior
Y explica que la disponibilidad quedaria sin cobertura
```

### CA-15 - Revalidar al reactivar

```gherkin
Dado un recurso inactivo cuya activacion volveria efectivas una o mas disponibilidades
Y alguna de esas disponibilidades quedaria sin cobertura o superpuesta
Cuando el administrativo intenta reactivar el recurso
Entonces el sistema rechaza toda la reactivacion
Y muestra la falta de cobertura o superposicion encontrada
```

Este criterio se aplica a la reactivacion de usuarios profesionales, centros, servicios municipales, servicios de un centro, asignaciones profesionales y disponibilidades.

### CA-16 - Rechazar relaciones con antecedentes inactivos

```gherkin
Dado un centro, servicio, asociacion o profesional inactivo
Cuando el administrativo intenta crear o reactivar una relacion que depende de ese registro
Entonces el sistema rechaza la operacion
Y no realiza escrituras parciales
```

### CA-17 - Proteger el rol profesional

```gherkin
Dado un usuario con asignaciones profesionales activas
Cuando se intenta retirar su rol PROFESIONAL_CENTRO
Entonces el sistema rechaza la operacion
Y solicita desactivar primero sus asignaciones
```

### CA-18 - Validar la duracion estimada del servicio

```gherkin
Dado un administrativo autorizado
Cuando intenta crear o editar un servicio con duracion estimada igual o menor a cero
Entonces el sistema rechaza la operacion
Y explica que la duracion debe ser positiva
```

### CA-19 - Conservar franjas desactivadas

```gherkin
Dado un horario de centro o disponibilidad profesional existente
Cuando el administrativo lo desactiva
Entonces el sistema conserva el registro como inactivo
Y deja de considerarlo efectivo
Y una reactivacion posterior vuelve a validar rango, cobertura y superposiciones antes de confirmarse
```

## Arquitectura propuesta

### Backend

Se incorporara la feature `center`, siguiendo la estructura existente:

```text
server/src/main/kotlin/com/uade/dda2/server/feature/center/
 |-- controller/
 |-- dto/
 |-- entity/
 |-- error/
 |-- mapper/
 |-- repository/
 |-- service/
 `-- validator/
```

Responsabilidades:

- los controladores validan formato, autenticacion y permisos;
- los servicios delimitan transacciones y coordinan auditoria;
- los validadores concentran reglas de negocio y conflictos horarios;
- los repositorios resuelven persistencia, busquedas de solapamiento y bloqueos;
- los mappers separan DTO de entidades JPA.

Las entidades de negocio utilizaran UUID, enums almacenados como texto, relaciones `LAZY`, restricciones declarativas y timestamps, siguiendo las convenciones actuales.

### API administrativa

La API se separara por recurso y evitara una operacion monolitica para guardar toda la configuracion del centro.

```text
/api/admin/municipal-services
/api/admin/municipal-centers
/api/admin/municipal-centers/{centerId}/opening-hours
/api/admin/municipal-centers/{centerId}/services
/api/admin/center-services/{centerServiceId}/professionals
/api/admin/professional-assignments/{assignmentId}/availability
```

Todos los recursos admiten consulta y excluyen el borrado fisico. El ciclo de vida de escritura sera:

| Recurso | Operaciones | Permiso de lectura | Permiso de escritura | Campos de relacion inmutables |
| --- | --- | --- | --- | --- |
| `MunicipalService` | Crear, editar, desactivar y reactivar. | `services:management:view` | Permiso `services:management:*` correspondiente a la accion. | No aplica. |
| `MunicipalCenter` | Crear, editar, desactivar y reactivar. | `centers:management:view` | Permiso `centers:management:*` correspondiente a la accion. | No aplica. |
| `CenterService` | Asignar, desactivar y reactivar. | `centers:management:view` | `centers:management:edit` | `center` y `service`. |
| `CenterOpeningHour` | Crear, editar dia/horas, desactivar y reactivar. | `schedules:management:view` | `schedules:management:manage` | `center`. |
| `ProfessionalAssignment` | Asignar, desactivar y reactivar. | `centers:management:view` | `centers:management:edit` | `professional` y `centerService`. |
| `ProfessionalAvailability` | Crear, editar dia/horas, desactivar y reactivar. | `schedules:management:view` | `schedules:management:manage` | `assignment`. |

Para cambiar un campo de relacion inmutable se desactiva la relacion anterior y se crea o reactiva la correcta. Los DTO de escritura no aceptaran IDs generados, timestamps, nombres normalizados ni actores de auditoria.

Los centros y servicios pueden editarse activos o inactivos. Las franjas inactivas tambien pueden editar sus datos propios, pero siguen sin ser efectivas hasta su reactivacion. Toda edicion valida sus campos intrinsecos; si el registro es efectivo, tambien debe preservar RN-03, RN-04 y RN-05. Crear o reactivar una relacion siempre exige antecedentes activos.

### Frontend

Se reutilizaran las rutas previstas:

```text
/gestion/centros
/gestion/centros/{centroId}
/gestion/centros/{centroId}/agenda
```

Se agregara una vista administrativa del catalogo central bajo:

```text
/gestion/centros/servicios
```

Distribucion funcional:

- listado de centros: alta, consulta, edicion y desactivacion;
- catalogo de servicios: alta, consulta, edicion y desactivacion;
- detalle del centro: datos, servicios ofrecidos y profesionales asignados;
- agenda del centro: horarios de apertura y disponibilidades profesionales.

Los guards del frontend mejoran la navegacion, pero el backend revalidara permisos en todos los endpoints.

## Autorizacion

Permisos propuestos:

```text
centers:management:view
centers:management:create
centers:management:edit
centers:management:change-status
services:management:view
services:management:create
services:management:edit
services:management:change-status
schedules:management:view
schedules:management:manage
```

El rol existente `ADMIN` representa al actor administrativo de la HU-17 y recibira los permisos nuevos definidos para esta funcionalidad. No se creara un rol adicional llamado `ADMINISTRATIVO`.

La incorporacion sera estrictamente aditiva:

- no se eliminaran, renombraran ni modificaran permisos existentes;
- no se quitaran ni reemplazaran asignaciones actuales entre roles y permisos;
- se crearan solamente los permisos nuevos de la HU-17 que no existan;
- esos permisos nuevos se agregaran al rol `ADMIN` sin alterar sus permisos actuales.

El rol `PROFESIONAL_CENTRO` no recibira permisos de mutacion de esta historia. La consulta de su propia agenda se definira junto con la historia correspondiente.

## Persistencia e integridad

### Restricciones de base de datos

Se definiran como minimo:

- unicidad del nombre normalizado de centro;
- unicidad del nombre normalizado de servicio;
- unicidad de `(center_id, service_id)` en `CenterService`;
- unicidad de `(professional_id, center_service_id)` en `ProfessionalAssignment`;
- checks `start_time < end_time` para horarios y disponibilidades;
- claves foraneas para todas las relaciones.

La ausencia de superposiciones requiere consultas de negocio porque abarca filas diferentes y, para profesionales, asignaciones diferentes.

Los nombres normalizados se persistiran en columnas dedicadas con restriccion unica. La normalizacion se aplicara en altas y modificaciones antes de comprobar la unicidad.

### Concurrencia

La validacion previa por si sola no evita que dos solicitudes concurrentes inserten franjas incompatibles.

Para mantener el patron existente del proyecto:

- se bloquearan pesimisticamente los centros afectados, ordenados por identificador cuando haya mas de uno;
- a continuacion se bloquearan los `User` profesionales afectados, tambien ordenados por identificador;
- la validacion y el guardado ocurriran dentro de la misma transaccion.

Todas las operaciones respetaran el orden centro-profesional para evitar interbloqueos. Bloquear solamente la asignacion no es suficiente porque el conflicto puede pertenecer a otra asignacion del mismo profesional.

### Auditoria

Se ampliara `LogEntityType` con:

```text
MUNICIPAL_CENTER
MUNICIPAL_SERVICE
CENTER_SERVICE
CENTER_OPENING_HOUR
PROFESSIONAL_ASSIGNMENT
PROFESSIONAL_AVAILABILITY
```

Las altas, modificaciones, activaciones y desactivaciones registraran actor, tipo de entidad, identificador y valores anteriores/nuevos mediante el mecanismo de auditoria existente.

## Errores

| Situacion | HTTP | Respuesta esperada |
| --- | --- | --- |
| Formato invalido o inicio no anterior al fin | `400 Bad Request` | Campo y regla incumplida. |
| Actor no autenticado | `401 Unauthorized` | Error de autenticacion existente. |
| Actor sin permiso | `403 Forbidden` | Error de autorizacion existente. |
| Entidad inexistente | `404 Not Found` | Tipo e identificador buscado. |
| Nombre o asignacion duplicada | `409 Conflict` | Recurso que ya existe. |
| Horario superpuesto | `409 Conflict` | Dia, rango y recurso en conflicto. |
| Disponibilidad fuera del horario del centro | `409 Conflict` | Horario del centro requerido. |
| Entidad inactiva | `409 Conflict` | Entidad que impide la operacion. |

Ningun error de negocio debe producir escrituras parciales.

## Estrategia de pruebas

### Pruebas unitarias

- rango con inicio anterior al fin;
- rechazo de rangos iguales o invertidos;
- deteccion de superposicion parcial, total y contenida;
- aceptacion de rangos adyacentes;
- exclusion del propio registro al editar;
- contencion de disponibilidad en una franja o union de franjas adyacentes del centro;
- conflicto profesional entre servicios diferentes;
- conflicto profesional entre centros diferentes;
- validacion de entidades y roles activos;
- normalizacion y unicidad de nombres.

### Pruebas de integracion backend

- alta, consulta, modificacion, desactivacion y reactivacion de los seis recursos;
- propagacion de cambios del catalogo compartido;
- unicidad de centro-servicio y profesional-asignacion;
- autorizacion segun el permiso otorgado por el rol activo;
- auditoria de altas y modificaciones;
- rollback completo ante conflictos;
- rechazo de cambios de apertura que dejen disponibilidades sin cobertura;
- revalidacion de cobertura y superposiciones al reactivar padres;
- dos escrituras concurrentes sobre horarios del mismo centro;
- dos escrituras concurrentes sobre asignaciones distintas del mismo profesional;
- concurrencia entre un cambio de apertura y un cambio de disponibilidad;
- proteccion contra borrado de usuarios con asignaciones;
- proteccion contra retiro del rol profesional con asignaciones activas.

### Pruebas frontend

- formularios y validaciones inmediatas de campos;
- seleccion de servicios desde el catalogo central;
- seleccion exclusiva de usuarios profesionales activos;
- visualizacion clara del rango que causa un conflicto;
- actualizacion de listados despues de altas, ediciones y desactivaciones;
- guards para el rol `ADMIN`;
- visualizacion en escritorio y dispositivos moviles.

## Dependencias con historias posteriores

La HU-17 deja preparado el modelo para que otras historias incorporen:

```text
ProfessionalAvailability
          |
          `-- futura fuente de horarios para Appointment
                                           |
                                           `-- futura CommunityAttention
```

El modelo concreto de turnos y atenciones se definira en sus historias correspondientes. Esas historias podran consumir la asignacion y disponibilidad, pero no forman parte de esta especificacion.

La consulta ciudadana debera mostrar solamente configuraciones efectivas segun RN-07. Esta regla no implica que los endpoints ciudadanos formen parte de la HU-17.

## Criterio de finalizacion

La HU-17 se considera terminada cuando:

1. Un administrativo autorizado puede administrar el catalogo municipal de servicios.
2. Puede crear y mantener centros con ubicacion y datos de contacto opcionales.
3. Puede asignar servicios compartidos a uno o varios centros.
4. Puede configurar horarios semanales sin rangos invalidos ni superpuestos.
5. Puede asignar profesionales existentes a servicios concretos de cada centro.
6. Puede configurar una agenda semanal especifica por servicio.
7. El sistema impide superposiciones globales de un profesional.
8. Todas las mutaciones quedan auditadas.
9. Las entidades pueden desactivarse sin perder relaciones historicas.
10. Las pruebas unitarias, de integracion y frontend definidas para el alcance pasan correctamente.

## Riesgos y consideraciones

- La estimacion original de 4 story points es ajustada para un subsistema que parte sin entidades ni endpoints y requiere seis recursos, pantallas, permisos, auditoria y pruebas. Conviene revisar la estimacion antes de planificar la implementacion.
- El proyecto utiliza actualmente `ddl-auto=update` y no posee migraciones versionadas. Las restricciones nuevas deben reflejarse tambien en la documentacion de base de datos y los datos iniciales.
- La agenda recurrente no representa feriados ni ausencias. Una futura ampliacion debera definir excepciones fechadas sin alterar el significado de estas franjas base.
- La zona horaria operativa sera la del municipio. Antes de implementar turnos con fechas concretas debera unificarse la politica temporal del modulo.
