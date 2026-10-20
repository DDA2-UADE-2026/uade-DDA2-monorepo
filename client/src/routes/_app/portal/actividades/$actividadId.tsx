import { createFileRoute } from "@tanstack/react-router"
import { CitizenActivityDetail } from "@/components/activities/CitizenActivityDetail"

export const Route = createFileRoute("/_app/portal/actividades/$actividadId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { actividadId } = Route.useParams()
  const { user } = Route.useRouteContext()
  return <CitizenActivityDetail activityId={actividadId} canEnroll={user.permissions?.includes("activities:own:enroll") ?? false} />
}
