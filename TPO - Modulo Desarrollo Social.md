# TPO - Módulo Desarrollo Social

> Fuente: `TPO - Desarollo de Apps II - Gestión de municipalidad.pdf` (Desarrollo de Aplicaciones II | Municipalidad UADE)

Este documento tiene dos partes:

1. **Estructura completa del TP** — resumen y explicación de las 12 secciones del enunciado, para tener el contexto general del trabajo.
2. **Texto textual de la Sección 8 (dentro del Listado de módulos)** — el módulo **"Desarrollo social, salud comunitaria y beneficios"**, copiado tal cual figura en el PDF, sin resumir ni parafrasear.

> Nota sobre la numeración: el PDF numera dos niveles distintos con el mismo esquema. Hay 12 secciones principales (1 a 12), y dentro de la sección **10. Listado de módulos** hay 9 sub-secciones numeradas 1 a 9 (una por equipo/módulo). El módulo pedido —Desarrollo Social— es el **ítem 8 de ese listado**, por eso este archivo se llama "Módulo Desarrollo Social".

---

## 1. Estructura completa del Trabajo Práctico Obligatorio

El TP se titula **"Municipalidad UADE — Plataforma distribuida para la gestión integral de una municipalidad"** y está organizado en 12 secciones:

### 1. Presentación del proyecto
Contexto narrativo: la Municipalidad de Ciudad UADE quiere transformarse digitalmente, integrando áreas hoy desconectadas (aplicaciones independientes, duplicación de datos, falta de trazabilidad). Se pide un **ecosistema de aplicaciones independientes** que funcionen como una solución única e integrada, donde cada equipo es dueño de un módulo. La integración entre módulos es principalmente por **eventos asincrónicos**, y por **APIs REST** solo cuando se necesita respuesta inmediata.

### 2. Objetivo general
Diseñar, desarrollar, desplegar e integrar la plataforma completa (trámites, reclamos, obras, habilitaciones, tributos, servicios urbanos, tránsito, programas sociales) manteniendo **independencia tecnológica y de datos** entre módulos. Cada módulo debe:
- Aplicar arquitectura de tres capas y separación de responsabilidades.
- Diseñar APIs REST documentadas.
- Implementar integración asincrónica basada en eventos.
- Aplicar autenticación en los servicios.
- Implementar pruebas unitarias con cobertura mínima del **85% en FE y BE** (cada uno).
- Desplegar los módulos en un entorno accesible.

### 3. Organización de los equipos
- 9 equipos en total (uno por módulo).
- Hasta 6 integrantes por equipo.
- Cada grupo diseña, desarrolla, prueba, documenta y despliega **su** módulo.
- Todos los integrantes deben conocer toda la solución de su equipo y poder defenderla.

### 4. Requerimientos generales
- Cada módulo: frontend + backend + base de datos propia.
- Frontend web o mobile.
- Backend con arquitectura de tres capas.
- Tecnología libre, pero justificada en las entregas.
- APIs documentadas (Swagger/OpenAPI/Postman o equivalente).
- Cada módulo publica y consume eventos.
- **Ningún módulo accede directamente a la base de datos de otro.**
- Los módulos deben tolerar la indisponibilidad temporal de otros componentes.
- La solución debe incluir autenticación y autorización.

### 5. Modalidad del proyecto
Lista de entregables funcionales esperados por módulo: frontend con navegación/validaciones/control de acceso, backend con API REST, base de datos independiente, arquitectura de tres capas, autenticación/autorización por roles, publicación y consumo de eventos, validaciones de negocio y transiciones de estado, manejo centralizado de errores, documentación técnica/funcional y despliegue accesible.

