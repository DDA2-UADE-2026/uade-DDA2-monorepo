import { createFileRoute } from "@tanstack/react-router"

import {
  OutletNavContent,
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"

export const Route = createFileRoute("/_app/gestion/usuarios/$usuarioId")({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavContent>
          Title
        </OutletNavContent>
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
