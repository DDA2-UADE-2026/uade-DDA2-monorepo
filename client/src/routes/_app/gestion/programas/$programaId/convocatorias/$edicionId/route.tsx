import { useQuery } from "@tanstack/react-query"
import { createFileRoute, Outlet } from "@tanstack/react-router"
import { RouteTabs } from "@/components/programs/ProgramRouteUi"
import { findById3Options } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/convocatorias/$edicionId",
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId, edicionId } = Route.useParams()
  const edition = useQuery(findById3Options({ path: { id: edicionId } }))
  const base = `/gestion/programas/${programaId}/convocatorias/${edicionId}`
  return <div><div className="border-b px-4 py-3 lg:px-6"><p className="text-xs text-muted-foreground">Convocatoria</p><h2 className="font-semibold">{edition.data?.name ?? "Cargando…"}</h2></div><RouteTabs items={[{ label: "Datos", to: base }, { label: "Requisitos", to: `${base}/requisitos` }, { label: "Beneficios", to: `${base}/beneficios` }]} /><Outlet /></div>
}
