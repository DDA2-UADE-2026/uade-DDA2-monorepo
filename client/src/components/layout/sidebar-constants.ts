import {
  IconApi,
  IconBuilding,
  IconCalendarEvent,
  IconCalendarUser,
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
    requiredPermissions: ["applications:own:view"],
  },
  {
    title: "Mis visitas",
    url: "/portal/visitas",
    icon: IconHomeCheck,
    requiredPermissions: ["applications:own:visits:view"],
  },
  { title: "Mis beneficios", url: "/portal/beneficios", icon: IconGift, requiredPermissions: ["applications:own:view"] },
  { title: "Mis intervenciones", url: "/portal/intervenciones", icon: IconClipboardHeart, requiredPermissions: ["applications:own:interventions:view"] },
  {
    title: "Mis turnos",
    url: "/portal/turnos",
    icon: IconCalendarEvent,
    requiredPermissions: ["appointments:own:view"],
  },
  { title: "Actividades", url: "/portal/actividades", icon: IconSpeakerphone, requiredPermissions: ["activities:own:view"] },
]

// Superset de ítems de las 5 matrices de rol (ver notes/routes.md #7) — falta filtrar por
// ROL_ACTUAL una vez que la sesión real esté disponible. Submenús solo donde el árbol
// tiene páginas hijas estáticas (no para detalles dinámicos $id).
// Rutas todavía sin implementar como Route: anchors simples hasta que existan.
export const OPERACION_NAV: readonly SidebarNavItem[] = [
  { title: "Inicio", url: "/gestion", icon: IconHome2 },
  {
    title: "Centros",
    url: "/gestion/centros",
    icon: IconBuilding,
    requiredPermissions: ["centers:management:view"],
    items: [
      { title: "Servicios ofrecidos", url: "/gestion/centros/servicios", icon: IconLibrary, requiredPermissions: ["services:management:view"] },
    ]
  },
  {
    title: "Programas",
    url: "/gestion/programas",
    icon: IconHeartHandshake,
    requiredPermissions: ["programs:management:view"],
    items: [
      { title: "Solicitudes", url: "/gestion/solicitudes", icon: IconFileText, requiredPermissions: ["applications:management:view"] },
      { title: "Solicitud asistida", url: "/gestion/solicitudes/asistida", icon: IconFileCheck, requiredPermissions: ["applications:management:view"] },
      { title: "Beneficios", url: "/gestion/beneficios", icon: IconGift, requiredPermissions: ["programs:management:view"] },
    ]
  },
  { 
    title: "Evaluaciones",
    url: "/gestion/casos",
    icon: IconClipboardHeart,
    requiredPermissions: ["applications:evaluations:view"],
    items: [
      { title: "Visitas", url: "/gestion/visitas", icon: IconHomeCheck, requiredPermissions: ["applications:visits:view", "applications:visits:reschedule-requests:view"] },
      { title: "Intervenciones", url: "/gestion/intervenciones", icon: IconClipboardHeart, requiredPermissions: ["applications:interventions:view", "applications:interventions:create", "applications:interventions:follow-ups:create", "applications:interventions:follow-ups:publish"] },
    ]
  },
  {
    title: "Actividades",
    url: "/gestion/actividades",
    icon: IconSpeakerphone,
    requiredPermissions: ["activities:management:view", "activities:attendance:view"],
    items: [
      { title: "Turnos", url: "/gestion/turnos", icon: IconCalendarEvent, requiredPermissions: ["appointments:management:view"] },
      { title: "Turnos asignados", url: "/gestion/mis-turnos", icon: IconCalendarUser, requiredPermissions: ["appointments:professional:view"] },
    ],
  },
]

export const ANALISIS_NAV: readonly SidebarNavItem[] = [
  { title: "Usuarios", url: "/gestion/usuarios", icon: IconUsers, requiredPermissions: ["users:view"] },
  { title: "Roles y permisos", url: "/gestion/roles", icon: IconShieldLock, requiredPermissions: ["roles:view"] },
  { title: "Indicadores", url: "/gestion/indicadores", icon: IconChartBar, requiredPermissions: ["indicators:view"] },
  { title: "Auditoría", url: "/gestion/auditoria", icon: IconShieldCheck, requiredPermissions: ["logs:view"] },
  { title: "Eventos", url: "/gestion/auditoria/eventos", icon: IconTimelineEventText, requiredPermissions: ["logs:view"] },
]

export const DEBUG_NAV: readonly SidebarNavItem[] = [
  { title: "API local (Autogen)", url: "/gestion/debug/documentacion-local", icon: IconApi },
  { title: "Eventos Async (Autogen)", url: "/gestion/debug/documentacion-eventos", icon: IconRoute },
  { title: "Estado de red", url: "/gestion/debug/estado-red", icon: IconNetwork },
]
