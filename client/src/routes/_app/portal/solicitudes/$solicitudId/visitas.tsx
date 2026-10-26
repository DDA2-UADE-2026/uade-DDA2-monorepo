import { IconArrowLeft, IconCalendarEvent, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { toast } from "sonner"
import { z } from "zod"

import { DataPagination } from "@/components/DataPagination"
import { ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { get2Options } from "@/generated/@tanstack/react-query.gen"
import { useMe } from "@/hooks/use-auth"
import { formatApplicationDateTime } from "@/lib/application-flow"
import {
  citizenRequestStatusLabels,
  citizenVisitError,
  citizenVisitQueries,
  citizenVisitStatusLabels,
  citizenVisitTypeLabels,
  citizenVisits,
  type CitizenVisit,
} from "@/lib/citizen-visits"

const PAGE_SIZE = 10
const REQUEST_PAGE_SIZE = 5
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
    {!validId ? <VisitError message="El enlace de la solicitud no es válido." /> : application.isPending ? <ApplicationLoading /> : application.isError ? (
      <VisitError message={citizenVisitError(application.error)} retry={() => application.refetch()} />
    ) : visits.isPending ? <ApplicationLoading /> : visits.isError ? (
      <VisitError message={citizenVisitError(visits.error)} retry={() => visits.refetch()} />
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
      {visits.data.content.map((visit) => <VisitCard key={visit.id} visit={visit} onOpen={() => setSelectedVisitId(visit.id)} />)}
      <DataPagination className="rounded-xl border" page={visits.isPlaceholderData ? visits.data.page + 1 : page} totalPages={Math.max(1, visits.data.totalPages)} totalItems={visits.data.totalElements} pageSize={PAGE_SIZE} onPageChange={(nextPage) => { if (!visits.isFetching) void navigate({ search: { page: nextPage } }) }} />
    </div>}
    {selectedVisitId && <VisitDialog key={selectedVisitId} applicationId={solicitudId} visitId={selectedVisitId} onClose={() => setSelectedVisitId(null)} />}
  </ApplicationPage>
}

function VisitCard({ visit, onOpen }: { visit: CitizenVisit; onOpen: () => void }) {
  return <Card>
    <CardHeader>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <CardTitle className="font-heading text-lg">{citizenVisitTypeLabels[visit.type]}</CardTitle>
        <div className="flex flex-wrap gap-2">
          <Badge variant={visit.status === "CANCELLED" ? "destructive" : "secondary"}>{citizenVisitStatusLabels[visit.status]}</Badge>
          {visit.latestRescheduleRequest?.status === "PENDING" && <Badge variant="outline">Reprogramación pendiente</Badge>}
        </div>
      </div>
      <CardDescription>{formatApplicationDateTime(visit.startsAt)} – {formatApplicationDateTime(visit.endsAt)}</CardDescription>
    </CardHeader>
    <CardContent className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <p><span className="text-muted-foreground">Lugar:</span> {visit.location}</p>
      <Button variant="outline" size="sm" onClick={onOpen}>Ver detalle</Button>
    </CardContent>
  </Card>
}

function VisitDialog({ applicationId, visitId, onClose }: { applicationId: string; visitId: string; onClose: () => void }) {
  const queryClient = useQueryClient()
  const { data: session } = useMe()
  const [reason, setReason] = useState("")
  const [requestPage, setRequestPage] = useState(1)
  const [selectedRequestId, setSelectedRequestId] = useState<string | null>(null)
  const [openedAt] = useState(() => Date.now())
  const visit = useQuery(citizenVisitQueries.detail(applicationId, visitId))
  const requests = useQuery({
    ...citizenVisitQueries.requests(applicationId, visitId, requestPage - 1, REQUEST_PAGE_SIZE),
    enabled: visit.isSuccess,
    placeholderData: keepPreviousData,
  })
  const requestDetail = useQuery({
    ...citizenVisitQueries.request(applicationId, visitId, selectedRequestId ?? ""),
    enabled: !!selectedRequestId,
  })
  const create = useMutation({
    mutationFn: () => citizenVisits.createRequest(applicationId, visitId, reason.trim()),
    onSuccess: async () => {
      setReason("")
      setRequestPage(1)
      await Promise.all([
        queryClient.invalidateQueries({ predicate: (query) => query.queryKey[0] === "citizen-visits" && query.queryKey[1] === applicationId }),
        queryClient.invalidateQueries({ queryKey: citizenVisitQueries.detail(applicationId, visitId).queryKey }),
        queryClient.invalidateQueries({ predicate: (query) => query.queryKey[0] === "citizen-visit-requests" && query.queryKey[1] === applicationId && query.queryKey[2] === visitId }),
      ])
      toast.success("Solicitud de reprogramación enviada")
    },
    onError: async () => {
      await queryClient.invalidateQueries({ queryKey: citizenVisitQueries.detail(applicationId, visitId).queryKey })
    },
  })
  const canRequest = !!visit.data && visit.data.status === "SCHEDULED" && new Date(visit.data.startsAt).getTime() > openedAt
    && visit.data.latestRescheduleRequest?.status !== "PENDING"
    && !!session?.user?.permissions?.includes("applications:own:visits:reschedule-requests:create")

  return <Dialog open onOpenChange={(open) => { if (!open && !create.isPending) onClose() }}>
    <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-2xl" showCloseButton={!create.isPending}>
      <DialogHeader>
        <DialogTitle>{visit.data ? citizenVisitTypeLabels[visit.data.type] : "Detalle de visita"}</DialogTitle>
        <DialogDescription>Información y pedidos de reprogramación de esta actuación.</DialogDescription>
      </DialogHeader>
      {visit.isPending ? <Skeleton className="h-36 w-full" /> : visit.isError ? <VisitError message={citizenVisitError(visit.error)} retry={() => visit.refetch()} /> : <>
        <div className="grid gap-3 rounded-xl border p-4 text-sm sm:grid-cols-2">
          <div><p className="text-muted-foreground">Estado</p><Badge className="mt-1" variant={visit.data.status === "CANCELLED" ? "destructive" : "secondary"}>{citizenVisitStatusLabels[visit.data.status]}</Badge></div>
          <div><p className="text-muted-foreground">Fecha y horario</p><p className="mt-1 font-medium">{formatApplicationDateTime(visit.data.startsAt)} – {formatApplicationDateTime(visit.data.endsAt)}</p></div>
          <div><p className="text-muted-foreground">Lugar</p><p className="mt-1 font-medium">{visit.data.location}</p></div>
          <div><p className="text-muted-foreground">Duración</p><p className="mt-1 font-medium">{visit.data.durationMinutes} minutos</p></div>
          {visit.data.rescheduled && <p className="sm:col-span-2 text-muted-foreground">Reprogramada por última vez el {formatApplicationDateTime(visit.data.lastRescheduledAt ?? undefined)}.</p>}
          {visit.data.cancellationReason && <p className="sm:col-span-2"><span className="text-muted-foreground">Motivo de cancelación:</span> {visit.data.cancellationReason}</p>}
        </div>
        {visit.data.latestRescheduleRequest?.status === "PENDING" && <Alert><IconCalendarEvent /><AlertTitle>Reprogramación pendiente</AlertTitle><AlertDescription>La fecha y el lugar actuales siguen vigentes hasta que se revise tu pedido.</AlertDescription></Alert>}
        {canRequest && <form className="space-y-3 rounded-xl border p-4" onSubmit={(event) => { event.preventDefault(); if (reason.trim().length > 0 && reason.trim().length <= 1000 && !create.isPending && new Date(visit.data.startsAt).getTime() > Date.now()) create.mutate() }}>
          <div><h3 className="font-medium">Pedir reprogramación</h3><p className="text-sm text-muted-foreground">Contanos por qué no podés asistir. El horario no cambia hasta que se apruebe el pedido.</p></div>
          <div className="space-y-2"><Label htmlFor="visit-reschedule-reason">Motivo</Label><Textarea id="visit-reschedule-reason" required minLength={1} maxLength={1000} value={reason} onChange={(event) => setReason(event.target.value)} disabled={create.isPending} /></div>
          {create.isError && <VisitError title="No pudimos enviar el pedido" message={citizenVisitError(create.error)} />}
          <Button type="submit" disabled={create.isPending || !reason.trim() || reason.trim().length > 1000}>{create.isPending ? "Enviando…" : "Enviar pedido"}</Button>
        </form>}
        <div className="space-y-3">
          <h3 className="font-medium">Historial de pedidos</h3>
          {requests.isPending ? <Skeleton className="h-20 w-full" /> : requests.isError ? <VisitError title="No pudimos cargar el historial" message={citizenVisitError(requests.error)} retry={() => requests.refetch()} /> : requests.data.content.length === 0 ? (
            <p className="text-sm text-muted-foreground">Todavía no hay pedidos de reprogramación.</p>
          ) : <>
            <div className="space-y-2">
              {requests.data.content.map((request) => <div key={request.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border p-3 text-sm">
                <div><p>Pedido del {formatApplicationDateTime(request.requestedAt)}</p><Badge className="mt-1" variant={request.status === "REJECTED" ? "destructive" : "secondary"}>{citizenRequestStatusLabels[request.status]}</Badge></div>
                <Button size="sm" variant="outline" onClick={() => setSelectedRequestId(request.id)}>Ver detalle</Button>
              </div>)}
            </div>
            <DataPagination className="rounded-xl border" page={requests.isPlaceholderData ? requests.data.page + 1 : requestPage} totalPages={Math.max(1, requests.data.totalPages)} totalItems={requests.data.totalElements} pageSize={REQUEST_PAGE_SIZE} onPageChange={(nextPage) => { if (!requests.isFetching) { setSelectedRequestId(null); setRequestPage(nextPage) } }} />
          </>}
          {selectedRequestId && (requestDetail.isPending ? <Skeleton className="h-28 w-full" /> : requestDetail.isError ? <VisitError title="No pudimos cargar el pedido" message={citizenVisitError(requestDetail.error)} retry={() => requestDetail.refetch()} /> : <div className="space-y-2 rounded-xl border p-4 text-sm">
            <div className="flex justify-between gap-2"><h4 className="font-medium">Detalle del pedido</h4><Button variant="ghost" size="sm" onClick={() => setSelectedRequestId(null)}>Cerrar</Button></div>
            <p><span className="text-muted-foreground">Motivo:</span> {requestDetail.data.reason}</p>
            <p><span className="text-muted-foreground">Horario al solicitar:</span> {formatApplicationDateTime(requestDetail.data.scheduleAtRequest.startsAt)}</p>
            {requestDetail.data.reviewedAt && <p><span className="text-muted-foreground">Revisado:</span> {formatApplicationDateTime(requestDetail.data.reviewedAt)}</p>}
            {requestDetail.data.reviewComment && <p><span className="text-muted-foreground">Respuesta:</span> {requestDetail.data.reviewComment}</p>}
            {requestDetail.data.approvedSchedule && <p><span className="text-muted-foreground">Nuevo horario y lugar:</span> {formatApplicationDateTime(requestDetail.data.approvedSchedule.startsAt)} · {requestDetail.data.approvedSchedule.location}</p>}
          </div>)}
        </div>
      </>}
      <DialogFooter><Button variant="outline" onClick={onClose} disabled={create.isPending}>Cerrar</Button></DialogFooter>
    </DialogContent>
  </Dialog>
}

function VisitError({ message, title = "No pudimos cargar las visitas", retry }: { message: string; title?: string; retry?: () => void }) {
  return <Alert variant="destructive"><AlertTitle>{title}</AlertTitle><AlertDescription className="flex flex-col items-start gap-2"><span>{message}</span>{retry && <Button variant="outline" size="sm" onClick={retry}>Reintentar</Button>}</AlertDescription></Alert>
}
