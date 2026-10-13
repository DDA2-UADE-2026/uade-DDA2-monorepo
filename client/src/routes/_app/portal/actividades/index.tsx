import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { CitizenActivities } from "@/components/activities/CitizenActivities"
import { citizenActivitySearchSchema } from "@/components/activities/activity-ui"

export const Route = createFileRoute("/_app/portal/actividades/")({
  validateSearch: citizenActivitySearchSchema,
  beforeLoad: ({ context }) => {
    if (!context.user.permissions?.includes("activities:own:view")) throw redirect({ to: "/403", replace: true })
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { page, vista } = Route.useSearch()
  const { user } = Route.useRouteContext()
  const navigate = useNavigate({ from: Route.fullPath })
  return <CitizenActivities page={page} view={vista} canEnroll={user.permissions?.includes("activities:own:enroll") ?? false} onSearchChange={(search) => navigate({ search })} />
}
