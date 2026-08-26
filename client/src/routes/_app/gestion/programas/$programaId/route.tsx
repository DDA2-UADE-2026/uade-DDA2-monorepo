import { useQuery } from "@tanstack/react-query"
import { createFileRoute, Outlet } from "@tanstack/react-router"
import { RouteTabs } from "@/components/programs/ProgramRouteUi"
import { OutletNavContent, OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { findById2Options } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute("/_app/gestion/programas/$programaId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const program = useQuery(findById2Options({ path: { id: programaId } }))
  const base = `/gestion/programas/${programaId}`
  return <SidebarShell>
    <OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavContent>{program.data?.name ?? "Programa"}</OutletNavContent></OutletNavSticky>
    <SidebarShellContent>
      <RouteTabs items={[{ label: "Datos", to: base }, { label: "Convocatorias", to: `${base}/convocatorias` }, { label: "Incompatibilidades", to: `${base}/incompatibilidades` }]} />
      <div className="min-h-0 flex-1 overflow-y-auto"><Outlet /></div>
    </SidebarShellContent>
  </SidebarShell>
}
