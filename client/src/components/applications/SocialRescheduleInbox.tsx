import { FilterActions, FilterBar, FilterField } from "@/components/FilterBar"
import { DateTimePicker } from "@/components/DateTimePicker"
import { IconRefresh } from "@tabler/icons-react"
import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { toast } from "sonner"
import { DataPagination } from "@/components/DataPagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { apiErrorMessage, dateTimeLocal, formatSocialDate, invalidateSocialQueries, requestStatusLabel, socialManagement, socialQueries, type RequestStatus } from "@/lib/social-management"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { UserChip, UserInline } from "@/components/UserChip"

export function SocialRescheduleInbox({ permissions, page, status, onPageChange, onStatusChange }: {
  permissions: string[]
  page: number
  status: RequestStatus | ""
  onPageChange: (page: number) => void
  onStatusChange: (status: RequestStatus | "") => void
}) {
  const canView = permissions.includes("applications:visits:reschedule-requests:view")
  const query = useQuery({ ...socialQueries.requests({ page: page - 1, size: 20, status: status || undefined }), enabled: canView })
  if (!canView) return <p role="alert">Tu rol no tiene permiso para consultar solicitudes de reprogramación.</p>
  return <div className="space-y-4"><FilterBar label="Filtrar reprogramaciones"><FilterField label="Estado" htmlFor="reschedule-status"><Select items={[{ value: null, label: "Todas" }, ...(Object.keys(requestStatusLabel) as RequestStatus[]).map(value => ({ value, label: requestStatusLabel[value] }))]} value={status || null} onValueChange={v => onStatusChange((v ?? "") as RequestStatus | "")}><SelectTrigger id="reschedule-status" className="w-full"><SelectValue /></SelectTrigger><SelectContent>{([{ value: null, label: "Todas" }, ...(Object.keys(requestStatusLabel) as RequestStatus[]).map(value => ({ value, label: requestStatusLabel[value] }))]).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.label}</SelectItem>)}</SelectContent></Select></FilterField><FilterActions><Button type="button" variant="ghost" onClick={() => query.refetch()} disabled={query.isFetching}><IconRefresh className={query.isFetching ? "animate-spin" : undefined} /><span className="sr-only sm:not-sr-only">Refrescar</span></Button></FilterActions></FilterBar>
    {query.isPending ? <p>Cargando solicitudes…</p> : query.isError ? <div role="alert" className="space-y-2"><p>No se pudieron cargar los pedidos.</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button></div> : !query.data.content.length ? <p className="rounded-xl border p-8 text-center text-muted-foreground">No hay pedidos con este estado.</p> : <Table><TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Titular</TableHead><TableHead>Responsable</TableHead><TableHead>Inicio original</TableHead><TableHead>Pedido</TableHead><TableHead>Estado</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data.content.map(item => <TableRow key={item.id}><TableCell><Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: item.applicationId }} className="hover:underline">N.º {item.applicationNumber}</Link></TableCell><TableCell className="min-w-44"><UserChip user={{ id: item.applicantUserId, name: item.applicantName, email: item.applicantEmail }} /></TableCell><TableCell className="min-w-44"><UserChip user={{ id: item.responsibleId, name: item.responsibleName, email: item.responsibleEmail }} /></TableCell><TableCell>{formatSocialDate(item.originalStartsAt)}</TableCell><TableCell>{formatSocialDate(item.requestedAt)}</TableCell><TableCell><Badge variant="outline">{requestStatusLabel[item.status]}</Badge></TableCell><TableCell><Button size="xs" variant="outline" render={<Link to="/gestion/visitas/reprogramaciones/$pedidoId" params={{ pedidoId: item.id }} />}>{item.status === "PENDING" ? "Revisar" : "Ver detalle"}</Button></TableCell></TableRow>)}</TableBody></Table>}
    {query.data && query.data.totalElements > 0 && <DataPagination page={page} totalPages={Math.max(1, query.data.totalPages)} totalItems={query.data.totalElements} pageSize={20} onPageChange={onPageChange} />}
  </div>
}