### 6. Arquitectura obligatoria
- **6.1 Frontend**: consume solo la API de su propio backend, no accede a bases de datos directamente, implementa validaciones/búsquedas/filtros/paginación/estados de carga, muestra operaciones según el rol autenticado, y debe incluir al menos un tablero/vista de indicadores.
- **6.2 Backend de tres capas**: capa de presentación (controladores, endpoints, contratos, códigos HTTP, validaciones de formato), capa de negocio (casos de uso, reglas, transiciones de estado, eventos) y capa de acceso a datos (repositorios, consultas, persistencia, transacciones, mapeo de entidades).
- **6.3 Base de datos**: independiente por módulo (relacional o no relacional), sin esquemas compartidos ni relaciones físicas entre módulos.

### 7. Integración entre módulos
Principalmente por eventos asincrónicos (Kafka, RabbitMQ u otra plataforma acordada). La integración sincrónica vía REST se permite solo cuando la respuesta es indispensable para continuar una operación (ej. validar identidad, consultar configuración, verificar una condición).

### 8. Formato común de eventos
Todos los eventos deben respetar un formato común, incluyendo como mínimo:
- Tipo de evento.
- Identificador único del evento.
- Fecha y hora de generación.
- Módulo de origen.
- Datos del evento.

### 9. Reglas generales de negocio e integración
1. Un evento ya procesado no debe generar efectos duplicados.
2. Los mensajes no procesables tras reintentos van a una Dead Letter Queue.
3. Los módulos conservan evidencia de eventos publicados y procesados.
4. Las fechas se almacenan en formato estándar con zona horaria definida.
5. Ningún módulo modifica directamente datos de otro sistema.
6. Las integraciones toleran indisponibilidad temporal de otros módulos.
7. Un error en un módulo no debe tumbar toda la plataforma.
8. Cada dato tiene un único módulo propietario.

### 10. Listado de módulos
Nueve módulos, cada uno con: objetivo, usuarios principales, información administrada, funcionalidades mínimas, reglas de negocio, flujo de información con otras áreas (qué recibe / qué envía) y una tabla de posibles eventos asincrónicos (qué publica / qué consume):

1. **Ciudadanos, organizaciones y expedientes digitales** — identidad municipal, domicilios, representantes, expedientes digitales. Es el punto de registro obligatorio previo para acceder a otros módulos.
2. **Atención ciudadana, reclamos y solicitudes** — recepción, clasificación, derivación y seguimiento de reclamos/denuncias/consultas.
3. **Obras públicas, infraestructura y mantenimiento urbano** — proyectos de obra, órdenes de trabajo, cuadrillas, cortes de calle.
4. **Habilitaciones, inspecciones y control comercial** — establecimientos, solicitudes de habilitación, inspecciones, clausuras, renovaciones.
5. **Rentas, tributos, deudas y planes de pago** — tasas, liquidaciones, pagos, intereses, exenciones, planes de regularización.
6. **Ambiente, higiene y servicios urbanos** — recolección, residuos, contenedores, arbolado, espacios verdes, operativos ambientales.
7. **Tránsito, estacionamiento y seguridad vial** — infracciones, operativos, accidentes, cortes de calle, estacionamiento medido, retención de vehículos.
8. **Desarrollo social, salud comunitaria y beneficios** — programas de asistencia, beneficios municipales, visitas sociales, salud comunitaria. **(Ver texto textual completo más abajo.)**
9. **Core: identidad, integración, notificaciones y monitoreo** — servicios técnicos comunes: identidad/acceso, catálogo de eventos, mensajería, trazabilidad, DLQ, monitoreo y notificaciones. Actúa como HUB de eventos y no implementa reglas de negocio de las demás áreas.

