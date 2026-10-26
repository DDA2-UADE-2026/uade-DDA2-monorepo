import { IconArrowLeft, IconCalendarEvent, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { CitizenVisitCard, CitizenVisitDialog, CitizenVisitError } from "@/components/applications/CitizenVisitUi"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { get2Options } from "@/generated/@tanstack/react-query.gen"
import { citizenVisitError, citizenVisitQueries } from "@/lib/citizen-visits"

const PAGE_SIZE = 10
const searchSchema = z.object({ page: z.coerce.number().int().positive().catch(1).default(1) })

export const Route = createFileRoute("/_app/portal/solicitudes/$solicitudId/visitas")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { solicitudId } = Route.useParams()
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const [selectedVisitId, setSelectedVisitId] = useState<string | null>(null)
  const validId = z.uuid().safeParse(solicitudId).success
  const application = useQuery({ ...get2Options({ path: { id: solicitudId } }), enabled: validId })
  const visits = useQuery({
    ...citizenVisitQueries.list(solicitudId, page - 1, PAGE_SIZE),
    enabled: validId && application.isSuccess,
    placeholderData: keepPreviousData,
  })

  return <ApplicationPage breadcrumbs={[
    { label: "Mis solicitudes", to: "/portal/solicitudes" },
    { label: application.data?.applicationNumber != null ? `Solicitud N.º ${application.data.applicationNumber}` : "Solicitud", to: `/portal/solicitudes/${solicitudId}` },
    { label: "Visitas y entrevistas" },
  ]}>
    <ApplicationHeading
      title="Visitas y entrevistas"
      description="Consultá las actuaciones de esta solicitud y pedí una reprogramación si no podés asistir."
      action={<Button variant="outline" render={<Link to="/portal/solicitudes/$solicitudId" params={{ solicitudId }} />}><IconArrowLeft />Volver a la solicitud</Button>}
    />
    {!validId ? <CitizenVisitError message="El enlace de la solicitud no es válido." /> : application.isPending ? <ApplicationLoading /> : application.isError ? (
      <CitizenVisitError message={citizenVisitError(application.error)} retry={() => application.refetch()} />
    ) : visits.isPending ? <ApplicationLoading /> : visits.isError ? (
      <CitizenVisitError message={citizenVisitError(visits.error)} retry={() => visits.refetch()} />
    ) : visits.data.content.length === 0 ? (
      <Empty className="min-h-64 rounded-xl border">
        <EmptyHeader>
          <EmptyMedia variant="icon"><IconCalendarEvent /></EmptyMedia>
          <EmptyTitle>{page > 1 ? "No hay visitas en esta página" : "Todavía no hay visitas ni entrevistas"}</EmptyTitle>
          <EmptyDescription>{page > 1 ? "Volvé a la primera página para ver las actuaciones." : "Cuando se programe una actuación para esta solicitud, aparecerá acá."}</EmptyDescription>
        </EmptyHeader>
        {page > 1 && <Button variant="outline" onClick={() => void navigate({ search: { page: 1 } })}>Primera página</Button>}
      </Empty>
    ) : <div className="space-y-4" aria-busy={visits.isFetching}>
      <div className="flex justify-end"><Button variant="outline" size="sm" disabled={visits.isFetching} onClick={() => void visits.refetch()}><IconRefresh className={visits.isFetching ? "animate-spin" : undefined} /><span className="sr-only sm:not-sr-only">Refrescar</span></Button></div>
      {visits.data.content.map((visit) => <CitizenVisitCard key={visit.id} visit={visit} onOpen={() => setSelectedVisitId(visit.id)} />)}
      <DataPagination className="rounded-xl border" page={visits.isPlaceholderData ? visits.data.page + 1 : page} totalPages={Math.max(1, visits.data.totalPages)} totalItems={visits.data.totalElements} pageSize={PAGE_SIZE} onPageChange={(nextPage) => { if (!visits.isFetching) void navigate({ search: { page: nextPage } }) }} />
    </div>}
    {selectedVisitId && <CitizenVisitDialog key={selectedVisitId} applicationId={solicitudId} visitId={selectedVisitId} onClose={() => setSelectedVisitId(null)} />}
  </ApplicationPage>
}
