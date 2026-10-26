import { IconRefresh } from "@tabler/icons-react"
import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { toast } from "sonner"
import { DataPagination } from "@/components/DataPagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { apiErrorMessage, dateTimeLocal, formatSocialDate, invalidateSocialQueries, socialManagement, socialQueries, visitStatusLabel, visitTypeLabel, type Visit, type VisitStatus, type VisitType } from "@/lib/social-management"

type Props = { applicationId?: string; permissions: string[]; currentUserId?: number; canSchedule?: boolean }
const newSchedule = () => ({ startsAt: "", durationMinutes: 60, location: "" })

export function SocialVisitsBoard({ applicationId, permissions, currentUserId, canSchedule = true }: Props) {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState("")
  const [number, setNumber] = useState("")
  const [appliedNumber, setAppliedNumber] = useState("")
  const [selected, setSelected] = useState<string | null>(null)
  const [type, setType] = useState<VisitType>("VISIT")
  const [schedule, setSchedule] = useState(newSchedule)
  const canView = permissions.includes("applications:visits:view")
  const canCreate = permissions.includes("applications:visits:create")
  const list = useQuery({ ...socialQueries.visits({ applicationId, page: page - 1, size: 20, status: status || undefined, applicationNumber: appliedNumber || undefined }), enabled: canView })
  const create = useMutation({
    mutationFn: () => socialManagement.createVisit(applicationId!, { type, startsAt: new Date(schedule.startsAt).toISOString(), durationMinutes: schedule.durationMinutes, location: schedule.location.trim() }),
    onSuccess: async () => { toast.success("Actuación programada"); setSchedule(newSchedule()); await queryClient.invalidateQueries(invalidateSocialQueries("social-visits")); await queryClient.invalidateQueries(invalidateSocialQueries("social-evaluations")) },
    onError: error => toast.error(apiErrorMessage(error)),
  })
  return <div className="space-y-5">
    {applicationId && canCreate && canSchedule && <Card><CardHeader><CardTitle>Programar visita o entrevista</CardTitle></CardHeader><CardContent><form className="grid gap-3 sm:grid-cols-2" onSubmit={e => { e.preventDefault(); create.mutate() }}>
      <label className="space-y-1 text-sm">Tipo<select className="h-9 w-full rounded-full border border-input bg-input/30 px-3" value={type} onChange={e => setType(e.target.value as VisitType)}><option value="VISIT">Visita</option><option value="INTERVIEW">Entrevista</option></select></label>
      <label className="space-y-1 text-sm">Inicio<Input required type="datetime-local" min={dateTimeLocal(new Date().toISOString())} value={schedule.startsAt} onChange={e => setSchedule({ ...schedule, startsAt: e.target.value })} /></label>
      <label className="space-y-1 text-sm">Duración en minutos<Input required type="number" min={1} value={schedule.durationMinutes} onChange={e => setSchedule({ ...schedule, durationMinutes: Number(e.target.value) })} /></label>
      <label className="space-y-1 text-sm">Lugar<Input required minLength={1} maxLength={300} value={schedule.location} onChange={e => setSchedule({ ...schedule, location: e.target.value })} /></label>
      <div className="sm:col-span-2"><Button type="submit" disabled={create.isPending || !schedule.startsAt || !schedule.location.trim() || schedule.durationMinutes < 1}>{create.isPending ? "Programando…" : "Programar actuación"}</Button></div>
    </form></CardContent></Card>}
    {!canView ? <p role="alert">Tu rol no tiene permiso para consultar visitas.</p> : <>
      {!applicationId && <form className="flex flex-wrap items-end gap-2" onSubmit={e => { e.preventDefault(); setPage(1); setAppliedNumber(number.trim()) }}><label className="space-y-1 text-sm">N.º de solicitud<Input type="number" min={1} value={number} onChange={e => setNumber(e.target.value)} /></label><label className="space-y-1 text-sm">Estado<select className="h-9 rounded-full border border-input bg-input/30 px-3" value={status} onChange={e => { setStatus(e.target.value); setPage(1) }}><option value="">Todos</option>{(Object.keys(visitStatusLabel) as VisitStatus[]).map(value => <option key={value} value={value}>{visitStatusLabel[value]}</option>)}</select></label><Button type="submit" variant="outline">Buscar</Button><Button type="button" variant="ghost" onClick={() => list.refetch()}><IconRefresh /><span className="sr-only sm:not-sr-only">Refrescar</span></Button></form>}
      {list.isPending ? <p>Cargando actuaciones…</p> : list.isError ? <div role="alert" className="space-y-2"><p>No se pudieron cargar las actuaciones.</p><Button variant="outline" onClick={() => list.refetch()}>Reintentar</Button></div> : !list.data.content.length ? <p className="rounded-xl border p-8 text-center text-muted-foreground">No hay visitas o entrevistas con estos filtros.</p> : <Table><TableHeader><TableRow>{!applicationId && <TableHead>Solicitud</TableHead>}<TableHead>Tipo</TableHead><TableHead>Inicio</TableHead><TableHead>Titular</TableHead><TableHead>Responsable</TableHead><TableHead>Estado</TableHead><TableHead>Acciones</TableHead></TableRow></TableHeader><TableBody>{list.data.content.map(visit => <TableRow key={visit.id}>{!applicationId && <TableCell><Link className="hover:underline" to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: visit.applicationId }}>N.º {visit.applicationNumber}</Link></TableCell>}<TableCell>{visitTypeLabel[visit.type]}</TableCell><TableCell>{formatSocialDate(visit.startsAt)}</TableCell><TableCell>{visit.applicantName}</TableCell><TableCell>{visit.responsibleName}</TableCell><TableCell><Badge variant="outline">{visitStatusLabel[visit.status]}</Badge>{visit.latestRescheduleRequest?.status === "PENDING" && <Badge className="ml-1" variant="secondary">Pedido pendiente</Badge>}</TableCell><TableCell><Button size="xs" variant="outline" onClick={() => setSelected(visit.id === selected ? null : visit.id)}>{visit.id === selected ? "Cerrar" : "Ver detalle"}</Button></TableCell></TableRow>)}</TableBody></Table>}
      {list.data && list.data.totalElements > 0 && <DataPagination page={page} totalPages={Math.max(1, list.data.totalPages)} totalItems={list.data.totalElements} pageSize={20} onPageChange={setPage} />}
      {selected && <VisitDetail key={selected} id={selected} permissions={permissions} currentUserId={currentUserId} onClose={() => setSelected(null)} />}
    </>}
  </div>
}

