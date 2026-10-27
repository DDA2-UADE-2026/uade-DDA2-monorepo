import { createFileRoute } from "@tanstack/react-router"

import { VisitDetail } from "@/components/applications/VisitDetail"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"

export const Route = createFileRoute("/_app/gestion/visitas/$visitaId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { visitaId } = Route.useParams()
  const { user } = Route.useRouteContext()
  const permissions = user.permissions ?? []

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Visitas y entrevistas", to: "/gestion/visitas" }, { label: "Actuación" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full min-w-0 max-w-6xl p-4 lg:p-6">
            {permissions.includes("applications:visits:view")
              ? <VisitDetail id={visitaId} permissions={permissions} currentUserId={user.id} />
              : <p role="alert">Tu rol no tiene permiso para consultar visitas.</p>}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
