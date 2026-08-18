import { useLocation } from "@tanstack/react-router"
import {
  IconCalendarEvent,
  IconFileText,
  IconGift,
  IconHeartHandshake,
  IconHome2,
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

// Ítems y submenús según notes/routes.md #2 y #7 — solo se agregan submenús donde
// el árbol tiene páginas hijas estáticas (no para detalles dinámicos $id).
// Rutas todavía sin implementar como Route: anchors simples hasta que existan.
const PORTAL_NAV: readonly SidebarNavItem[] = [
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

function PortalSidebar() {
  const { pathname } = useLocation()

  return (
    <Sidebar variant="sidebar" collapsible="icon">
      <AppSidebarHeader />
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Portal ciudadano</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu items={PORTAL_NAV} pathname={pathname} />
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

export { PortalSidebar }
