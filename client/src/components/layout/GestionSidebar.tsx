import { useLocation } from "@tanstack/react-router"
import {
  IconBuilding,
  IconCalendarEvent,
  IconChartBar,
  IconClipboardHeart,
  IconFolders,
  IconGift,
  IconHeartHandshake,
  IconHome2,
  IconHomeCheck,
  IconShieldCheck,
  IconSpeakerphone,
} from "@tabler/icons-react"

import { AppSidebarHeader } from "@/components/layout/AppSidebarHeader"
import { KeyboardNotice } from "@/components/layout/KeyboardNotice"
import { SidebarNavMenu, type SidebarNavItem } from "@/components/layout/SidebarNavMenu"
import { SidebarUser } from "@/components/layout/SidebarUser"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
} from "@/components/ui/sidebar"

// Superset de ítems de las 5 matrices de rol (ver notes/routes.md #7) — falta filtrar por
// ROL_ACTUAL una vez que la sesión real esté disponible. Submenús solo donde el árbol
// tiene páginas hijas estáticas (no para detalles dinámicos $id).
// Rutas todavía sin implementar como Route: anchors simples hasta que existan.
const OPERACION_NAV: readonly SidebarNavItem[] = [
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
      { title: "Turnos del centro", url: "/gestion/turnos" },
      { title: "Nuevo turno", url: "/gestion/turnos/nuevo" },
      { title: "Mi agenda", url: "/gestion/turnos/agenda" },
    ],
  },
  { title: "Campañas", url: "/gestion/campanias", icon: IconSpeakerphone },
]

const ANALISIS_NAV: readonly SidebarNavItem[] = [
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

function GestionSidebar() {
  const { pathname } = useLocation()

  return (
    <Sidebar variant="sidebar" collapsible="icon">
      <AppSidebarHeader />
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Gestión municipal</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu items={OPERACION_NAV} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel>Análisis</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu items={ANALISIS_NAV} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

        <KeyboardNotice />
      </SidebarContent>
      <SidebarFooter>
        <SidebarMenu>
          <SidebarUser />
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  )
}

export { GestionSidebar }