### 11. Flujo de información entre áreas
Reafirma que cada dato tiene un único módulo propietario: los módulos intercambian identificadores y hechos de negocio, pero no modifican datos ajenos. Incluye 4 ejemplos de flujos end-to-end entre módulos:
- **Reclamo por bache**: Ciudadanos → Atención Ciudadana → Obras → (Tránsito si hay corte de calle) → Atención Ciudadana.
- **Habilitación de un comercio**: Ciudadanos → Habilitaciones → Rentas (liquidación/pago) → Habilitaciones (inspección y aprobación/rechazo).
- **Infracción de tránsito**: Tránsito registra → descargo del ciudadano → confirmación → Rentas genera deuda → pago → Tránsito actualiza estado.
- **Solicitud de beneficio social**: Atención Ciudadana deriva → Desarrollo Social crea el caso, valida requisitos, programa visita → aprueba/rechaza/lista de espera.

### 12. Entregas
- **Primera entrega**: cada módulo con su BE y FE hechos, Swagger, BE y FE desplegados.
- **Segunda entrega**: módulos completamente integrados.
- **Entrega Final**: "Guilds" con Journey del cliente.

---

## 2. Texto textual — Módulo 8: Desarrollo social, salud comunitaria y beneficios

*(Copiado literalmente del PDF, sección 10 "Listado de módulos", ítem 8)*

### 8. Desarrollo social, salud comunitaria y beneficios

**Objetivo.** Gestionar programas de asistencia, beneficios municipales, visitas sociales, campañas y servicios básicos de salud comunitaria.

**Usuarios principales**
- Ciudadanos.
- Trabajadores sociales.
- Profesionales de centros municipales.
- Administrativos.
- Coordinadores y auditores.

**Información administrada por el módulo**
- Programas y convocatorias.
- Solicitudes y evaluaciones.
- Beneficiarios y listas de espera.
- Visitas e intervenciones.
- Ayudas y subsidios.
- Turnos y campañas comunitarias.

**Funcionalidades mínimas**
- Crear programas con objetivos, requisitos, cupos, beneficios y vigencia.
- Abrir, suspender y cerrar convocatorias.
- Registrar solicitudes y documentación.
- Validar identidad, grupo familiar, ingresos y domicilio.
- Detectar documentación faltante.
- Programar entrevistas y visitas domiciliarias.
- Registrar evaluación y calcular prioridad o vulnerabilidad.
- Asignar, suspender, renovar y finalizar beneficios.
- Registrar planes de intervención y seguimientos.
- Administrar centros municipales y agendas.
- Otorgar, reprogramar y cancelar turnos.
- Registrar asistencia y atención comunitaria.
- Gestionar campañas, talleres y actividades.
- Mostrar indicadores de cobertura, cupos y resultados.

**Reglas de negocio**
- Cada beneficio deberá tener requisitos configurables.
- Los cupos no podrán superarse.
- Los beneficios incompatibles no podrán otorgarse simultáneamente.
- Las renovaciones deberán reevaluar requisitos.

**Flujo de información con otras áreas**

Información que recibe.
- Datos personales y del grupo familiar.
- Solicitudes de asistencia desde Atención Ciudadana.
- Estado tributario y exenciones.
- Estado de comunicaciones.

Información que envía.
- Beneficios y exenciones recomendadas a Rentas.
- Actuaciones y resultados a Expedientes.
- Estados de intervención a Atención Ciudadana.
- Turnos, visitas y resultados a Notificaciones.

**Posibles eventos asincrónicos**

| Eventos que publica | Eventos que consume |
|---|---|
| ProgramaSocialCreado | CiudadanoRegistrado |
| SolicitudBeneficioSocialCreada | DomicilioActualizado |
| DocumentacionSocialSolicitada | ReclamoDerivado |
| VisitaSocialProgramada | ExpedienteIniciado |
| VisitaSocialRealizada | DeudaVencida |
| BeneficioSocialAprobado | ExencionAprobada |
| BeneficioSocialRechazado | NotificacionEnviada |
| BeneficioSocialSuspendido | NotificacionFallida |
| BeneficioSocialFinalizado | |
| TurnoSaludMunicipalOtorgado | |
| AtencionComunitariaRegistrada | |
| SituacionVulnerabilidadCriticaDetectada | |
