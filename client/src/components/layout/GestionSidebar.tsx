import { useLocation } from "@tanstack/react-router"

import { AppSidebarHeader } from "@/components/layout/AppSidebarHeader"
import { ANALISIS_NAV, DEBUG_NAV, OPERACION_NAV } from "@/components/layout/sidebar-constants"
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
  const activityPermissions = data?.user?.permissions ?? []

  return (
    <Sidebar variant="sidebar" collapsible="icon">
      <AppSidebarHeader />
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Gestión municipal</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu permissions={activityPermissions} items={OPERACION_NAV} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel>Seguridad y Análisis</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu permissions={activityPermissions} items={ANALISIS_NAV} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel className="flex gap-1.5 items-center justify-between">Debug y estado <BackendStatusPill xs/></SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu permissions={activityPermissions} items={DEBUG_NAV} pathname={pathname} />
          </SidebarGroupContent>
        </SidebarGroup>

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
