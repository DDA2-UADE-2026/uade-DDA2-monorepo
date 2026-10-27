import { useLocation } from "@tanstack/react-router"

import { AppSidebarHeader } from "@/components/layout/AppSidebarHeader"
import { PORTAL_NAV } from "@/components/layout/sidebar-constants"
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

function PortalSidebar() {
  const { pathname } = useLocation()
  const { data } = useMe()

  return (
    <Sidebar variant="sidebar" collapsible="icon">
      <AppSidebarHeader />
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Portal ciudadano</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarNavMenu permissions={data?.user?.permissions ?? []} items={PORTAL_NAV} pathname={pathname} />
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

export { PortalSidebar }
