import {
  IconApi,
  IconBuilding,
  IconCalendarEvent,
  IconChartBar,
  IconClipboardHeart,
  IconFileCheck,
  IconFileText,
  IconGift,
  IconHeartHandshake,
  IconHome2,
  IconHomeCheck,
  IconLibrary,
  IconNetwork,
  IconRoute,
  IconShieldCheck,
  IconShieldLock,
  IconSpeakerphone,
  IconTimelineEventText,
  IconUsers,
} from "@tabler/icons-react"

import type { SidebarNavItem } from "@/components/layout/SidebarNavMenu"

// Ítems y submenús según notes/routes.md #2 y #7 — solo se agregan submenús donde
// el árbol tiene páginas hijas estáticas (no para detalles dinámicos $id).
// Rutas todavía sin implementar como Route: anchors simples hasta que existan.
export const PORTAL_NAV: readonly SidebarNavItem[] = [
  { title: "Inicio", url: "/portal", icon: IconHome2 },
  { title: "Programas", url: "/portal/programas", icon: IconHeartHandshake },
  {
    title: "Mis solicitudes",
    url: "/portal/solicitudes",
    icon: IconFileText,
  },
  { title: "Mis beneficios", url: "/portal/beneficios", icon: IconGift },
  {
    title: "Mis turnos",
    url: "/portal/turnos",
    icon: IconCalendarEvent,
  },
  { title: "Campañas", url: "/portal/campanias", icon: IconSpeakerphone },
]

// Superset de ítems de las 5 matrices de rol (ver notes/routes.md #7) — falta filtrar por
// ROL_ACTUAL una vez que la sesión real esté disponible. Submenús solo donde el árbol
// tiene páginas hijas estáticas (no para detalles dinámicos $id).
// Rutas todavía sin implementar como Route: anchors simples hasta que existan.
export const OPERACION_NAV: readonly SidebarNavItem[] = [
  { title: "Inicio", url: "/gestion", icon: IconHome2 },
  { 
    title: "Solicitudes",
    url: "/gestion/solicitudes",
    icon: IconFileText,
    items: [
      { title: "Solicitud asistida", url: "/gestion/solicitudes/asistida", icon: IconFileCheck },
    ]
  },
  { title: "Visitas", url: "/gestion/visitas", icon: IconHomeCheck },
  { title: "Beneficios", url: "/gestion/beneficios", icon: IconGift },
  { title: "Intervenciones", url: "/gestion/intervenciones", icon: IconClipboardHeart },
  {
    title: "Programas",
    url: "/gestion/programas",
    icon: IconHeartHandshake,
  },
  { 
    title: "Centros",
    url: "/gestion/centros",
    icon: IconBuilding,
    items: [
      { title: "Servicios ofrecidos", url: "/gestion/centros/servicios", icon: IconLibrary },
    ]
  },
  {
    title: "Turnos",
    url: "/gestion/turnos",
    icon: IconCalendarEvent,
  },
  { title: "Campañas", url: "/gestion/campanias", icon: IconSpeakerphone },
]

export const ANALISIS_NAV: readonly SidebarNavItem[] = [
  { title: "Usuarios", url: "/gestion/usuarios", icon: IconUsers },
  { title: "Roles y permisos", url: "/gestion/roles", icon: IconShieldLock },
  { title: "Indicadores", url: "/gestion/indicadores", icon: IconChartBar },
  { title: "Auditoría", url: "/gestion/auditoria", icon: IconShieldCheck },
  { title: "Eventos", url: "/gestion/eventos", icon: IconTimelineEventText },
]

export const DEBUG_NAV: readonly SidebarNavItem[] = [
  { title: "API local (Autogen)", url: "/gestion/debug/documentacion-local", icon: IconApi },
  { title: "Eventos Async (Autogen)", url: "/gestion/debug/documentacion-eventos", icon: IconRoute },
  { title: "Estado de red", url: "/gestion/debug/estado-red", icon: IconNetwork },
]
