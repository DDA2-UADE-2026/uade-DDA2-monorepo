# Módulo 8 — Estructura de rutas del frontend

**Stack:** React + TanStack Router (file-based routing) + TanStack Query + shadcn/ui + Tailwind
**Capa de API:** hey-api — tipos, cliente y schemas de Zod generados desde el OpenAPI del backend
**Layout:** aplicación con sidebar, dos ramas según el rol actual de la sesión

---

## Índice

1. [Decisión de arquitectura: dos ramas por rol](#1-decisión-de-arquitectura-dos-ramas-por-rol)
2. [Árbol de rutas](#2-árbol-de-rutas)
3. [Guards de autenticación y autorización](#3-guards-de-autenticación-y-autorización)
4. [Cambio de rol en caliente](#4-cambio-de-rol-en-caliente)
5. [Filtros, búsqueda y paginación en la URL](#5-filtros-búsqueda-y-paginación-en-la-url)
6. [Capa de API generada con hey-api](#6-capa-de-api-generada-con-hey-api)
7. [Sidebar](#7-sidebar)
8. [Detalles de implementación](#8-detalles-de-implementación)
9. [Trazabilidad con el documento de alcance](#9-trazabilidad-con-el-documento-de-alcance)

---

# 1. Decisión de arquitectura: dos ramas por rol

El árbol se divide en dos ramas según `ROL_ACTUAL`:

| Rama | Rol actual | Qué contiene |
|---|---|---|
| `/portal` | `CIUDADANO` | Autogestión: catálogo, solicitudes propias, beneficios propios, turnos propios, campañas |
| `/gestion` | Cualquier rol municipal | Operación: bandeja de casos, evaluación, resolución, ABM, agendas, indicadores, auditoría |

**Por qué separarlas.** Si ambos modos compartieran rutas, cada pantalla tendría que decidir internamente qué mostrar y qué datos pedir. Separándolas, el guard vive una sola vez en el layout de cada rama y las pantallas quedan limpias. Además el sidebar cambia por completo al conmutar de rol, y con esta estructura eso sale gratis: cada layout renderiza su propio menú.

**Consecuencia deseada:** una trabajadora social que pasa a modo ciudadana navega a `/portal` y ve exactamente lo mismo que cualquier vecina. No hay pantallas "a medias" ni menús con la mitad de los ítems deshabilitados.

> Algunas rutas aparecen en las dos ramas, y está bien: `/portal/programas` es el catálogo de solo lectura con la acción "solicitar", mientras que `/gestion/programas` es el ABM. Se comparten los componentes de presentación, no la ruta.

---

# 2. Árbol de rutas

Se usa **routing por directorios**: cada carpeta que necesita layout propio lleva un `route.tsx`, y las rutas hijas viven dentro de esa carpeta. Evita los nombres punteados largos (`_app.gestion.casos.$solicitudId.evaluacion.tsx`) y hace visible de un vistazo qué layout envuelve a qué.

**Convenciones**

| Archivo | Qué es |
|---|---|
| `route.tsx` | Layout de esa carpeta. Renderiza `<Outlet />`. Es donde va el guard. |
| `index.tsx` | Ruta índice de la carpeta (la URL de la carpeta, sin segmento extra). |
| `_carpeta/` | Carpeta con guión bajo inicial: **pathless**, no aporta segmento a la URL. |
| `$param.tsx` | Segmento dinámico. |

```
src/routes/
├── __root.tsx                            contexto del router, providers, error boundary
├── 403.tsx                          →  /403        sin permisos
├── 404.tsx                          →  /404        no encontrado
│
├── _auth/                                pathless — layout público, sin sidebar
│   ├── route.tsx
│   ├── login.tsx                    →  /login              SSO + credencial municipal
│   ├── callback.tsx                 →  /callback           recibe token y emite sesión propia
│   └── seleccionar-rol.tsx          →  /seleccionar-rol    solo si ROL_ASIGNADO no es nulo
│
└── _app/                                 pathless — layout con sidebar
    ├── route.tsx                         guard nivel 1: sesión válida
    │
    ├── portal/
    │   ├── route.tsx                     guard nivel 2: ROL_ACTUAL = CIUDADANO
    │   ├── index.tsx                →  /portal                        P01  mi tablero
    │   ├── programas/
    │   │   ├── index.tsx            →  /portal/programas              P02  catálogo con filtros
    │   │   └── $programaId.tsx      →  /portal/programas/:id          P03  detalle + solicitar
    │   ├── solicitudes/
    │   │   ├── index.tsx            →  /portal/solicitudes            P05  mis solicitudes
    │   │   ├── nueva.tsx            →  /portal/solicitudes/nueva      P04  nueva solicitud
    │   │   └── $solicitudId/
    │   │       ├── index.tsx        →  .../:id                        P06  detalle + línea de tiempo
    │   │       └── documentacion.tsx →  .../:id/documentacion         P06  carga de faltantes
    │   ├── beneficios/
    │   │   └── index.tsx            →  /portal/beneficios                  mis beneficios y vigencias
    │   ├── turnos/
    │   │   ├── index.tsx            →  /portal/turnos                      mis turnos
    │   │   └── nuevo.tsx            →  /portal/turnos/nuevo           P14  reserva de turno
    │   └── campanias/
    │       ├── index.tsx            →  /portal/campanias              P16  actividades vigentes
    │       └── $actividadId.tsx     →  /portal/campanias/:id               detalle + inscripción
    │
    └── gestion/
        ├── route.tsx                     guard nivel 2: ROL_ACTUAL distinto de CIUDADANO
        ├── index.tsx                →  /gestion                       P01  tablero operativo
        ├── indicadores.tsx          →  /gestion/indicadores           P17  tablero de indicadores
        ├── casos/
        │   ├── index.tsx            →  /gestion/casos                 P07  bandeja de casos
        │   └── $solicitudId/
        │       ├── route.tsx              layout del caso: cabecera + pestañas
        │       ├── index.tsx        →  .../:id                        P06  detalle del caso
        │       ├── evaluacion.tsx   →  .../:id/evaluacion             P08  evaluación social
        │       ├── visitas.tsx      →  .../:id/visitas                     visitas del caso
        │       └── resolucion.tsx   →  .../:id/resolucion             P10  aprobar / rechazar / espera
        ├── visitas/
        │   └── index.tsx            →  /gestion/visitas               P09  agenda de visitas
        ├── beneficios/
        │   ├── index.tsx            →  /gestion/beneficios            P10  listado
        │   └── $beneficioId.tsx     →  /gestion/beneficios/:id             renovar / suspender / finalizar
        ├── intervenciones/
        │   ├── index.tsx            →  /gestion/intervenciones        P11  planes de intervención
        │   └── $planId.tsx          →  /gestion/intervenciones/:id         objetivos y seguimientos
        ├── programas/
        │   ├── index.tsx                  →  /gestion/programas                    P12  listado
        │   ├── nuevo.tsx                  →  /gestion/programas/nuevo                   alta
        │   └── $programaId/
        │       ├── route.tsx                    layout del programa: cabecera + pestañas (datos · convocatorias · incompatibilidades)
        │       ├── index.tsx              →  .../:id                                    edición de datos del programa
        │       ├── incompatibilidades.tsx →  .../:id/incompatibilidades                 programa ↔ programa
        │       └── convocatorias/
        │           ├── index.tsx          →  .../:id/convocatorias                 P12  listado de ediciones
        │           ├── nueva.tsx          →  .../:id/convocatorias/nueva                alta de edición
        │           └── $edicionId/
        │               ├── route.tsx                layout de la edición: cabecera + pestañas (datos · requisitos · beneficios)
        │               ├── index.tsx      →  .../:edicionId                        P12  datos + abrir / suspender / cerrar
        │               ├── requisitos.tsx →  .../:edicionId/requisitos                  requisitos de la convocatoria
        │               └── beneficios.tsx →  .../:edicionId/beneficios                  beneficios de la convocatoria
        ├── centros/
        │   ├── index.tsx            →  /gestion/centros               P13  listado
        │   └── $centroId/
        │       ├── route.tsx              layout del centro
        │       ├── index.tsx        →  .../:id                             servicios y profesionales
        │       └── agenda.tsx       →  .../:id/agenda                 P13  horarios y disponibilidad
        ├── turnos/
        │   ├── index.tsx            →  /gestion/turnos                     turnos del centro
        │   ├── nuevo.tsx            →  /gestion/turnos/nuevo          P14  reserva por administrativo
        │   └── agenda.tsx           →  /gestion/turnos/agenda         P15  agenda del profesional
        ├── campanias/
        │   ├── index.tsx            →  /gestion/campanias             P16  ABM de actividades
        │   └── $actividadId/
        │       ├── index.tsx        →  .../:id                             edición
        │       └── asistencia.tsx   →  .../:id/asistencia                  inscriptos y presentismo
        └── auditoria/
            ├── route.tsx                 guard nivel 3: solo AUDITOR
            ├── index.tsx            →  /gestion/auditoria             P18  cambios registrados
            ├── eventos.tsx          →  /gestion/auditoria/eventos     P18  publicados y procesados
            └── dlq.tsx              →  /gestion/auditoria/dlq         P18  mensajes fallidos
```

**Sobre los pathless.** Solo `_auth/` y `_app/` lo son. `portal/` y `gestion/` **sí aportan segmento** —de ahí `/portal` y `/gestion`— y eso es deliberado: si fueran pathless, las dos ramas colisionarían, porque `programas/` existe en ambas apuntando a componentes distintos.

**Los `route.tsx` intermedios** (`casos/$solicitudId/`, `programas/$programaId/`, `programas/$programaId/convocatorias/$edicionId/`, `centros/$centroId/`) sirven para dos cosas: cargar la entidad una sola vez en el loader del layout y compartirla con todas las pestañas, y renderizar la cabecera con el navegador de pestañas. `auditoria/route.tsx` existe solo para poner el guard de rol una vez en vez de repetirlo en tres archivos.

**Por qué `convocatorias/` es una carpeta y no un archivo.** `ProgramRequirement` y `ProgramBenefit` cuelgan de `ProgramEdition` (`program_edition_id`), no de `Program` — un programa puede tener varias convocatorias (borrador, activa, cerradas) y cada una tiene sus propios requisitos y beneficios. Por eso `requisitos` y `beneficios` no pueden vivir como pestañas del programa: necesitan el `$edicionId` en la URL para saber de qué convocatoria se habla. `incompatibilidades`, en cambio, sí es programa↔programa (`ProgramIncompatibility`), así que queda como pestaña de `$programaId`.

**Los IDs de ruta no cambian** respecto de la notación punteada: `createFileRoute('/_app/gestion/casos/')` sigue siendo el mismo, porque el ID se arma con la ruta de carpetas incluyendo los segmentos pathless. Solo cambia dónde vive el archivo.

---

# 3. Guards de autenticación y autorización

## Los tres niveles

Cada nivel resuelve una pregunta distinta y no se pisan entre sí:

| Nivel | Archivo | Pregunta que responde | Si falla |
|---|---|---|---|
| **1. Sesión** | `_app/route.tsx` | ¿Hay una sesión válida? | Redirige a `/login` guardando el destino |
| **2. Rama** | `_app/portal/route.tsx` · `_app/gestion/route.tsx` | ¿El rol actual corresponde a esta rama? | Redirige a la rama correcta |
| **3. Rol puntual** | `route.tsx` de la carpeta, o la ruta individual | ¿Este rol específico puede entrar acá? | Redirige a `/403` |

> **Los guards son navegación, no seguridad.** Evitan que el usuario llegue a una pantalla que va a fallar o que no le sirve. **El backend revalida `ROL_ACTUAL` en cada endpoint**, según RNF-02. Un guard de router se saltea escribiendo la URL a mano o llamando la API directamente.

## Nivel 1 — Sesión

El contexto del router lleva la sesión decodificada, así que `beforeLoad` la lee sin hacer fetch.

```tsx
// src/routes/_app/route.tsx
export const Route = createFileRoute('/_app')({
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.href },   // vuelve acá después del login
      })
    }
  },
  component: AppLayout,
})
```

`redirect` en el search permite que, tras autenticar, el usuario vuelva exactamente a donde intentaba ir. Es especialmente útil con los links de notificaciones que apuntan a un caso concreto.

## Nivel 2 — Rama

```tsx
// src/routes/_app/portal/route.tsx
export const Route = createFileRoute('/_app/portal')({
  beforeLoad: ({ context }) => {
    if (context.auth.rolActual !== 'CIUDADANO') {
      throw redirect({ to: '/gestion' })
    }
  },
  component: PortalLayout,
})
```

```tsx
// src/routes/_app/gestion/route.tsx
export const Route = createFileRoute('/_app/gestion')({
  beforeLoad: ({ context }) => {
    if (context.auth.rolActual === 'CIUDADANO') {
      throw redirect({ to: '/portal' })
    }
  },
  component: GestionLayout,
})
```

Redirigir a la otra rama en vez de mostrar 403 es deliberado: no es un error de permisos, es que la persona está en el modo equivocado. El 403 se reserva para el nivel 3.

## Nivel 3 — Rol puntual

Conviene un helper para no repetir la comprobación en cada archivo:

```tsx
// src/lib/guards.ts
import { redirect } from '@tanstack/react-router'
import type { Rol, RouterContext } from '@/types'

export function requiereRol(...roles: Rol[]) {
  return ({ context }: { context: RouterContext }) => {
    if (!roles.includes(context.auth.rolActual)) {
      throw redirect({ to: '/403' })
    }
  }
}
```

```tsx
// src/routes/_app/gestion/programas/index.tsx
export const Route = createFileRoute('/_app/gestion/programas/')({
  beforeLoad: requiereRol('ADMINISTRATIVO', 'COORDINADOR'),
  component: ProgramasPage,
})
```

Cuando **toda una carpeta** comparte el mismo requisito de rol, el guard va en su `route.tsx` y no se repite en cada hija:

```tsx
// src/routes/_app/gestion/auditoria/route.tsx
export const Route = createFileRoute('/_app/gestion/auditoria')({
  beforeLoad: requiereRol('AUDITOR'),
  component: AuditoriaLayout,   // cabecera + pestañas: cambios · eventos · DLQ
})
```

Con eso, `index.tsx`, `eventos.tsx` y `dlq.tsx` quedan sin `beforeLoad` propio.

## Matriz de rol por ruta

| Ruta | Roles habilitados |
|---|---|
| `/gestion` (tablero) | Todos los municipales |
| `/gestion/casos` | Trabajador social, administrativo, coordinador, auditor (lectura) |
| `/gestion/casos/$id/evaluacion` | Trabajador social |
| `/gestion/casos/$id/resolucion` | Coordinador |
| `/gestion/visitas` | Trabajador social, coordinador |
| `/gestion/beneficios` | Administrativo, coordinador |
| `/gestion/intervenciones` | Trabajador social |
| `/gestion/programas` | Administrativo, coordinador |
| `/gestion/programas/$id/convocatorias` | Coordinador |
| `/gestion/centros` | Administrativo |
| `/gestion/turnos/agenda` | Profesional de centro |
| `/gestion/campanias` | Administrativo |
| `/gestion/indicadores` | Coordinador, auditor |
| `/gestion/auditoria` | Auditor |

El auditor entra en modo lectura a varias rutas: el guard lo deja pasar y la pantalla oculta las acciones de escritura. **El backend igual rechaza cualquier mutación suya**, así que el ocultamiento es comodidad, no control.

## Reglas que no son de rol

Dos restricciones del alcance no se resuelven con guards de router porque dependen de datos, no del rol:

- **Conflicto de interés.** Un profesional no puede evaluar, visitar ni resolver una solicitud donde él mismo es el solicitante. Se valida en el backend al cargar el caso; el frontend recibe un flag y deshabilita las acciones mostrando el motivo.
- **Transiciones de estado.** No se puede entrar a `resolucion` si la solicitud no está en evaluación o en visita. Se comprueba en el `loader` del caso y se redirige al detalle si no corresponde.

---

# 4. Cambio de rol en caliente

El cambio de rol **no es una ruta**: es una acción del footer del sidebar, disponible solo si `ROL_ASIGNADO` no es nulo.

```tsx
async function cambiarRol(nuevoRol: Rol) {
  await api.postAuthRolActual({ body: { rolActual: nuevoRol } })  // reemite el token
  await auth.refrescarSesion()   // el interceptor pasa a usar el token nuevo

  queryClient.clear()            // los datos del rol anterior ya no aplican
  await router.invalidate()      // fuerza reejecución de beforeLoad y loaders

  router.navigate({ to: nuevoRol === 'CIUDADANO' ? '/portal' : '/gestion' })
}
```

**Los pasos son obligatorios y en ese orden.** Si el interceptor no tomó el token nuevo, los loaders se disparan con el anterior y el backend devuelve el scope viejo. Si no se limpia el cache de queries, una bandeja de casos cargada como trabajadora social queda en memoria y puede repintarse en modo ciudadano. Si no se invalida el router, los `beforeLoad` no vuelven a correr y los guards evalúan contra el rol viejo. Ver sección 6 para el detalle del interceptor.

**El backend valida la invariante al reemitir:** `ROL_ACTUAL` debe pertenecer al conjunto `CIUDADANO` más `ROL_ASIGNADO`. El endpoint de cambio de rol no otorga privilegios nuevos, solo conmuta entre los que la persona ya tiene.

**Se audita:** quién, cuándo, desde qué rol y hacia cuál.

---

# 5. Filtros, búsqueda y paginación en la URL

RF-18 exige búsquedas, filtros, ordenamiento y paginación en los listados de volumen variable. Ese estado va en el search param, no en `useState`.

El schema **no se escribe a mano**: se compone a partir del que genera hey-api desde el OpenAPI del backend, extendido con lo que es puramente de interfaz.

```tsx
// src/routes/_app/gestion/casos/index.tsx
import { zGetCasosData } from '@/api/zod.gen'
import { getCasosOptions } from '@/api/@tanstack/react-query.gen'

const casosSearchSchema = zGetCasosData.shape.query.unwrap().extend({
  page: z.number().int().min(1).catch(1),   // tolerancia ante URL malformada
})

export const Route = createFileRoute('/_app/gestion/casos/')({
  validateSearch: casosSearchSchema,
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) =>
    context.queryClient.ensureQueryData(getCasosOptions({ query: deps })),
  component: BandejaCasos,
})
```

> **Por qué componer y no escribir el schema.** Si el backend agrega un estado nuevo a la solicitud, el filtro del frontend se entera al regenerar. Con enums escritos a mano la desincronización es silenciosa y aparece recién en la demostración. Los defaults tolerantes (`.catch`, `.min`) no los genera hey-api y sí los queremos, por eso se agregan en la extensión. **Nunca se editan los archivos generados.**

Ventajas concretas de mantener el estado en la URL:

- URLs compartibles entre miembros del equipo municipal.
- El botón atrás del navegador funciona como se espera.
- El criterio de aceptación *"los totales del tablero coinciden con los listados transaccionales para los mismos filtros"* se vuelve verificable: se comparan dos URLs con los mismos search params.
- `.catch(1)` en `page` evita que una URL malformada rompa la pantalla.

**Rutas que necesitan `validateSearch`:** `/portal/programas`, `/portal/solicitudes`, `/gestion/casos`, `/gestion/visitas`, `/gestion/beneficios`, `/gestion/turnos`, `/gestion/indicadores`, `/gestion/auditoria` y sus subrutas.

> **Dependencia con el backend.** Para que esto funcione, los endpoints de listado tienen que **declarar sus query params en el OpenAPI**. Si no los exponen, hey-api no genera nada aprovechable para `validateSearch` y hay que volver a escribir los filtros a mano. Conviene acordarlo con quien implemente los controladores antes de empezar los listados.

---

# 6. Capa de API generada con hey-api

Los tipos, el cliente y los schemas de Zod se generan desde el OpenAPI del backend. La estructura de rutas no cambia por esto: hey-api resuelve la capa de acceso a datos, el árbol de rutas es independiente. Lo que sí cambia es cómo se escriben los loaders, los schemas de search y la configuración de cobertura.

## Organización

```
src/api/                          ← todo generado, nunca se edita a mano
├── types.gen.ts                  tipos de request y response
├── sdk.gen.ts                    funciones por operación
├── zod.gen.ts                    schemas de validación
└── @tanstack/react-query.gen.ts  queryOptions por operación
```

```jsonc
// package.json
"scripts": {
  "api:generate": "openapi-ts"
}
```

## Separación de responsabilidades con el contexto del router

Son dos cosas distintas y conviene no mezclarlas:

| Pieza | Responsabilidad |
|---|---|
| **Cliente hey-api** | Transporte: adjunta el token en cada request mediante interceptor |
| **Contexto del router** | Autorización de navegación: `rolActual`, `rolAsignado` y `ciudadanoId` decodificados, para que los `beforeLoad` no hagan fetch |

```ts
// src/api/client.ts
client.interceptors.request.use((req) => {
  req.headers.set('Authorization', `Bearer ${auth.token}`)
  return req
})

client.interceptors.response.use((res) => {
  if (res.status === 401) router.navigate({ to: '/login' })
  if (res.status === 403) router.navigate({ to: '/403' })
  return res
})
```

El interceptor de response cubre el token expirado a mitad de sesión sin duplicar lógica en cada pantalla.

## Impacto en el cambio de rol

El orden de la sección 4 se amplía con un paso: **el interceptor tiene que tomar el token nuevo antes de invalidar el router**. Si no, los loaders se disparan con el token anterior y el backend devuelve el scope viejo.

```ts
async function cambiarRol(nuevoRol: Rol) {
  await api.postAuthRolActual({ body: { rolActual: nuevoRol } })
  await auth.refrescarSesion()      // 1. el interceptor ya usa el token nuevo
  queryClient.clear()               // 2. datos del rol anterior fuera
  await router.invalidate()         // 3. beforeLoad y loaders se reejecutan
  router.navigate({ to: nuevoRol === 'CIUDADANO' ? '/portal' : '/gestion' })
}
```

## Cobertura de pruebas

RNF-04 exige 85 % en frontend. Los archivos generados son cientos de líneas de SDK sin tests y arrastran el porcentaje hacia abajo, así que se excluyen:

```ts
// vitest.config.ts
coverage: {
  exclude: ['src/api/**', ...configDefaults.coverage.exclude],
}
```

La exclusión es defendible: es código generado a partir de un contrato, no lógica de negocio propia. Lo que **sí** se testea es todo lo que se escribe encima — los schemas compuestos (que la extensión funcione, que el `.catch` tolere una URL rota), los guards y los componentes.

## Regeneración en integración continua

Un check de CI que regenere y falle si la salida difiere de lo commiteado. Con nueve squads integrando contratos, es lo que avisa temprano que otro equipo cambió un endpoint del que dependemos.

---

# 7. Sidebar

Cada layout de rama arma su propio menú, así que el sidebar cambia entero al conmutar de rol.

**Portal (ciudadano)** — Inicio · Programas · Mis solicitudes · Mis beneficios · Mis turnos · Campañas

**Gestión (municipal)** — depende del rol:

| Rol | Ítems del sidebar |
|---|---|
| Trabajador social | Inicio, Casos, Visitas, Intervenciones |
| Administrativo | Inicio, Casos, Beneficios, Programas, Centros, Turnos, Campañas |
| Coordinador | Inicio, Casos, Visitas, Beneficios, Programas, Indicadores |
| Profesional de centro | Inicio, Mi agenda |
| Auditor | Inicio, Casos (lectura), Indicadores, Auditoría |

Los ítems se derivan de la misma matriz de roles de la sección 3, para no mantener dos listas que puedan divergir. Si un ítem no está en el sidebar, su ruta tampoco es accesible.

El footer del sidebar muestra el nombre, el rol actual y el conmutador de rol cuando corresponde.

---

# 8. Detalles de implementación

## Contexto del router

La sesión se inyecta al crear el router para que los `beforeLoad` la lean sin fetch:

```tsx
export interface RouterContext {
  auth: {
    isAuthenticated: boolean
    ciudadanoId: string
    rolAsignado: Rol | null
    rolActual: Rol
  }
  queryClient: QueryClient
}
```

## Estados de interfaz

RNF-05 exige estados de carga, vacío, éxito y error. TanStack Router los cubre a nivel ruta:

- `pendingComponent` — skeleton mientras el loader resuelve.
- `errorComponent` — error de carga, con acción de reintentar.
- `notFoundComponent` — para rutas con parámetro (`$solicitudId` inexistente).
- El estado vacío se maneja dentro del componente, con acción de limpiar filtros.

## Resiliencia visible

RNF-08 pide que la indisponibilidad de otro módulo no impida registrar operaciones locales. En el detalle de solicitud, cuando la validación contra Ciudadanos quedó pendiente, se muestra un aviso persistente que aclara que el trámite sigue su curso y se reintentará. No es un error: es un estado intermedio legítimo.

## Rutas fuera del alcance

Deliberadamente **no** existen: gestión de usuarios (la identidad vive en Ciudadanos), configuración de notificaciones (propiedad del Core), exportaciones, mapas de cobertura ni pantallas de historia clínica.

---

# 9. Trazabilidad con el documento de alcance

| Pantalla | Ruta | Bloque funcional |
|---|---|---|
| P01 Tablero personal | `/portal` y `/gestion` | Transversal |
| P02 Catálogo de programas | `/portal/programas` | 1 |
| P03 Detalle de programa | `/portal/programas/$id` | 1 |
| P04 Nueva solicitud | `/portal/solicitudes/nueva` | 2 |
| P05 Mis solicitudes | `/portal/solicitudes` | 2 |
| P06 Detalle de solicitud | `/portal/solicitudes/$id` · `/gestion/casos/$id` | 2 |
| P07 Bandeja de casos | `/gestion/casos` | 2 |
| P08 Evaluación social | `/gestion/casos/$id/evaluacion` | 3 |
| P09 Agenda de visitas | `/gestion/visitas` | 3 |
| P10 Gestión de beneficios | `/gestion/beneficios` · `/gestion/casos/$id/resolucion` | 3 |
| P11 Plan de intervención | `/gestion/intervenciones` | 3 |
| P12 Programas y convocatorias | `/gestion/programas` | 1 |
| P13 Centros y agendas | `/gestion/centros` | 4 |
| P14 Reserva de turno | `/portal/turnos/nuevo` · `/gestion/turnos/nuevo` | 4 |
| P15 Agenda del profesional | `/gestion/turnos/agenda` | 4 |
| P16 Campañas y actividades | `/portal/campanias` · `/gestion/campanias` | 5 |
| P17 Indicadores | `/gestion/indicadores` | 5 |
| P18 Auditoría y eventos | `/gestion/auditoria` | Transversal |