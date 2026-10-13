import { createFileRoute, redirect } from "@tanstack/react-router"
import { CitizenActivityDetail } from "@/components/activities/CitizenActivityDetail"

export const Route = createFileRoute("/_app/portal/actividades/$actividadId")({
  beforeLoad: ({ context }) => {
    if (!context.user.permissions?.includes("activities:own:view")) throw redirect({ to: "/403", replace: true })
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { actividadId } = Route.useParams()
  const { user } = Route.useRouteContext()
  return <CitizenActivityDetail activityId={actividadId} canEnroll={user.permissions?.includes("activities:own:enroll") ?? false} />
}
