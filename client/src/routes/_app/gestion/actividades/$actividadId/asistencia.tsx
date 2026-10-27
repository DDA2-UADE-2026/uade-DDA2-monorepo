import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { ActivityAttendance } from "@/components/activities/ActivityAttendance"
import { activitySearchSchema } from "@/components/activities/activity-ui"

export const Route = createFileRoute(
  "/_app/gestion/actividades/$actividadId/asistencia",
)({
  validateSearch: activitySearchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { actividadId } = Route.useParams()
  const { page } = Route.useSearch()
  const { user } = Route.useRouteContext()
  const navigate = useNavigate({ from: Route.fullPath })
  return <ActivityAttendance activityId={actividadId} page={page} onPageChange={(nextPage) => navigate({ search: { page: nextPage } })} canManage={user.permissions?.includes("activities:attendance:manage") ?? false} />
}
