import { Link, createFileRoute } from "@tanstack/react-router"
import { SocialVisitsBoard } from "@/components/applications/SocialVisitsBoard"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { Button } from "@/components/ui/button"
import { useQuery } from "@tanstack/react-query"
import { get3Options } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute(
  "/_app/gestion/casos/$solicitudId/visitas",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useParams()
  const { user } = Route.useRouteContext()
  const application = useQuery(get3Options({ path: { id: solicitudId } }))
  const canSchedule = !!application.data && ["IN_EVALUATION", "IN_VISIT"].includes(application.data.status ?? "") && application.data.assignedWorkerUserId === user.id
  return <SidebarShell><OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={[{ label: "Solicitudes", to: "/gestion/solicitudes" }, { label: "Visitas" }]} /></OutletNavSticky><SidebarShellContent><div className="min-h-0 flex-1 overflow-auto p-4 lg:p-6"><div className="mx-auto max-w-5xl space-y-5"><div><h1 className="font-heading text-2xl font-medium">Visitas y entrevistas de la solicitud</h1><div className="mt-2 flex gap-2"><Button size="sm" variant="outline" render={<Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId }} />}>Ver solicitud</Button><Button size="sm" variant="outline" render={<Link to="/gestion/casos/$solicitudId/evaluacion" params={{ solicitudId }} />}>Ver evaluación</Button><Button size="sm" variant="outline" render={<Link to="/gestion/casos/$solicitudId/resolucion" params={{ solicitudId }} />}>Resolución</Button></div></div>{application.isError && <p role="alert">No se pudo consultar la solicitud para programar una actuación.</p>}<SocialVisitsBoard applicationId={solicitudId} permissions={user.permissions ?? []} currentUserId={user.id} canSchedule={canSchedule} /></div></div></SidebarShellContent></SidebarShell>
}