export function RescheduleRequestDetail({ id, canReview, currentUserId, showVisitLink = false }: { id: string; canReview: boolean; currentUserId?: number; showVisitLink?: boolean }) {
  const queryClient = useQueryClient()
  const detail = useQuery(socialQueries.request(id))
  const [decision, setDecision] = useState<"APPROVED" | "REJECTED">("APPROVED")
  const [startsAt, setStartsAt] = useState("")
  const [durationMinutes, setDurationMinutes] = useState(60)
  const [location, setLocation] = useState("")
  const [comment, setComment] = useState("")
  const mutation = useMutation({
    mutationFn: () => {
      const item = detail.data!
      return socialManagement.reviewRequest(item.applicationId, item.visitId, item.id,
        decision === "APPROVED"
          ? { status: decision, startsAt: new Date(startsAt).toISOString(), durationMinutes, location: location.trim(), ...(comment.trim() && { comment: comment.trim() }) }
          : { status: decision, comment: comment.trim() })
    },
    onSuccess: async () => { toast.success("Pedido resuelto"); await queryClient.invalidateQueries(invalidateSocialQueries("social-reschedule-requests")); await queryClient.invalidateQueries({ queryKey: socialQueries.request(id).queryKey }); await queryClient.invalidateQueries(invalidateSocialQueries("social-visits")); await queryClient.invalidateQueries(invalidateSocialQueries("social-visit")) },
    onError: error => toast.error(apiErrorMessage(error)),
  })
  const item = detail.data
  return <Card><CardHeader className="flex-row items-center justify-between"><CardTitle>Pedido de reprogramación</CardTitle>{showVisitLink && detail.data && <Button variant="outline" size="sm" render={<Link to="/gestion/visitas/$visitaId" params={{ visitaId: detail.data.visitId }} />}>Ver actuación</Button>}</CardHeader><CardContent className="space-y-4 text-sm">
    {detail.isPending ? <p>Cargando detalle…</p> : detail.isError ? <div role="alert"><p>No se pudo consultar el pedido.</p><Button variant="outline" onClick={() => detail.refetch()}>Reintentar</Button></div> : item && <>
      <div className="grid gap-2 sm:grid-cols-2"><p><b>Solicitud:</b> N.º {item.applicationNumber}</p><p><b>Estado:</b> {requestStatusLabel[item.status]}</p><p><b>Titular:</b> <UserInline user={{ id: item.applicantUserId, name: item.applicantName, email: item.applicantEmail }} /></p><p><b>Responsable:</b> <UserInline user={{ id: item.responsibleId, name: item.responsibleName, email: item.responsibleEmail }} /></p><p><b>Solicitado:</b> {formatSocialDate(item.requestedAt)}</p><p><b>Horario original:</b> {formatSocialDate(item.scheduleAtRequest?.startsAt ?? item.originalStartsAt)}</p><p className="sm:col-span-2"><b>Lugar original:</b> {item.scheduleAtRequest?.location ?? "—"}</p><p className="sm:col-span-2 whitespace-pre-wrap"><b>Motivo:</b> {item.reason}</p></div>
      {item.reviewComment && <p className="whitespace-pre-wrap"><b>Comentario de revisión:</b> {item.reviewComment}</p>}
      {item.approvedSchedule && <p><b>Nuevo horario aprobado:</b> {formatSocialDate(item.approvedSchedule.startsAt)} · {item.approvedSchedule.location}</p>}
      {item.status === "PENDING" && canReview && item.responsibleId === currentUserId && <form className="space-y-3 rounded-xl border p-4" onSubmit={e => { e.preventDefault(); mutation.mutate() }}><div className="space-y-1"><Label htmlFor="reschedule-decision">Decisión</Label><Select items={[{ value: "APPROVED", label: "Aprobar" }, { value: "REJECTED", label: "Rechazar" }]} value={decision} onValueChange={v => v && setDecision(v as "APPROVED" | "REJECTED")}><SelectTrigger id="reschedule-decision" className="w-full"><SelectValue /></SelectTrigger><SelectContent>{([{ value: "APPROVED", label: "Aprobar" }, { value: "REJECTED", label: "Rechazar" }]).map(item => <SelectItem key={item.value ?? "all"} value={item.value}>{item.label}</SelectItem>)}</SelectContent></Select></div>
        {decision === "APPROVED" && <div className="grid gap-3 sm:grid-cols-2"><div className="space-y-1"><Label htmlFor="reschedule-starts-at">Nuevo inicio</Label><DateTimePicker id="reschedule-starts-at" required timeLabel="Hora del nuevo inicio" min={dateTimeLocal(new Date().toISOString())} value={startsAt} onChange={setStartsAt} /></div><label>Duración (minutos)<Input required type="number" min={1} value={durationMinutes} onChange={e => setDurationMinutes(Number(e.target.value))} /></label><label className="sm:col-span-2">Nuevo lugar<Input required maxLength={300} value={location} onChange={e => setLocation(e.target.value)} /></label></div>}
        <label className="block">Comentario {decision === "APPROVED" ? "(opcional)" : "(obligatorio)"}<Textarea required={decision === "REJECTED"} maxLength={1000} value={comment} onChange={e => setComment(e.target.value)} /></label>
        <Button type="submit" disabled={mutation.isPending || (decision === "REJECTED" && !comment.trim()) || (decision === "APPROVED" && (!startsAt || !location.trim() || durationMinutes < 1))}>{mutation.isPending ? "Guardando…" : "Confirmar decisión"}</Button>
      </form>}
    </>}
  </CardContent></Card>
}
