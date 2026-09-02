import { useQuery } from "@tanstack/react-query"
import { createFileRoute, Outlet, useParams } from "@tanstack/react-router"

import {
  OutletNavRightButton,
  OutletNavSidebarTrigger,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent,
} from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { RouteTabs, statusLabels } from "@/components/programs/ProgramRouteUi"
import { Badge } from "@/components/ui/badge"
import { findById2Options, findById3Options } from "@/generated/@tanstack/react-query.gen"

export const Route = createFileRoute("/_app/gestion/programas/$programaId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { programaId } = Route.useParams()
  const edicionId = useParams({
    strict: false,
    select: (params) => params.edicionId,
  })
  const program = useQuery(findById2Options({ path: { id: programaId } }))
  const edition = useQuery({
    ...findById3Options({ path: { id: edicionId ?? "" } }),
    enabled: Boolean(edicionId),
  })
  const base = `/gestion/programas/${programaId}`
  const editionBase = `${base}/convocatorias/${edicionId}`
  const programTabs = [
    { label: "Datos", to: base },
    { label: "Convocatorias", to: `${base}/convocatorias` },
    { label: "Incompatibilidades", to: `${base}/incompatibilidades` },
  ]
  const editionTabs = [
    { label: "Datos", to: editionBase },
    { label: "Períodos de inscripción", to: `${editionBase}/periodos-inscripcion` },
    { label: "Requisitos", to: `${editionBase}/requisitos` },
    { label: "Beneficios", to: `${editionBase}/beneficios` },
  ]

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={edicionId ? [
          { label: "Programas", to: "/gestion/programas" },
          { label: program.data?.name ?? "Programa", to: base },
          { label: "Convocatorias", to: `${base}/convocatorias` },
          { label: edition.data?.name ?? "Convocatoria" },
        ] : [
          { label: "Programas", to: "/gestion/programas" },
          { label: program.data?.name ?? "Programa" },
        ]} />
        {edicionId && edition.data?.status && (
          <OutletNavRightButton>
            <Badge variant={edition.data.status === "CLOSED" ? "destructive" : "outline"}>
              {statusLabels[edition.data.status]}
            </Badge>
          </OutletNavRightButton>
        )}
      </OutletNavSticky>
      <SidebarShellContent>
        <RouteTabs items={edicionId ? editionTabs : programTabs} />
        <div className="min-h-0 flex-1 overflow-y-auto"><Outlet /></div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