function VisitDetail({ id, permissions, currentUserId, onClose }: { id: string; permissions: string[]; currentUserId?: number; onClose: () => void }) {
  const queryClient = useQueryClient()
  const detail = useQuery(socialQueries.visit(id))
  const [now] = useState(() => Date.now())
  const [action, setAction] = useState<"reschedule" | "cancel" | "result" | null>(null)
  const [schedule, setSchedule] = useState(newSchedule)
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
    onSuccess: async () => { toast.success("Actuación actualizada"); setAction(null); await queryClient.invalidateQueries({ queryKey: socialQueries.visit(id).queryKey }); await queryClient.invalidateQueries(invalidateSocialQueries("social-visits")); await queryClient.invalidateQueries(invalidateSocialQueries("social-reschedule-requests")) },
    onError: error => toast.error(apiErrorMessage(error)),
  })
  const visit = detail.data
  const future = !!visit && new Date(visit.startsAt).getTime() > now
  const finished = !!visit && new Date(visit.endsAt).getTime() <= now
  const pendingRequest = visit?.latestRescheduleRequest?.status === "PENDING"
  return <Card><CardHeader className="flex-row items-center justify-between"><CardTitle>Detalle de la actuación</CardTitle><Button variant="ghost" size="sm" onClick={onClose}>Cerrar</Button></CardHeader><CardContent className="space-y-4 text-sm">
    {detail.isPending ? <p>Cargando detalle…</p> : detail.isError ? <div role="alert"><p>No se pudo cargar el detalle.</p><Button variant="outline" onClick={() => detail.refetch()}>Reintentar</Button></div> : visit && <>
      <div className="grid gap-2 sm:grid-cols-2"><p><b>Solicitud:</b> <Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: visit.applicationId }} className="hover:underline">N.º {visit.applicationNumber}</Link></p><p><b>Estado:</b> {visitStatusLabel[visit.status]}</p><p><b>Titular:</b> {visit.applicantName}</p><p><b>Responsable:</b> {visit.responsibleName}</p><p><b>Horario:</b> {formatSocialDate(visit.startsAt)} – {formatSocialDate(visit.endsAt)}</p><p><b>Lugar:</b> {visit.location}</p></div>
      {visit.cancellationReason && <p><b>Motivo de cancelación:</b> {visit.cancellationReason}</p>}
      {visit.observations && <p className="whitespace-pre-wrap"><b>Observaciones:</b> {visit.observations}</p>}
      {visit.conclusions && <p className="whitespace-pre-wrap"><b>Conclusiones:</b> {visit.conclusions}</p>}
      {pendingRequest && <p className="rounded-lg border p-3">Hay una solicitud de reprogramación pendiente. Resolvela en la bandeja antes de modificar esta actuación.</p>}
      <div className="flex flex-wrap gap-2">
        {visit.status === "SCHEDULED" && future && !pendingRequest && visit.responsibleId === currentUserId && permissions.includes("applications:visits:manage") && <><Button variant="outline" size="sm" onClick={() => { setAction("reschedule"); setSchedule({ startsAt: dateTimeLocal(visit.startsAt), durationMinutes: visit.durationMinutes, location: visit.location }) }}>Reprogramar</Button><Button variant="destructive" size="sm" onClick={() => setAction("cancel")}>Cancelar</Button></>}
        {visit.status === "SCHEDULED" && visit.applicationStatus === "IN_VISIT" && finished && !pendingRequest && visit.responsibleId === currentUserId && permissions.includes("applications:visits:results:create") && <Button size="sm" onClick={() => setAction("result")}>Registrar resultado</Button>}
      </div>
      {action && <form className="space-y-3 rounded-xl border p-4" onSubmit={e => { e.preventDefault(); mutation.mutate(visit) }}>
        <h3 className="font-medium">{action === "reschedule" ? "Nuevo horario" : action === "cancel" ? "Cancelar actuación" : "Resultado de la actuación"}</h3>
        {action === "reschedule" && <div className="grid gap-3 sm:grid-cols-2"><label>Inicio<Input required type="datetime-local" min={dateTimeLocal(new Date().toISOString())} value={schedule.startsAt} onChange={e => setSchedule({ ...schedule, startsAt: e.target.value })} /></label><label>Duración (minutos)<Input required type="number" min={1} value={schedule.durationMinutes} onChange={e => setSchedule({ ...schedule, durationMinutes: Number(e.target.value) })} /></label><label className="sm:col-span-2">Lugar<Input required maxLength={300} value={schedule.location} onChange={e => setSchedule({ ...schedule, location: e.target.value })} /></label></div>}
        {action === "cancel" && <label className="block">Motivo<Textarea required maxLength={1000} value={reason} onChange={e => setReason(e.target.value)} /></label>}
        {action === "result" && <><label className="block">Estado<select className="h-9 w-full rounded-full border border-input bg-input/30 px-3" value={result} onChange={e => setResult(e.target.value as "REALIZED" | "NOT_REALIZED")}><option value="REALIZED">Realizada</option><option value="NOT_REALIZED">No realizada</option></select></label><label className="block">Observaciones (opcional)<Textarea maxLength={2000} value={observations} onChange={e => setObservations(e.target.value)} /></label><label className="block">Conclusiones (opcional)<Textarea maxLength={2000} value={conclusions} onChange={e => setConclusions(e.target.value)} /></label></>}
        <div className="flex gap-2"><Button type="submit" disabled={mutation.isPending || (action === "cancel" && !reason.trim()) || (action === "reschedule" && (!schedule.startsAt || !schedule.location.trim() || schedule.durationMinutes < 1))}>{mutation.isPending ? "Guardando…" : "Confirmar"}</Button><Button type="button" variant="ghost" onClick={() => setAction(null)}>Volver</Button></div>
      </form>}
    </>}
  </CardContent></Card>
}
