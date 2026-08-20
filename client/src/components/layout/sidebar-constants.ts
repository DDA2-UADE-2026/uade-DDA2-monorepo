import {
  IconApi,
  IconBuilding,
  IconCalendarEvent,
  IconChartBar,
  IconClipboardHeart,
  IconFileText,
  IconFolders,
  IconGift,
  IconHeartHandshake,
  IconHome2,
  IconHomeCheck,
  IconNetwork,
  IconRoute,
  IconShieldCheck,
  IconSpeakerphone,
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
    items: [{ title: "Nueva solicitud", url: "/portal/solicitudes/nueva" }],
  },
  { title: "Mis beneficios", url: "/portal/beneficios", icon: IconGift },
  {
    title: "Mis turnos",
    url: "/portal/turnos",
    icon: IconCalendarEvent,
    items: [{ title: "Reservar turno", url: "/portal/turnos/nuevo" }],
  },
  { title: "Campañas", url: "/portal/campanias", icon: IconSpeakerphone },
]

// Superset de ítems de las 5 matrices de rol (ver notes/routes.md #7) — falta filtrar por
// ROL_ACTUAL una vez que la sesión real esté disponible. Submenús solo donde el árbol
// tiene páginas hijas estáticas (no para detalles dinámicos $id).
// Rutas todavía sin implementar como Route: anchors simples hasta que existan.
export const OPERACION_NAV: readonly SidebarNavItem[] = [
  { title: "Inicio", url: "/gestion", icon: IconHome2 },
  { title: "Casos", url: "/gestion/casos", icon: IconFolders },
  { title: "Visitas", url: "/gestion/visitas", icon: IconHomeCheck },
  { title: "Beneficios", url: "/gestion/beneficios", icon: IconGift },
  { title: "Intervenciones", url: "/gestion/intervenciones", icon: IconClipboardHeart },
  {
    title: "Programas",
    url: "/gestion/programas",
    icon: IconHeartHandshake,
    items: [{ title: "Nuevo programa", url: "/gestion/programas/nuevo" }],
  },
  { title: "Centros", url: "/gestion/centros", icon: IconBuilding },
  {
    title: "Turnos",
    url: "/gestion/turnos",
    icon: IconCalendarEvent,
    items: [
      { title: "Nuevo turno", url: "/gestion/turnos/nuevo" },
      { title: "Mi agenda", url: "/gestion/turnos/agenda" },
    ],
  },
  { title: "Campañas", url: "/gestion/campanias", icon: IconSpeakerphone },
]

export const ANALISIS_NAV: readonly SidebarNavItem[] = [
  { title: "Indicadores", url: "/gestion/indicadores", icon: IconChartBar },
  {
    title: "Auditoría",
    url: "/gestion/auditoria",
    icon: IconShieldCheck,
    items: [
      { title: "Cambios", url: "/gestion/auditoria" },
      { title: "Eventos", url: "/gestion/auditoria/eventos" },
      { title: "DLQ", url: "/gestion/auditoria/dlq" },
    ],
  },
]

export const DEBUG_NAV: readonly SidebarNavItem[] = [
  { title: "Documentación local", url: "/gestion/debug/documentacion-local", icon: IconApi },
  { title: "Documentación eventos", url: "/gestion/debug/documentacion-eventos", icon: IconRoute },
  { title: "Estado de red", url: "/gestion/debug/estado-red", icon: IconNetwork },
]
