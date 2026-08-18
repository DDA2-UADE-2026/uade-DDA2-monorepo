import { createFileRoute, Outlet } from "@tanstack/react-router"

import { GestionSidebar } from "@/components/layout/GestionSidebar"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"

export const Route = createFileRoute("/_app/gestion")({
  component: GestionLayout,
})

function GestionLayout() {
  return (
    <SidebarProvider>
      <GestionSidebar />
      <SidebarInset>
        <Outlet />
      </SidebarInset>
    </SidebarProvider>
  )
}
