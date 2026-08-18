import { createFileRoute, Outlet } from "@tanstack/react-router"

import { PortalSidebar } from "@/components/layout/PortalSidebar"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"

export const Route = createFileRoute("/_app/portal")({
  component: PortalLayout,
})

function PortalLayout() {
  return (
    <SidebarProvider>
      <PortalSidebar />
      <SidebarInset>
        <Outlet />
      </SidebarInset>
    </SidebarProvider>
  )
}
