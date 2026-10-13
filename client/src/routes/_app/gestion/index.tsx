import { createFileRoute } from "@tanstack/react-router"

import { GestionDashboard } from "@/components/gestion/GestionDashboard"
import {
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"

export const Route = createFileRoute("/_app/gestion/")({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Inicio" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <GestionDashboard />
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
