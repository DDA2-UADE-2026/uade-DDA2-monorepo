import { IconArrowRight, IconFileText, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { ApplicationError, ApplicationHeading, ApplicationLoading, ApplicationPage, ApplicationStatusBadge } from "@/components/applications/ApplicationUi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { listOptions } from "@/generated/@tanstack/react-query.gen"
import { formatApplicationDate } from "@/lib/application-flow"

const PAGE_SIZE = 10
const searchSchema = z.object({ page: z.coerce.number().int().positive().catch(1).default(1) })

export const Route = createFileRoute("/_app/portal/solicitudes/")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { page } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const applications = useQuery({ ...listOptions({ query: { page: page - 1, size: PAGE_SIZE } }), placeholderData: keepPreviousData })
  return <ApplicationPage breadcrumbs={[{ label: "Mis solicitudes" }]}>
    <ApplicationHeading title="Mis solicitudes" description="Consultá tus presentaciones y completá la documentación de cada una." action={
      <Button variant="outline" size="sm" disabled={applications.isFetching} onClick={() => applications.refetch()}><IconRefresh className={applications.isFetching ? "animate-spin" : undefined} />Actualizar</Button>
    } />
    {applications.isPending ? <ApplicationLoading /> : applications.isError ? <ApplicationError error={applications.error} title="No pudimos cargar tus solicitudes" retry={() => applications.refetch()} /> : !applications.data?.content?.length ? (
      <Empty className="min-h-72 rounded-xl border">
        <EmptyHeader><EmptyMedia variant="icon"><IconFileText /></EmptyMedia><EmptyTitle>{page > 1 ? "No hay solicitudes en esta página" : "Todavía no presentaste solicitudes"}</EmptyTitle><EmptyDescription>{page > 1 ? "Volvé a la primera página para consultar tus presentaciones." : "Consultá los programas disponibles y solicitá el que corresponda desde su página."}</EmptyDescription></EmptyHeader>
        {page > 1 ? <Button variant="outline" onClick={() => navigate({ search: { page: 1 } })}>Primera página</Button> : <Button variant="outline" render={<Link to="/portal/programas" search={{ page: 1 }} />}>Ver programas</Button>}
      </Empty>
    ) : <>
      <div className="space-y-4" aria-busy={applications.isFetching}>
        {applications.data.content.map((application) => <Card key={application.id}>
          <CardHeader>
            <div className="flex flex-wrap items-center justify-between gap-2"><p className="text-xs font-medium text-muted-foreground">Solicitud N.º {application.applicationNumber}</p><ApplicationStatusBadge status={application.status} /></div>
            <CardTitle className="font-heading text-lg">{application.programName ?? "Solicitud de programa"}</CardTitle>
            <CardDescription>{application.programEditionName ? `${application.programEditionName} · ` : ""}Presentada el {formatApplicationDate(application.submittedAt)}</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            {(application.pendingDocuments?.length ?? 0) > 0 ? <Badge variant="outline">{application.pendingDocuments!.length} documento{application.pendingDocuments!.length === 1 ? " pendiente" : "s pendientes"}</Badge> : <p className="text-sm text-muted-foreground">Sin documentos obligatorios faltantes u observados.</p>}
            {application.id && <Button variant="outline" render={<Link to="/portal/solicitudes/$solicitudId" params={{ solicitudId: application.id }} />}>Ver solicitud<IconArrowRight /></Button>}
          </CardContent>
        </Card>)}
      </div>
      <DataPagination className="rounded-xl border" page={applications.isPlaceholderData ? (applications.data.page ?? 0) + 1 : page} totalPages={Math.max(1, applications.data.totalPages ?? 1)} totalItems={applications.data.totalElements ?? 0} pageSize={PAGE_SIZE} onPageChange={(nextPage) => { if (!applications.isFetching) navigate({ search: { page: nextPage } }) }} />
    </>}
  </ApplicationPage>
}
