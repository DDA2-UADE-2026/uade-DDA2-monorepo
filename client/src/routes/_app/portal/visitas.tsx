import { IconCalendarEvent, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { CitizenVisitCard, CitizenVisitDialog, CitizenVisitError } from "@/components/applications/CitizenVisitUi"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { citizenVisitError, citizenVisitQueries, type CitizenVisit } from "@/lib/citizen-visits"

const PAGE_SIZE = 10
const VIEWS = ["proximas", "todas"] as const
type VisitView = (typeof VIEWS)[number]
const VIEW_LABELS: Record<VisitView, string> = { proximas: "Próximas", todas: "Todas" }

const searchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
  vista: z.enum(VIEWS).catch("proximas").default("proximas"),
})

export const Route = createFileRoute("/_app/portal/visitas")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page, vista } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [selected, setSelected] = useState<CitizenVisit | null>(null)
  const visits = useQuery({
    ...citizenVisitQueries.listAll(vista === "proximas", page - 1, PAGE_SIZE),
    placeholderData: keepPreviousData,
  })

  return <ApplicationPage breadcrumbs={[{ label: "Mis visitas" }]}>
    <ApplicationHeading
      title="Mis visitas y entrevistas"
      description="Las actuaciones programadas para todas tus solicitudes. Abrí una para ver el detalle o pedir una reprogramación."
      action={<Button variant="outline" size="sm" disabled={visits.isFetching} onClick={() => void visits.refetch()}><IconRefresh className={visits.isFetching ? "animate-spin" : undefined} /><span className="sr-only sm:not-sr-only">Refrescar</span></Button>}
    />
    <ToggleGroup
      variant="outline"
      size="sm"
      aria-label="Filtrar visitas"
      value={[vista]}
      onValueChange={(value: string[]) => { const next = value[0] as VisitView | undefined; if (next) void navigate({ search: { vista: next, page: 1 } }) }}
    >
      {VIEWS.map((view) => <ToggleGroupItem key={view} value={view}>{VIEW_LABELS[view]}</ToggleGroupItem>)}
    </ToggleGroup>
    {visits.isPending ? <ApplicationLoading /> : visits.isError ? (
      <CitizenVisitError message={citizenVisitError(visits.error)} retry={() => visits.refetch()} />
    ) : visits.data.content.length === 0 ? (
      <Empty className="min-h-64 rounded-xl border">
        <EmptyHeader>
          <EmptyMedia variant="icon"><IconCalendarEvent /></EmptyMedia>
          <EmptyTitle>{page > 1 ? "No hay visitas en esta página" : vista === "proximas" ? "No tenés visitas próximas" : "Todavía no hay visitas ni entrevistas"}</EmptyTitle>
          <EmptyDescription>{page > 1 ? "Volvé a la primera página para ver las actuaciones." : "Cuando se programe una visita o entrevista para alguna de tus solicitudes, aparecerá acá."}</EmptyDescription>
        </EmptyHeader>
        {page > 1 ? <Button variant="outline" onClick={() => void navigate({ search: { vista, page: 1 } })}>Primera página</Button>
          : vista === "proximas" && <Button variant="outline" onClick={() => void navigate({ search: { vista: "todas", page: 1 } })}>Ver todas</Button>}
      </Empty>
    ) : <div className="space-y-4" aria-busy={visits.isFetching}>
      {visits.data.content.map((visit) => <CitizenVisitCard key={visit.id} visit={visit} showApplication onOpen={() => setSelected(visit)} />)}
      <DataPagination className="rounded-xl border" page={visits.isPlaceholderData ? visits.data.page + 1 : page} totalPages={Math.max(1, visits.data.totalPages)} totalItems={visits.data.totalElements} pageSize={PAGE_SIZE} onPageChange={(nextPage) => { if (!visits.isFetching) void navigate({ search: { vista, page: nextPage } }) }} />
    </div>}
    {selected && <CitizenVisitDialog key={selected.id} applicationId={selected.applicationId} visitId={selected.id} onClose={() => setSelected(null)} />}
  </ApplicationPage>
}
