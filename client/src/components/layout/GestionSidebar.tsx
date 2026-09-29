import { useLocation } from "@tanstack/react-router"

import { AppSidebarHeader } from "@/components/layout/AppSidebarHeader"
import { KeyboardNotice } from "@/components/layout/KeyboardNotice"
import { ANALISIS_NAV, DEBUG_NAV, OPERACION_NAV, PROFESSIONAL_APPOINTMENTS_NAV } from "@/components/layout/sidebar-constants"
import { SidebarNavMenu } from "@/components/layout/SidebarNavMenu"
import { SidebarUser } from "@/components/layout/SidebarUser"
import { useMe } from "@/hooks/use-auth"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
} from "@/components/ui/sidebar"
import { BackendStatusPill } from "../BackendStatusPill"

function GestionSidebar() {
  const { pathname } = useLocation()
  const { data } = useMe()
  const professional = data?.user?.activeRole?.toUpperCase() === "PROFESIONAL_CENTRO"
  const operationItems = professional ? [...OPERACION_NAV, PROFESSIONAL_APPOINTMENTS_NAV] : OPERACION_NAV

  return (
    <Sidebar variant="sidebar" collapsible="icon">
      <AppSidebarHeader />
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Gestión municipal</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu items={operationItems} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel>Seguridad y Análisis</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu items={ANALISIS_NAV} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel className="flex gap-1.5 items-center justify-between">Debug y estado <BackendStatusPill xs/></SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu items={DEBUG_NAV} pathname={pathname} />
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
