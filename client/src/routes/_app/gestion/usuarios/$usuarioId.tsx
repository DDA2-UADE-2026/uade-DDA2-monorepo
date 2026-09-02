import { createFileRoute } from "@tanstack/react-router"

import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"

export const Route = createFileRoute("/_app/gestion/usuarios/$usuarioId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { usuarioId } = Route.useParams()
  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[
          { label: "Usuarios", to: "/gestion/usuarios" },
          { label: `Usuario ${usuarioId}` },
        ]} />
        <OutletNavRightButton className="gap-1.5">
          action
        </OutletNavRightButton>
      </OutletNavSticky>
      <SidebarShellContent>
        Content
      </SidebarShellContent>
    </SidebarShell>
  )
}
