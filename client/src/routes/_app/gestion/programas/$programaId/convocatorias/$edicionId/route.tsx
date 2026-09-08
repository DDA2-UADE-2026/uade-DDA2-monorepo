import { createFileRoute, Outlet } from "@tanstack/react-router"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId",
)({
  component: RouteComponent,
})

function RouteComponent() {
  return <Outlet />
}
