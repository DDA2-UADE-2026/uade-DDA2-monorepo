import { IconAlertCircle, IconCalendarEvent, IconClock, IconMapPin } from "@tabler/icons-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { useState, type ReactNode } from "react"
import { toast } from "sonner"

import { DateTimePicker } from "@/components/DateTimePicker"
import { RescheduleRequestDetail } from "@/components/applications/SocialRescheduleInbox"
import { UserChip } from "@/components/UserChip"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import {
  apiErrorMessage,
  dateTimeLocal,
  formatSocialDate,
  invalidateSocialQueries,
  requestStatusLabel,
  socialManagement,
  socialQueries,
  visitStatusLabel,
  visitTypeLabel,
  type Visit,
  type VisitStatus,
} from "@/lib/social-management"

type Action = "reschedule" | "cancel" | "result"
const RESULT_ITEMS = [{ value: "REALIZED", label: "Realizada" }, { value: "NOT_REALIZED", label: "No realizada" }]

const statusVariant: Record<VisitStatus, "default" | "secondary" | "outline" | "destructive"> = {
  SCHEDULED: "secondary",
  REALIZED: "default",
  NOT_REALIZED: "outline",
  CANCELLED: "destructive",
}

export function VisitDetail({ id, permissions, currentUserId }: { id: string; permissions: string[]; currentUserId?: number }) {
  const queryClient = useQueryClient()
  const detail = useQuery(socialQueries.visit(id))
  const [now] = useState(() => Date.now())
  const [action, setAction] = useState<Action | null>(null)
  const [schedule, setSchedule] = useState({ startsAt: "", durationMinutes: 60, location: "" })
  const [reason, setReason] = useState("")
  const [result, setResult] = useState<"REALIZED" | "NOT_REALIZED">("REALIZED")
  const [observations, setObservations] = useState("")
  const [conclusions, setConclusions] = useState("")
  const mutation = useMutation({
    mutationFn: (visit: Visit) => {
      if (action === "reschedule") return socialManagement.rescheduleVisit(visit.applicationId, visit.id, { startsAt: new Date(schedule.startsAt).toISOString(), durationMinutes: schedule.durationMinutes, location: schedule.location.trim() })
      if (action === "cancel") return socialManagement.cancelVisit(visit.applicationId, visit.id, reason.trim())
      return socialManagement.recordResult(visit.applicationId, visit.id, { status: result, ...(observations.trim() && { observations: observations.trim() }), ...(conclusions.trim() && { conclusions: conclusions.trim() }) })
    },
    onSuccess: async () => {
      toast.success("Actuación actualizada")
      setAction(null)
      await queryClient.invalidateQueries({ queryKey: socialQueries.visit(id).queryKey })
      await queryClient.invalidateQueries(invalidateSocialQueries("social-visits"))
      await queryClient.invalidateQueries(invalidateSocialQueries("social-reschedule-requests"))
    },
    onError: (error) => toast.error(apiErrorMessage(error)),
  })

  if (detail.isPending) {
    return (
      <div role="status" aria-label="Cargando actuación" className="space-y-4">
        <Skeleton className="h-9 w-72" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }
  if (detail.isError) {
    return (
      <Alert variant="destructive">
        <IconAlertCircle />
        <AlertTitle>No se pudo cargar la actuación</AlertTitle>
        <AlertDescription><Button size="sm" variant="outline" className="mt-2" onClick={() => void detail.refetch()}>Reintentar</Button></AlertDescription>
      </Alert>
    )
  }

  const visit = detail.data
  const future = new Date(visit.startsAt).getTime() > now
  const finished = new Date(visit.endsAt).getTime() <= now
  const pendingRequest = visit.latestRescheduleRequest?.status === "PENDING"
  const isResponsible = visit.responsibleId === currentUserId
  const canManage = visit.status === "SCHEDULED" && future && !pendingRequest && isResponsible && permissions.includes("applications:visits:manage")
  const canRecord = visit.status === "SCHEDULED" && visit.applicationStatus === "IN_VISIT" && finished && !pendingRequest && isResponsible && permissions.includes("applications:visits:results:create")

  return (
    <div className="space-y-6">
      <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="font-heading text-2xl font-medium">{visitTypeLabel[visit.type]} · Solicitud N.º {visit.applicationNumber}</h1>
            <Badge variant={statusVariant[visit.status]}>{visitStatusLabel[visit.status]}</Badge>
            {visit.rescheduled && <Badge variant="outline">Reprogramada</Badge>}
          </div>
          <p className="text-sm text-muted-foreground">{visit.programName} · {visit.programEditionName}</p>
          <div className="flex flex-wrap gap-2 pt-1">
            <Button size="sm" variant="outline" render={<Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: visit.applicationId }} />}>Ver solicitud</Button>
            <Button size="sm" variant="outline" render={<Link to="/gestion/casos/$solicitudId/visitas" params={{ solicitudId: visit.applicationId }} />}>Visitas de la solicitud</Button>
            <Button size="sm" variant="outline" render={<Link to="/gestion/casos/$solicitudId/evaluacion" params={{ solicitudId: visit.applicationId }} />}>Evaluación</Button>
          </div>
        </div>
        {(canManage || canRecord) && (
          <div className="flex flex-wrap gap-2">
            {canManage && <>
              <Button variant="outline" onClick={() => { setAction("reschedule"); setSchedule({ startsAt: dateTimeLocal(visit.startsAt), durationMinutes: visit.durationMinutes, location: visit.location }) }}>Reprogramar</Button>
              <Button variant="destructive" onClick={() => setAction("cancel")}>Cancelar</Button>
            </>}
            {canRecord && <Button onClick={() => setAction("result")}>Registrar resultado</Button>}
          </div>
        )}
      </header>

      {pendingRequest && (
        <Alert>
          <IconClock />
          <AlertTitle>Reprogramación pendiente</AlertTitle>
          <AlertDescription>
            El ciudadano pidió reprogramar esta actuación. Resolvé el pedido antes de modificarla.
          </AlertDescription>
        </Alert>
      )}

      {visit.latestRescheduleRequest && permissions.includes("applications:visits:reschedule-requests:view") && (
        <RescheduleRequestDetail
          key={visit.latestRescheduleRequest.id}
          id={visit.latestRescheduleRequest.id}
          canReview={permissions.includes("applications:visits:reschedule-requests:review")}
          currentUserId={currentUserId}
        />
      )}

      {action && (
        <Card>
          <CardHeader>
            <CardTitle>{action === "reschedule" ? "Nuevo horario" : action === "cancel" ? "Cancelar actuación" : "Resultado de la actuación"}</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); mutation.mutate(visit) }}>
              {action === "reschedule" && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5 sm:col-span-2"><Label htmlFor="visit-reschedule-starts-at">Inicio</Label><DateTimePicker id="visit-reschedule-starts-at" required timeLabel="Hora de inicio" min={dateTimeLocal(new Date().toISOString())} value={schedule.startsAt} onChange={(startsAt) => setSchedule({ ...schedule, startsAt })} /></div>
                  <div className="space-y-1.5"><Label htmlFor="visit-reschedule-duration">Duración (minutos)</Label><Input id="visit-reschedule-duration" required type="number" min={1} value={schedule.durationMinutes} onChange={(event) => setSchedule({ ...schedule, durationMinutes: Number(event.target.value) })} /></div>
                  <div className="space-y-1.5"><Label htmlFor="visit-reschedule-location">Lugar</Label><Input id="visit-reschedule-location" required maxLength={300} value={schedule.location} onChange={(event) => setSchedule({ ...schedule, location: event.target.value })} /></div>
                </div>
              )}
              {action === "cancel" && (
                <div className="space-y-1.5"><Label htmlFor="visit-cancel-reason">Motivo</Label><Textarea id="visit-cancel-reason" required maxLength={1000} value={reason} onChange={(event) => setReason(event.target.value)} /></div>
              )}
              {action === "result" && <>
                <div className="space-y-1.5">
                  <Label htmlFor="visit-result">Estado</Label>
                  <Select items={RESULT_ITEMS} value={result} onValueChange={(value) => value && setResult(value as "REALIZED" | "NOT_REALIZED")}>
                    <SelectTrigger id="visit-result" className="w-full"><SelectValue /></SelectTrigger>
                    <SelectContent>{RESULT_ITEMS.map((item) => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
                <div className="space-y-1.5"><Label htmlFor="visit-observations">Observaciones (opcional)</Label><Textarea id="visit-observations" maxLength={2000} value={observations} onChange={(event) => setObservations(event.target.value)} /></div>
                <div className="space-y-1.5"><Label htmlFor="visit-conclusions">Conclusiones (opcional)</Label><Textarea id="visit-conclusions" maxLength={2000} value={conclusions} onChange={(event) => setConclusions(event.target.value)} /></div>
              </>}
              <div className="flex gap-2">
                <Button type="submit" disabled={mutation.isPending || (action === "cancel" && !reason.trim()) || (action === "reschedule" && (!schedule.startsAt || !schedule.location.trim() || schedule.durationMinutes < 1))}>
                  {mutation.isPending ? "Guardando…" : "Confirmar"}
                </Button>
                <Button type="button" variant="ghost" onClick={() => setAction(null)}>Volver</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>Datos de la actuación</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-5 text-sm sm:grid-cols-2">
            <Field label="Titular"><UserChip user={{ id: visit.applicantUserId, name: visit.applicantName, email: visit.applicantEmail }} secondary={visit.applicantEmail} /></Field>
            <Field label="Responsable"><UserChip user={{ id: visit.responsibleId, name: visit.responsibleName, email: visit.responsibleEmail }} secondary={visit.responsibleEmail} /></Field>
            <Field label="Horario" icon={<IconCalendarEvent />}>{formatSocialDate(visit.startsAt)} – {formatSocialDate(visit.endsAt)}</Field>
            <Field label="Duración" icon={<IconClock />}>{visit.durationMinutes} minutos</Field>
            <Field label="Lugar" icon={<IconMapPin />} wide>{visit.location}</Field>
            {visit.cancellationReason && <Field label="Motivo de cancelación" wide><p className="whitespace-pre-wrap">{visit.cancellationReason}</p></Field>}
            {visit.observations && <Field label="Observaciones" wide><p className="whitespace-pre-wrap">{visit.observations}</p></Field>}
            {visit.conclusions && <Field label="Conclusiones" wide><p className="whitespace-pre-wrap">{visit.conclusions}</p></Field>}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Historial</CardTitle>
            <CardDescription>Cambios registrados sobre la actuación.</CardDescription>
          </CardHeader>
          <CardContent>
            <ol className="space-y-4 border-l pl-4 text-sm">
              <HistoryItem title="Programada" date={visit.scheduledAt} />
              {visit.lastRescheduledAt && <HistoryItem title="Reprogramada" date={visit.lastRescheduledAt} />}
              {visit.latestRescheduleRequest && (
                <HistoryItem
                  title={`Pedido de reprogramación: ${requestStatusLabel[visit.latestRescheduleRequest.status].toLowerCase()}`}
                  date={visit.latestRescheduleRequest.reviewedAt ?? visit.latestRescheduleRequest.requestedAt}
                />
              )}
              {visit.cancelledAt && <HistoryItem title="Cancelada" date={visit.cancelledAt} by={visit.cancelledByName} />}
              {visit.resultRecordedAt && <HistoryItem title={`Resultado: ${visitStatusLabel[visit.status].toLowerCase()}`} date={visit.resultRecordedAt} by={visit.resultRecordedByName} />}
            </ol>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function Field({ label, icon, wide, children }: { label: string; icon?: ReactNode; wide?: boolean; children: ReactNode }) {
  return (
    <div className={wide ? "space-y-1 sm:col-span-2" : "space-y-1"}>
      <p className="text-xs text-muted-foreground">{label}</p>
      <div className="flex min-w-0 items-start gap-2 [&>svg]:mt-0.5 [&>svg]:size-4 [&>svg]:shrink-0 [&>svg]:text-muted-foreground">{icon}<div className="min-w-0">{children}</div></div>
    </div>
  )
}

function HistoryItem({ title, date, by }: { title: string; date: string | null | undefined; by?: string | null }) {
  return (
    <li className="relative">
      <span aria-hidden="true" className="absolute top-1.5 -left-[21px] size-2 rounded-full bg-primary" />
      <p className="font-medium">{title}</p>
      <p className="text-xs text-muted-foreground">{formatSocialDate(date)}{by ? ` · ${by}` : ""}</p>
    </li>
  )
}
