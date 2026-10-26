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

export function SocialRescheduleInbox({ permissions, currentUserId }: { permissions: string[]; currentUserId?: number }) {
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState<RequestStatus | "">("PENDING")
  const [selected, setSelected] = useState<string | null>(null)
  const canView = permissions.includes("applications:visits:reschedule-requests:view")
  const query = useQuery({ ...socialQueries.requests({ page: page - 1, size: 20, status: status || undefined }), enabled: canView })
  if (!canView) return <p role="alert">Tu rol no tiene permiso para consultar solicitudes de reprogramación.</p>
  return <div className="space-y-4"><div className="flex items-end gap-2"><label className="space-y-1 text-sm">Estado<select className="h-9 rounded-full border border-input bg-input/30 px-3" value={status} onChange={e => { setStatus(e.target.value as RequestStatus | ""); setPage(1) }}><option value="">Todas</option>{(Object.keys(requestStatusLabel) as RequestStatus[]).map(value => <option key={value} value={value}>{requestStatusLabel[value]}</option>)}</select></label><Button variant="ghost" onClick={() => query.refetch()}>Actualizar</Button></div>
    {query.isPending ? <p>Cargando solicitudes…</p> : query.isError ? <div role="alert" className="space-y-2"><p>No se pudieron cargar los pedidos.</p><Button variant="outline" onClick={() => query.refetch()}>Reintentar</Button></div> : !query.data.content.length ? <p className="rounded-xl border p-8 text-center text-muted-foreground">No hay pedidos con este estado.</p> : <Table><TableHeader><TableRow><TableHead>Solicitud</TableHead><TableHead>Titular</TableHead><TableHead>Responsable</TableHead><TableHead>Inicio original</TableHead><TableHead>Pedido</TableHead><TableHead>Estado</TableHead><TableHead /></TableRow></TableHeader><TableBody>{query.data.content.map(item => <TableRow key={item.id}><TableCell><Link to="/gestion/solicitudes/$solicitudId" params={{ solicitudId: item.applicationId }} className="hover:underline">N.º {item.applicationNumber}</Link></TableCell><TableCell>{item.applicantName}</TableCell><TableCell>{item.responsibleName}</TableCell><TableCell>{formatSocialDate(item.originalStartsAt)}</TableCell><TableCell>{formatSocialDate(item.requestedAt)}</TableCell><TableCell><Badge variant="outline">{requestStatusLabel[item.status]}</Badge></TableCell><TableCell><Button size="xs" variant="outline" onClick={() => setSelected(item.id === selected ? null : item.id)}>{item.id === selected ? "Cerrar" : "Revisar"}</Button></TableCell></TableRow>)}</TableBody></Table>}
    {query.data && query.data.totalElements > 0 && <DataPagination page={page} totalPages={Math.max(1, query.data.totalPages)} totalItems={query.data.totalElements} pageSize={20} onPageChange={setPage} />}
    {selected && <RequestDetail key={selected} id={selected} canReview={permissions.includes("applications:visits:reschedule-requests:review")} currentUserId={currentUserId} onClose={() => setSelected(null)} />}
  </div>
}

function RequestDetail({ id, canReview, currentUserId, onClose }: { id: string; canReview: boolean; currentUserId?: number; onClose: () => void }) {
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
  return <Card><CardHeader className="flex-row items-center justify-between"><CardTitle>Pedido de reprogramación</CardTitle><Button variant="ghost" size="sm" onClick={onClose}>Cerrar</Button></CardHeader><CardContent className="space-y-4 text-sm">
    {detail.isPending ? <p>Cargando detalle…</p> : detail.isError ? <div role="alert"><p>No se pudo consultar el pedido.</p><Button variant="outline" onClick={() => detail.refetch()}>Reintentar</Button></div> : item && <>
      <div className="grid gap-2 sm:grid-cols-2"><p><b>Solicitud:</b> N.º {item.applicationNumber}</p><p><b>Estado:</b> {requestStatusLabel[item.status]}</p><p><b>Titular:</b> {item.applicantName}</p><p><b>Responsable:</b> {item.responsibleName}</p><p><b>Solicitado:</b> {formatSocialDate(item.requestedAt)}</p><p><b>Horario original:</b> {formatSocialDate(item.scheduleAtRequest?.startsAt ?? item.originalStartsAt)}</p><p className="sm:col-span-2"><b>Lugar original:</b> {item.scheduleAtRequest?.location ?? "—"}</p><p className="sm:col-span-2 whitespace-pre-wrap"><b>Motivo:</b> {item.reason}</p></div>
      {item.reviewComment && <p className="whitespace-pre-wrap"><b>Comentario de revisión:</b> {item.reviewComment}</p>}
      {item.approvedSchedule && <p><b>Nuevo horario aprobado:</b> {formatSocialDate(item.approvedSchedule.startsAt)} · {item.approvedSchedule.location}</p>}
      {item.status === "PENDING" && canReview && item.responsibleId === currentUserId && <form className="space-y-3 rounded-xl border p-4" onSubmit={e => { e.preventDefault(); mutation.mutate() }}><label className="block">Decisión<select className="h-9 w-full rounded-full border border-input bg-input/30 px-3" value={decision} onChange={e => setDecision(e.target.value as "APPROVED" | "REJECTED")}><option value="APPROVED">Aprobar</option><option value="REJECTED">Rechazar</option></select></label>
        {decision === "APPROVED" && <div className="grid gap-3 sm:grid-cols-2"><label>Nuevo inicio<Input required type="datetime-local" min={dateTimeLocal(new Date().toISOString())} value={startsAt} onChange={e => setStartsAt(e.target.value)} /></label><label>Duración (minutos)<Input required type="number" min={1} value={durationMinutes} onChange={e => setDurationMinutes(Number(e.target.value))} /></label><label className="sm:col-span-2">Nuevo lugar<Input required maxLength={300} value={location} onChange={e => setLocation(e.target.value)} /></label></div>}
        <label className="block">Comentario {decision === "APPROVED" ? "(opcional)" : "(obligatorio)"}<Textarea required={decision === "REJECTED"} maxLength={1000} value={comment} onChange={e => setComment(e.target.value)} /></label>
        <Button type="submit" disabled={mutation.isPending || (decision === "REJECTED" && !comment.trim()) || (decision === "APPROVED" && (!startsAt || !location.trim() || durationMinutes < 1))}>{mutation.isPending ? "Guardando…" : "Confirmar decisión"}</Button>
      </form>}
    </>}
  </CardContent></Card>
}
