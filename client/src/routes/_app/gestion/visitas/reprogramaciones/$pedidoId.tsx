import { createFileRoute } from "@tanstack/react-router"

import { RescheduleRequestDetail } from "@/components/applications/SocialRescheduleInbox"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"

export const Route = createFileRoute("/_app/gestion/visitas/reprogramaciones/$pedidoId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { pedidoId } = Route.useParams()
  const { user } = Route.useRouteContext()
  const permissions = user.permissions ?? []

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[
          { label: "Visitas y entrevistas", to: "/gestion/visitas" },
          { label: "Pedidos de reprogramación", to: "/gestion/visitas/reprogramaciones" },
          { label: "Pedido" },
        ]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full min-w-0 max-w-4xl p-4 lg:p-6">
            {permissions.includes("applications:visits:reschedule-requests:view")
              ? <RescheduleRequestDetail id={pedidoId} canReview={permissions.includes("applications:visits:reschedule-requests:review")} currentUserId={user.id} showVisitLink />
              : <p role="alert">Tu rol no tiene permiso para consultar pedidos de reprogramación.</p>}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
