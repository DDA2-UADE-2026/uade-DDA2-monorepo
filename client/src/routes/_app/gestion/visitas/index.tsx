import { Navigate, createFileRoute } from "@tanstack/react-router"

import { SocialVisitsBoard } from "@/components/applications/SocialVisitsBoard"
import { VisitsSectionLayout } from "@/components/applications/VisitsSectionLayout"

export const Route = createFileRoute("/_app/gestion/visitas/")({
  component: RouteComponent,
})

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const permissions = user.permissions ?? []
  if (!permissions.includes("applications:visits:view") && permissions.includes("applications:visits:reschedule-requests:view")) {
    return <Navigate to="/gestion/visitas/reprogramaciones" search={{ page: 1, estado: "PENDING" }} replace />
  }
  return <VisitsSectionLayout tab="agenda" permissions={permissions}>
    <SocialVisitsBoard permissions={permissions} currentUserId={user.id} />
  </VisitsSectionLayout>
}
